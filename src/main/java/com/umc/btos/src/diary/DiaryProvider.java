package com.umc.btos.src.diary;

import com.umc.btos.config.BaseException;
import com.umc.btos.config.secret.Secret;
import com.umc.btos.src.diary.model.*;
import com.umc.btos.utils.AES128;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.umc.btos.config.BaseResponseStatus.*;

@Service
public class DiaryProvider {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DiaryDao diaryDao;

    @Autowired
    public DiaryProvider(DiaryDao diaryDao) {
        this.diaryDao = diaryDao;
    }

    /*
     * 일기 작성 여부 확인
     * [GET] /diaries/:date
     */
    public GetCheckDiaryRes checkDiaryDate(int userIdx, String date) throws BaseException {
        try {
            return new GetCheckDiaryRes(diaryDao.checkDiaryDate(userIdx, date));

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /*
     * Archive 조회 - 캘린더
     * [GET] /diaries/calendar?userIdx=&date=&type
     * date = YYYY-MM
     * type (조회 방식) = 1. doneList : 나뭇잎 색으로 done list 개수 표현 / 2. emotion : 감정 이모티콘
     */
    public List<GetCalendarRes> getCalendar(int userIdx, String date, String type) throws BaseException {
        // TODO : 의미적 validaion - 프리미엄 미가입자는 감정 이모티콘으로 조회 불가
        if (type.compareTo("emotion") == 0 && diaryDao.isPremium(userIdx).compareTo("free") == 0) {
            throw new BaseException(DIARY_NONPREMIUM_USER); // 프리미엄 가입이 필요합니다.
        }

        try {
            // 캘린더 : 한달 단위로 날짜마다 저장된 일기에 대한 정보(done list 개수 또는 감정 이모티콘 식별자)를 저장
            List<GetCalendarRes> calendar = diaryDao.getCalendarList(userIdx, date);

            if (type.compareTo("doneList") == 0) { // done list로 조회 -> 일기 별 doneList 개수 저장 (set doneListNum)
                for (GetCalendarRes dateInfo : calendar) {
                    dateInfo.setDoneListNum(diaryDao.setDoneListNum(userIdx, dateInfo.getDiaryDate()));
                }
            } else { // emotion으로 조회 -> 일기 별 감정 이모티콘 정보 저장 (set emotionIdx)
                for (GetCalendarRes dateInfo : calendar) {
                    dateInfo.setEmotionIdx(diaryDao.setEmotion(userIdx, dateInfo.getDiaryDate()));
                }
            }
            return calendar;

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /*
     * Archive 조회 - 달별 일기 리스트
     * [GET] /diaries/diaryList?userIdx=&date=&search=&startDate=&lastDate=
     * date = YYYY-MM
     * search = 검색할 문장이나 단어 (String)
     * startDate, lastDate = 날짜 기간 설정 (YYYY-MM-DD ~ YYYY-MM-DD)
     * 최신순 정렬 (diaryDate 기준 내림차순 정렬)
     */
    public List<GetDiaryRes> getDiaryList(String[] params) throws BaseException {
        try {
            // String[] params = new String[]{userIdx, date, search, startDate, lastDate};
            int userIdx = Integer.parseInt(params[0]);
            String date = params[1];
            String search = params[2];
            String startDate = params[3];
            String endDate = params[4];

            if (date != null) { // 기간 설정 조회가 아닐 경우 = 한달 단위로 조회 (date)
                startDate = date + "-01";
                endDate = date + "-31";
            }
            // diaryList : 한달 단위 또는 지정된 날짜 범위에서 저장된 일기들에 대한 모든 정보를 저장
            List<GetDiaryRes> diaryList = diaryDao.getDiaryList(userIdx, startDate, endDate);

            // 각 일기에 해당하는 done list 정보 저장
            for (GetDiaryRes diary : diaryList) {
                int diaryIdx = diary.getDiaryIdx();
                diary.setDoneList(diaryDao.getDoneList(diaryIdx));
            }

            // content 복호화
            for (GetDiaryRes diary : diaryList) {
                if (diary.getIsPublic() == 0) { // private일 경우 (isPublic == 0)
                    decryptContents(diary);
                }
            }
            return diaryList;

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // content 복호화
    public void decryptContents(GetDiaryRes diary) throws BaseException {
        try {
            // Diary.content
            String diaryContent = diary.getContent();
            diary.setContent(new AES128(Secret.PASSWORD_KEY).decrypt(diaryContent));

            // Done.content
            List<GetDoneRes> doneList = diary.getDoneList();
            for (int j = 0; j < doneList.size(); j++) {
                String doneContent = diary.getDoneList().get(j).getContent();
                diary.getDoneList().get(j).setContent(new AES128(Secret.PASSWORD_KEY).decrypt(doneContent));
            }

        } catch (Exception ignored) {
            throw new BaseException(DIARY_DECRYPTION_ERROR); // 일기 복호화에 실패하였습니다.
        }
    }

    /*
     * 일기 조회
     * [GET] /diaries?diaryIdx=
     */
    public GetDiaryRes getDiary(int diaryIdx) throws BaseException {
        try {
            GetDiaryRes diary = diaryDao.getDiary(diaryIdx); // 일기의 정보
            diary.setDoneList(diaryDao.getDoneList(diaryIdx)); // done list 정보

            // content 복호화
            if (diary.getIsPublic() == 0) { // private일 경우 (isPublic == 0)
                decryptContents(diary);
            }
            return diary;

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

}
