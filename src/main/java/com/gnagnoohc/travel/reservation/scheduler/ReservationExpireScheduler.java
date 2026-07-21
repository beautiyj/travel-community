package com.gnagnoohc.travel.reservation.scheduler;

import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 결제 대기(PENDING) 상태로 30분이 지난 예약을 자동으로 EXPIRED 처리.
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
}
