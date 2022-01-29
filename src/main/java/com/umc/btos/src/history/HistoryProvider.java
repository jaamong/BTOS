package com.umc.btos.src.history;

import com.umc.btos.config.BaseException;
import com.umc.btos.src.history.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static com.umc.btos.config.BaseResponseStatus.*;

@Service
public class HistoryProvider {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HistoryDao historyDao;

    @Autowired
    public HistoryProvider(HistoryDao historyDao) {
        this.historyDao = historyDao;
    }

    /*
     * History 목록 조회
     * [GET] /histories/list/:userIdx/:pageNum?filtering=&search=
     * filtering = 1. sender : 발신인 / 2. diary : 일기만 / 3. letter : 편지만
     * search = 검색할 문자열 ("String")
     * 최신순 정렬 (createdAt 기준 내림차순 정렬)
     * 페이징 처리 (무한 스크롤) - 20개씩 조회
     */
    public GetHistoryListRes getHistoryList(String[] params) throws BaseException, NullPointerException {
        try {
            int userIdx = Integer.parseInt(params[0]);
            String filtering = params[1];
            String search = params[2];

            GetHistoryListRes historyListRes = new GetHistoryListRes(filtering);
            /*
             * filtering = "sender"인 경우 GetHistoryListRes.list = List<HistoryList_Sender>
             * filtering = "diary" 또는 "letter"인 경우 GetHistoryListRes.list = List<History>
             * History 객체 : 수신한 일기 또는 편지에 대한 상세 정보를 저장
             *
             * 1. filtering = sender(발신인)
             *      senderNickNameList에 저장되어 있는 닉네임 순서대로(createdAt 기준 내림차순 정렬) HistoryList_Sender 객체 생성
             *      HistoryList_Sender 필드 : senderNickName(발신자 닉네임), historyListNum(History 개수), List<History> historyList
             *      -> HistoryList_Sender 객체들을 List 형태로 묶어서 GetHistoryListRes.list에 저장
             *
             * 2. filtering = diary(일기만) & letter(편지만)
             *      History 객체들을 List 형태로 묶어서 GetHistoryListRes.list에 저장
             */

            // 발신인
            if (filtering.compareTo("sender") == 0) {
                // userIdx 회원이 받은 일기와 편지의 발신자 닉네임 목록 (createdAt 기준 내림차순 정렬)
                List<String> senderNickNameList = historyDao.getNickNameList_sortedByCreatedAt(userIdx);

                List<HistoryList_Sender> historyListRes_list = new ArrayList<>(); // GetHistoryListRes.list

                for (String senderNickName : senderNickNameList) {
                    List<History> historyList = new ArrayList<>(); // HistoryList_Sender.historyList
                    HistoryList_Sender historyList_sender = new HistoryList_Sender(senderNickName, historyList);

                    if (historyDao.hasHistory_diary(userIdx, senderNickName) != 0) { // null 확인
                        historyList_sender.getHistoryList().addAll(historyDao.getDiaryList(userIdx, senderNickName)); // 일기
                    }
                    if (historyDao.hasHistory_letter(userIdx, senderNickName) != 0) { // null 확인
                        historyList_sender.getHistoryList().addAll(historyDao.getLetterList(userIdx, senderNickName)); // 편지
                    }
                    Collections.sort(historyList_sender.getHistoryList()); // createAt 기준 내림차순 정렬

                    historyList_sender.setHistoryListNum(historyList_sender.getHistoryList().size()); // HistoryList_Sender.historyListNum
                    historyListRes_list.add(historyList_sender);
                }
                historyListRes.setList(historyListRes_list);
            }

            // 일기만
            else if (filtering.compareTo("diary") == 0) {
                List<History> historyList = new ArrayList<>(); // GetHistoryListRes.list

                if (historyDao.hasHistory_diary(userIdx) != 0) { // null 확인
                    historyList.addAll(historyDao.getDiaryList(userIdx)); // 일기 (createAt 기준 내림차순 정렬)
                } else {
                    throw new NullPointerException();
                }
                historyListRes.setList(historyList);
            }

            // 편지만
            else {
                List<History> historyList = new ArrayList<>(); // GetHistoryListRes.list

                if (historyDao.hasHistory_letter(userIdx) != 0) { // null 확인
                    historyList.addAll(historyDao.getLetterList(userIdx)); // 편지 (createAt 기준 내림차순 정렬)
                } else {
                    throw new NullPointerException();
                }
                historyListRes.setList(historyList);
            }

            if (search != null) {

            }

            return historyListRes;

        } catch (NullPointerException nullPointerException) {
            throw new BaseException(EMPTY_RESULT); // 검색 결과 없음
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

}
