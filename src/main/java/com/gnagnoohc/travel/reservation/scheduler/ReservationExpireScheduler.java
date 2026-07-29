package com.gnagnoohc.travel.reservation.scheduler;

import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 예약 상태를 시간에 따라 자동 전환하는 스케줄러.
 * 1) 결제 대기(PENDING)로 30분이 지난 예약 -> EXPIRED
 * 2) 방문일이 지난 예약(PAID)             -> COMPLETED
 * 주의: TravelApplication(또는 config 클래스)에 @EnableScheduling 필요!
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private static final int EXPIRE_MINUTES = 30;

    private final ReservationMapper reservationMapper;

    /** 1분마다 실행 */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRE_MINUTES);
        int expired = reservationMapper.expirePending(cutoff);
        if (expired > 0) {
            log.info("미결제 예약 {}건을 만료 처리했습니다.", expired);
        }
    }

    /**
     * 매시 5분에 실행. 방문일이 지난 예약을 방문완료로 전환한다.
     * 하루 1회가 아니라 매시로 돌리는 이유: 서버가 꺼져 있어 특정 시각을 놓쳐도
     * 다음 실행 때 자동으로 따라잡히기 때문. 조건에 맞는 건이 없으면 0건 처리라 부담이 없다.
     */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void completeVisitedReservations() {
        int completed = reservationMapper.completeVisited();
        if (completed > 0) {
            log.info("방문일이 지난 예약 {}건을 방문완료 처리했습니다.", completed);
        }
    }
}
