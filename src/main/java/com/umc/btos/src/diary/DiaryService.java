package com.umc.btos.src.diary;

import com.umc.btos.config.BaseException;
import com.umc.btos.config.secret.Secret;
import com.umc.btos.src.diary.model.*;
import com.umc.btos.utils.AES128;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.umc.btos.config.BaseResponseStatus.*;

@Service
public class DiaryService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DiaryDao diaryDao;

    @Autowired
    public DiaryService(DiaryDao diaryDao) {
        this.diaryDao = diaryDao;
    }

    /*
     * 일기 저장
     * [POST] /diaries
     */
    public void saveDiary(PostDiaryReq postDiaryReq) throws BaseException {
        // TODO : 의미적 validation - 일기는 하루에 하나만 작성 가능, 당일에 작성한 일기가 아니라면 발송 불가
        // 1. 일기는 하루에 하나씩만 작성 가능
        checkDiaryDate(postDiaryReq.getUserIdx(), postDiaryReq.getDiaryDate());
        // 2. 당일에 작성한 일기가 아니라면 발송 불가
        checkPublicDate(postDiaryReq.getDiaryDate(), postDiaryReq.getIsPublic());

        // isPublic == 0(private)인 경우 -> Diary.content & Done.content 부분 암호화하여 저장
        if (postDiaryReq.getIsPublic() == 0) {
            String diaryContent_encrypted = encryptDiaryContent(postDiaryReq.getDiaryContent()); // Diary.content 암호화
            postDiaryReq.setDiaryContent(diaryContent_encrypted);

            List doneList_encrypted = encryptDoneContents(postDiaryReq.getDoneList()); // Done.content 암호화
            postDiaryReq.setDoneList(doneList_encrypted);
        }

        try {
            int diaryIdx = diaryDao.saveDiary(postDiaryReq);
            diaryDao.saveDoneList(diaryIdx, postDiaryReq.getDoneList());

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 일기 작성 또는 수정 시 의미적 validaion - 일기는 하루에 하나씩만 작성 가능
    public void checkDiaryDate(int userIdx, String diaryDate) throws BaseException {
        if (diaryDao.checkDiaryDate(userIdx, diaryDate) == 1) {
            throw new BaseException(DIARY_EXISTS); // 일기는 하루에 하나만 작성 가능합니다.
        }
    }

    // 일기 작성 또는 수정 시 의미적 validaion - 당일에 작성한 일기가 아니라면 발송 불가
    public void checkPublicDate(String diaryDate, int isPublic) throws BaseException {
        LocalDate now = LocalDate.now(); // 오늘 날짜 (YYYY-MM-DD) -> .으로 바꿔야함
//        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd"); // ex. 2022-01-20 14:03:23
//        Date now_formatted = format.parse(now.toString());

        // 작성일과 일기의 해당 날짜가 다를 경우 발송(isPublic == 1) 불가
        if (diaryDate.compareTo(now.toString()) != 0 && isPublic == 1) {
            throw new BaseException(UNPRIVATE_DATE); // 당일에 작성한 일기만 발송 가능합니다!
        }
    }

    // private 일기 암호화 - Diary.content
    public String encryptDiaryContent(String diaryContent) throws BaseException {
        try {
            return new AES128(Secret.PRIVATE_DIARY_KEY).encrypt(diaryContent);

        } catch (Exception ignored) {
            throw new BaseException(DIARY_ENCRYPTION_ERROR); // 일기 또는 done list 내용 암호화에 실패하였습니다.
        }
    }

    // private 일기 암호화 - Done.content
    public List encryptDoneContents(List doneList) throws BaseException {
        try {
            List doneList_encrypted = new ArrayList(); // 암호화된 done list 내용들을 저장하는 리스트
            for (int i = 0; i < doneList.size(); i++) {
                doneList_encrypted.add(new AES128(Secret.PRIVATE_DIARY_KEY).encrypt(doneList.get(i).toString()));
            }
            return doneList_encrypted;

        } catch (Exception ignored) {
            throw new BaseException(DIARY_ENCRYPTION_ERROR); // 일기 또는 done list 내용 암호화에 실패하였습니다.
        }
    }

    /*
     * 일기 수정
     * [PUT] /diaries
     */
    public void modifyDiary(PutDiaryReq putDiaryReq) throws BaseException {
        // TODO : 의미적 validation - 일기는 하루에 하나만 작성 가능, 당일에 작성한 일기가 아니라면 발송 불가
        // 1. 일기는 하루에 하나씩만 작성 가능
        checkDiaryDate(putDiaryReq.getUserIdx(), putDiaryReq.getDiaryDate());
        // 2. 당일에 작성한 일기가 아니라면 발송 불가
        checkPublicDate(putDiaryReq.getDiaryDate(), putDiaryReq.getIsPublic());

        // isPublic == 0(private)인 경우 -> Diary.content & Done.content 부분 암호화하여 저장
        if (putDiaryReq.getIsPublic() == 0) {
            String diaryContent_encrypted = encryptDiaryContent(putDiaryReq.getDiaryContent()); // Diary.content 암호화
            putDiaryReq.setDiaryContent(diaryContent_encrypted);

            List doneList_encrypted = encryptDoneContents(putDiaryReq.getDoneList()); // Done.content 암호화
            putDiaryReq.setDoneList(doneList_encrypted);
        }

        try {
            // Diary Table 수정
            if (diaryDao.modifyDiary(putDiaryReq) == 0) {
                throw new BaseException(MODIFY_FAIL_DIARY); // 일기 수정 실패 - 일기 내용
            }

            // Done Table 수정
            List doneIdxList = diaryDao.getDoneIdxList(putDiaryReq); // 해당 일기의 모든 doneIdx (List 형태로 저장)
            if (diaryDao.modifyDoneList(putDiaryReq, doneIdxList) == 0) {
                throw new BaseException(MODIFY_FAIL_DONELIST); // 일기 수정 실패 - done list
            }

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /*
     * 일기 삭제
     * [PATCH] /diaries/delete/:diaryIdx
     */
    public void deleteDiary(int diaryIdx) throws BaseException {
        try {
            // Diary.status 수정
            if (diaryDao.deleteDiary(diaryIdx) == 0) {
                throw new BaseException(DELETE_FAIL_DIARY); // 일기 삭제 실패
            }

            // Done.status 수정
            if (diaryDao.deleteDone(diaryIdx) == 0) {
                throw new BaseException(DELETE_FAIL_DONELIST); // done list 삭제 실패
            }

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

}
