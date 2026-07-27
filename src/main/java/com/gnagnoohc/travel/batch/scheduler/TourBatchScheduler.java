package com.gnagnoohc.travel.batch.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gnagnoohc.travel.batch.service.TourApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourBatchScheduler {
    private final TourApiService tourApiService;

    //  매일 새벽 3시에 공공데이터 자동 동기화 실행 () cron = "초 분 시 일 월 요일")
    @Scheduled(cron = "0 0 3 * * *")
    public void runAutoSync() {
        log.info("TourBatchScheduler: 공공데이터 배치 수집 시작 (지역 정보 동기화 & 핵심 장소 + 이미지 동기화)");
        try {
            // 법정동 코드 동기화 (REGION)
            tourApiService.syncRegionData();
            // 장소 및 이미지 데이터 동기화 (PLACE, PLACE_IMAGE)
            tourApiService.syncTourData();

            log.info("TourBatchScheduler: 공공데이터 배치 수집 완료");
        } catch (Exception e) {
            log.error("TourBatchScheduler: 공공데이터 배치 수집 중 에러 발생", e);
        }
    }
}