package com.gnagnoohc.travel.batch.payment;

import com.gnagnoohc.travel.batch.payment.adapter.InquiryResult;
import com.gnagnoohc.travel.batch.payment.adapter.PaymentInquiryAdapter;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유령 결제 정합성 보정.
 * PG 콜백을 놓쳐 PENDING으로 남은 예약을 PG에 직접 조회해, 실제 결제됐으면 PAID로 복구한다.
 * (PG 웹훅 대신 서버가 주기적으로 물어보는 polling 방식)
 *
 * PG별 조회 방법은 PaymentInquiryAdapter 구현체가 담당하고, 이 서비스는 PG를 구분하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconcileService {

    /** 결제 준비 직후(진행 중일 수 있는) 건은 건드리지 않도록 두는 유예 시간(분) */
    private static final int READY_GRACE_MINUTES = 2;

    private final List<PaymentInquiryAdapter> adapters;   // 등록된 PG 어댑터 전부 자동 주입
    private final ReservationMapper reservationMapper;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    public void reconcile() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(READY_GRACE_MINUTES);
        List<Reservation> targets = reservationMapper.findReadyForInquiry(cutoff);
        for (Reservation r : targets) {
            try {
                reconcileOne(r);
            } catch (Exception e) {
                // 한 건 실패(PG 오류 등)가 나머지 보정을 막지 않도록 개별 격리
                log.warn("[정합성 보정 실패] reservationId={}, orderId={} : {}",
                        r.getReservationId(), r.getOrderId(), e.getMessage());
            }
        }
    }

    private void reconcileOne(Reservation r) {
        if (r.getPaymentType() == null) return;   // 시도 PG 미상 — 대상 아님

        PaymentInquiryAdapter adapter = adapters.stream()
                .filter(a -> a.supports(r.getPaymentType()))
                .findFirst()
                .orElse(null);
        if (adapter == null) return;   // 담당 어댑터 없음

        InquiryResult result = adapter.inquire(r);
        switch (result.status()) {
            case PAID -> {
                // 기존 결제 성공 처리 재사용(orderId 중복 dedup + 예약 PAID 전환 포함)
                int amount = reservationService.calculateAmount(r);
                paymentService.saveSuccess(r.getReservationId(), amount,
                        result.paymentKey(), r.getOrderId(), r.getPaymentType());
                log.info("[유령 결제 보정] reservationId={} → PAID 복구 (orderId={})",
                        r.getReservationId(), r.getOrderId());
            }
            case FAILED -> log.info("[정합성] reservationId={} PG 결제실패 확인 (orderId={}) — 만료 스케줄러가 정리 예정",
                    r.getReservationId(), r.getOrderId());
            case NOT_PAID -> { /* 진행 중/미결제 — 다음 회차에 재확인 */ }
        }
    }
}
