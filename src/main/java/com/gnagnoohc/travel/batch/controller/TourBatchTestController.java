package com.gnagnoohc.travel.batch.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class TourBatchTestController {

    // 지정한 지역(region_id)들만 대상으로 5개 타입 조회 후 샘플링 테스트 - 소규모 검증용
    // 예: POST /test/batch/region-sample?regionIds=11440,12790,26350
    @PostMapping("/test/batch/region-sample")
    public String testRegionSample(@RequestParam List<Integer> regionIds) {
        tourApiService.syncTourDataForRegions(regionIds);
        return "지정 지역 샘플링 테스트 완료 - regionIds: " + regionIds + " - 로그 및 DB 확인";
    }


}

