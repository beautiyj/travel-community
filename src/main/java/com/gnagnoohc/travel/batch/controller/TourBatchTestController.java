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

    // 특정 지역만 핀포인트 테스트하고 싶을 때 사용
    // POST http://localhost:9999/test/batch/sync-regions-test?regionIds=11110
    @PostMapping("/sync-regions-test")
    public ResponseEntity<String> testRegionsSync(@RequestParam List<Integer> regionIds) {
        tourApiService.syncTargetListForRegions(regionIds);
        return ResponseEntity.ok("지정 지역 1차 적재 테스트 완료 - regionIds: " + regionIds);
    }

    // 테스트 - 부족한 T.O만 핀포인트로 채우는 보충 로직 검증
    // POST http://localhost:9999/test/batch/sync-supplement-regions?regionIds=11110
    @PostMapping("/sync-supplement-regions")
    public ResponseEntity<String> testSupplementSyncForRegions(@RequestParam List<Integer> regionIds) {
        tourApiService.fetchSupplementForRegions(regionIds);
        return ResponseEntity.ok("지정 지역 보충 배치 테스트 완료 - regionIds: " + regionIds);
    }

    // 수동으로 전국 전체 적재를 돌리고 싶을 때 사용
    // POST http://localhost:9999/test/batch/sync-all-real
    @PostMapping("/sync-all-real")
    public ResponseEntity<String> syncAllReal() {
        tourApiService.syncAllTargetList();
        return ResponseEntity.ok("전국 296개 지역군 1차 전체 적재 완료!");
    }

}