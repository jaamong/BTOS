package com.umc.btos.src.diary;

import com.umc.btos.config.*;
import com.umc.btos.src.diary.model.*;
import com.umc.btos.src.plant.PlantService;
import com.umc.btos.src.plant.model.PatchModifyScoreRes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/diaries")
public class DiaryController {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private final DiaryProvider diaryProvider;
    @Autowired
    private final DiaryService diaryService;
    @Autowired
    private final PlantService plantService;

    public DiaryController(DiaryProvider diaryProvider, DiaryService diaryService, PlantService plantService) {
        this.diaryProvider = diaryProvider;
        this.diaryService = diaryService;
        this.plantService = plantService;
    }

    /*
     * 일기 작성 여부 확인
     * [GET] /diaries/:userIdx/:date
     */
    @ResponseBody
    @GetMapping("/{userIdx}/{date}")
    public BaseResponse<GetCheckDiaryRes> checkDiary(@PathVariable("userIdx") int userIdx, @PathVariable("date") String date) {
        try {
            GetCheckDiaryRes getCheckDiaryRes = diaryProvider.checkDiaryDate(userIdx, date);
            return new BaseResponse<>(getCheckDiaryRes);

        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /*
     * 일기 저장 및 발송, 화분 점수와 레벨 변경
     * [POST] /diaries
     */
//    @ResponseBody
//    @PostMapping("")
//    public BaseResponse<PatchModifyScoreRes> saveDiary(@RequestBody PostDiaryReq postDiaryReq) {
//        try {
//            diaryService.saveDiary(postDiaryReq);
//            PatchModifyScoreRes result = plantService.modifyScore_plus(postDiaryReq.getUserIdx(), Constant.PLANT_LEVELUP_DIARY, "diary");
//            return new BaseResponse<>(result);
//
//        } catch (BaseException exception) {
//            return new BaseResponse<>(exception.getStatus());
//        }
//    }

    /*
     * 일기 수정
     * [PUT] /diaries
     */
//    @ResponseBody
//    @PutMapping("")
//    public BaseResponse<String> modifyDiary(@RequestBody PutDiaryReq putDiaryReq) {
//        try {
//            diaryService.modifyDiary(putDiaryReq);
//
//            String result = "일기(diaryIdx=" + putDiaryReq.getDiaryIdx() + ") 수정 완료";
//            return new BaseResponse<>(result);
//
//        } catch (BaseException exception) {
//            return new BaseResponse<>(exception.getStatus());
//        }
//    }

    /*
     * 일기 삭제
     * [PATCH] /diaries/delete/:diaryIdx
     */
    @ResponseBody
    @PatchMapping("/delete/{diaryIdx}")
    public BaseResponse<String> deleteDiary(@PathVariable("diaryIdx") int diaryIdx) {
        try {
            diaryService.deleteDiary(diaryIdx);

            String result = "일기-diaryIdx=" + diaryIdx + " 삭제 완료";
            return new BaseResponse<>(result);

        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /*
     * Archive 조회 - 달력
     * [GET] /diaries/calendar/:userIdx/:date?type=
     * date = YYYY.MM
     * type (조회 방식) = 1. doneList : 나뭇잎 색으로 done list 개수 표현 / 2. emotion : 감정 이모티콘
     */
    @ResponseBody
    @GetMapping("/calendar/{userIdx}/{date}")
    public BaseResponse<List<GetCalendarRes>> getCalendar(@PathVariable("userIdx") int userIdx, @PathVariable("date") String date, @RequestParam("type") String type) {
        try {
            List<GetCalendarRes> calendar = diaryProvider.getCalendar(userIdx, date, type);
            return new BaseResponse<>(calendar);

        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /*
     * Archive 조회 - 일기 리스트
     * [GET] /diaries/diaryList/:userIdx/:pageNum?search=&startDate=&endDate=
     * search = 검색할 문자열 ("String")
     * startDate, lastDate = 날짜 기간 설정 (YYYY.MM.DD ~ YYYY.MM.DD)
     * 검색 & 기간 설정 조회는 중첩됨
     * 최신순 정렬 (diaryDate 기준 내림차순 정렬)
     * 페이징 처리 (무한 스크롤) - 20개씩 조회
     *
     * 1. 전체 조회 - default
     * 2. 문자열 검색 (search)
     * 3. 기간 설정 조회 (startDate ~ endDate)
     * 4. 문자열 검색 & 기간 설정 조회 (search, startDate ~ endDate)
     */
    @ResponseBody
    @GetMapping("/diaryList/{userIdx}/{pageNum}")
    public BaseResponsePaging<List<GetDiaryRes>> getDiaryList(@PathVariable("userIdx") String userIdx, @PathVariable("pageNum") int pageNum, @RequestParam(required = false) String search, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        try {
            String[] params = new String[]{userIdx, search, startDate, endDate};
            PagingRes pageInfo = new PagingRes(pageNum, Constant.DIARYLIST_DATA_NUM); // 페이징 정보

            List<GetDiaryRes> diaryList = diaryProvider.getDiaryList(params, pageInfo);
            return new BaseResponsePaging<>(diaryList, pageInfo);

        } catch (BaseException exception) {
            return new BaseResponsePaging<>(exception.getStatus());
        }
    }

    /*
     * 일기 조회
     * [GET] /diaries/:diaryIdx
     */
    @ResponseBody
    @GetMapping("/{diaryIdx}")
    public BaseResponse<GetDiaryRes> getDiary(@PathVariable("diaryIdx") int diaryIdx) {
        try {
            GetDiaryRes diary = diaryProvider.getDiary(diaryIdx);
            return new BaseResponse<>(diary);

        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

}
