package com.gnagnoohc.travel.batch.controller;

import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.service.TourApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: 테스트 완료 후 반드시 삭제 또는 관리자 인증 처리 필요 (운영 배포 전 제거 대상)
@Slf4j
@RestController
@RequiredArgsConstructor
public class TourBatchTestController {
    private final TourApiService tourApiService;
    private final TourApiClient tourApiClient;

    // 테스트 - 공공데이터 원본(raw) 응답을 가공 없이 그대로 확인
    // TourApiClient를 서비스 거치지 않고 직접 호출 -> contentId, firstimage, firstimage2 등 원본 필드 확인용
    @GetMapping("/test/batch/raw")
    public String testRawResponse(@RequestParam String contentTypeId,
                                  @RequestParam(defaultValue = "1") int pageNo) {
        return tourApiClient.fetchAreaBasedSyncList(pageNo, contentTypeId, null, null);
    }

    // 법정동 코드 원본(raw) 응답 확인
    @GetMapping("/test/batch/raw-region")
    public String testRawRegionResponse() {
        return tourApiClient.fetchLdongCode(null, "Y");
    }

    // 법정동 코드 수집만 단독 테스트
    @PostMapping("/test/batch/region")
    public String testRegionSync() {
        tourApiService.syncRegionData();
        return "REGION 동기화 완료 - 로그 확인";
    }

    // 특정 contentTypeId 소량(limit) 테스트 - 기본값 10건
    // syncTourData -> syncTourDataForTest 호출로 변경, limit 파라미터 추가 (기본 10건)
    // 0729 startPageNo추가
    @PostMapping("/test/batch/place")
    public String testPlaceSync(@RequestParam String contentTypeId,
                                @RequestParam(defaultValue = "1") int startPageNo,
                                @RequestParam(defaultValue = "10") int limit) {
        tourApiService.syncTourDataForTest(contentTypeId, startPageNo, limit);
        return "PLACE 소량 테스트 완료 (contentTypeId=" + contentTypeId + ", limit=" + limit + ") - 로그 확인";
    }

}