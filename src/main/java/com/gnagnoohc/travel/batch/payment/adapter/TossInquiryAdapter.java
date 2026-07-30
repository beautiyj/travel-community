package com.gnagnoohc.travel.batch.payment.adapter;

import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import org.springframework.stereotype.Component;

/**
 * 토스 결제 상태 조회 어댑터 — 현재는 스텁(항상 NOT_PAID, 아무 동작 안 함).
 *
 * ⚠️ 실구현 = lee 담당 (토스 API 연동부). 채워야 할 것:
 *   1) PaymentController.tossReady 에 markPaymentReady(reservationId, orderId, TYPE_TOSS, null) 한 줄 (order_id 저장)
 *   2) TossPayService.getOrder(orderId) 추가: GET /v1/payments/orders/{orderId}
 *   3) 아래 inquire()에서 status DONE→InquiryResult.paid(paymentKey),
 *      ABORTED/EXPIRED/CANCELED→InquiryResult.FAILED, 그 외→NOT_PAID
 * 그 전까지 토스 PENDING 건은 보정되지 않는다(안전한 무동작).
 */
@Component
public class TossInquiryAdapter implements PaymentInquiryAdapter {

    @Override
    public boolean supports(int paymentType) {
        return paymentType == Payment.TYPE_TOSS;
    }

    @Override
    public InquiryResult inquire(Reservation r) {
        // TODO(lee): 토스 주문조회 연동 (위 주석 참고)
        return InquiryResult.NOT_PAID;
    }
}
