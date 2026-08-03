package com.gnagnoohc.travel.reservation.scheduler;

import com.gnagnoohc.travel.batch.payment.PaymentReconcileService;
import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 상태를 시간에 따라 자동 전환하는 스케줄러.
 * 1) 결제 대기(PENDING)로 30분이 지난 예약   -> EXPIRED
 * 2) 방문일이 지난 예약확정(CONFIRMED) 건     -> COMPLETED(이용완료)
 * 3) 방문일이 지났는데 사업자가 승인/거절 안 한(PAID) 건 -> 자동 취소+환불
 * 주의: TravelApplication(또는 config 클래스)에 @EnableScheduling 필요!
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private static final int EXPIRE_MINUTES = 30;

    private final ReservationMapper reservationMapper;
    private final PaymentReconcileService paymentReconcileService;
    private final PaymentService paymentService;

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
     * 2분마다 실행. 결제 준비까지 갔으나 콜백 유실로 PENDING에 남은 예약을 PG에 대조해 보정한다.
     * 만료(30분)보다 촘촘히 돌려, 유령 결제가 만료로 잘못 정리되기 전에 PAID로 복구한다.
     * (트랜잭션은 보정 서비스가 건별로 관리하므로 여기선 걸지 않는다)
     */
    @Scheduled(fixedDelay = 120_000)
    public void reconcilePayments() {
        paymentReconcileService.reconcile();
    }

    /**
     * 매시 5분에 실행. 방문일이 지난 예약확정(CONFIRMED) 건을 이용완료로 전환한다.
     * 하루 1회가 아니라 매시로 돌리는 이유: 서버가 꺼져 있어 특정 시각을 놓쳐도
     * 다음 실행 때 자동으로 따라잡히기 때문. 조건에 맞는 건이 없으면 0건 처리라 부담이 없다.
     */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void completeVisitedReservations() {
        int completed = reservationMapper.completeVisited();
        if (completed > 0) {
            log.info("방문일이 지난 예약확정 {}건을 이용완료 처리했습니다.", completed);
        }
    }

    /**
     * 매시 5분에 실행. 방문일이 지났는데 사업자가 승인도 거절도 안 한(PAID) 예약을 자동 취소+환불한다.
     * 건별로 환불(PG 호출)이 필요해 하나씩 처리하고, 개별 실패가 나머지를 막지 않도록 catch한다.
     */
    @Scheduled(cron = "0 5 * * * *")
    public void cancelUnapprovedExpiredReservations() {
        List<Long> targets = reservationMapper.findExpiredUnapprovedPaidIds();
        for (Long reservationId : targets) {
            try {
                paymentService.rejectPaid(reservationId, "방문일 경과로 자동 취소되었습니다.");
            } catch (Exception e) {
                log.error("방문일 경과 자동취소 실패. reservationId={}", reservationId, e);
            }
        }
        if (!targets.isEmpty()) {
            log.info("방문일 경과 미승인 예약 {}건 자동취소 처리 시도 완료.", targets.size());
        }
    }
}
