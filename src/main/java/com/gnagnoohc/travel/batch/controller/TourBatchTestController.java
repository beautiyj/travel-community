package com.gnagnoohc.travel.batch.controller;

import com.gnagnoohc.travel.batch.service.TourApiService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test/batch")
@RequiredArgsConstructor
public class TourBatchTestController {

    private final TourApiService tourApiService;

    /**
     * 공공데이터의 전국 시도/시군구 코드를 수집하여 DB REGION 테이블에 적재
     * 포스트맨 요청: POST http://localhost:9999/test/batch/init-regions
     */
    @PostMapping("/init-regions")
    public String initRegionData() {
        // 실제 서비스에 있는 REGION 수집/적재 메서드 호출
        tourApiService.syncRegionData(); 
        return "DB REGION 테이블에 실제 지역 코드 1차 적재 완료!";
    }

    /**
     * [2단계 - 장소 수집 테스트]
     * DB REGION 테이블에 적재된 region_id를 기반으로 특정 지역 장소(PLACE) 샘플 수집 진행
     * 포스트맨 요청: POST http://localhost:9999/test/batch/region-sample?regionIds=12790
     */
    @PostMapping("/region-sample")
    public String testRegionSample(@RequestParam List<Integer> regionIds) {
        // 실제 서비스에 있는 장소 수집/배치 메서드 호출
        tourApiService.syncTourDataForRegions(regionIds); 
        return "[2단계 완료] 지정 지역 장소 데이터 수집 완료 - regionIds: " + regionIds;
    }

    // 보충 배치 수집 실행
    @PostMapping("/sync-supplement")
    public ResponseEntity<String> testSupplementSync() {
        tourApiService.fetchAllTargetSyncList();
        return ResponseEntity.ok("보충 배치 수집 테스트 완료");
    }

    // TODO: 테스트후제거필요 
    // 0805 추가 - 지정 지역만 대상으로 배치스케줄러 로직(fetchAllTargetSyncList와 동일 로직) 동작 검증용 테스트
    // 포스트맨: POST http://localhost:9999/test/batch/sync-supplement-regions?regionIds=11440,12790,26350
    @PostMapping("/sync-supplement-regions")
    public ResponseEntity<String> testSupplementSyncForRegions(@RequestParam List<Integer> regionIds) {
        tourApiService.fetchSupplementForRegions(regionIds);
        return ResponseEntity.ok("지정 지역 보충 배치 테스트 완료 - regionIds: " + regionIds);
    }
}