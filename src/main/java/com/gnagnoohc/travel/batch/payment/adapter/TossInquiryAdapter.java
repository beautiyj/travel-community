package com.gnagnoohc.travel.batch.payment.adapter;

import com.gnagnoohc.travel.reservation.dto.TossConfirmResponse;
import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.service.TossPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 토스 결제 상태 조회 어댑터.
 * 토스 주문조회(GET /v1/payments/orders/{orderId})는 orderId가 키라서, 예약에 저장해둔 order_id로 조회한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossInquiryAdapter implements PaymentInquiryAdapter {

    private final TossPayService tossPayService;

    @Override
    public boolean supports(int paymentType) {
        return paymentType == Payment.TYPE_TOSS;
    }

    @Override
    public InquiryResult inquire(Reservation r) {
        String orderId = r.getOrderId();
        if (orderId == null) return InquiryResult.NOT_PAID;   // orderId 없으면 조회 불가

        TossConfirmResponse res;
        try {
            res = tossPayService.getOrder(orderId);
        } catch (Exception e) {
            // 조회 자체가 실패(PG 오류/일시 장애 등)해도 다음 회차에 재확인하면 되므로 안전하게 넘어간다
            log.warn("[토스 주문조회 실패] orderId={} : {}", orderId, e.getMessage());
            return InquiryResult.NOT_PAID;
        }

        String status = res != null ? res.getStatus() : null;
        if ("DONE".equals(status)) {
            return InquiryResult.paid(res.getPaymentKey());
        }
        if ("CANCELED".equals(status) || "PARTIAL_CANCELED".equals(status)
                || "ABORTED".equals(status) || "EXPIRED".equals(status)) {
            return InquiryResult.FAILED;
        }
        return InquiryResult.NOT_PAID;   // READY / IN_PROGRESS / WAITING_FOR_DEPOSIT 등 아직 진행 중
    }
}
