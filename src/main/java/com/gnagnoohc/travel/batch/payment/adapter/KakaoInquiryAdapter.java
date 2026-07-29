package com.gnagnoohc.travel.batch.payment.adapter;

import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.service.KakaoPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 카카오 결제 상태 조회 어댑터.
 * 카카오 주문조회(/online/v1/payment/order)는 tid가 키라서, 예약에 저장해둔 pg_tid로 조회한다.
 * 결제완료(SUCCESS_PAYMENT)면 paymentKey 자리에 tid를 실어 반환한다 (PAYMENT.payment_key = 카카오 tid).
 */
@Component
@RequiredArgsConstructor
public class KakaoInquiryAdapter implements PaymentInquiryAdapter {

    private final KakaoPayService kakaoPayService;

    @Override
    public boolean supports(int paymentType) {
        return paymentType == Payment.TYPE_KAKAO;
    }

    @Override
    public InquiryResult inquire(Reservation r) {
        String tid = r.getPgTid();
        if (tid == null) return InquiryResult.NOT_PAID;   // tid 없으면 조회 불가

        String status = kakaoPayService.getOrderStatus(tid);
        if ("SUCCESS_PAYMENT".equals(status)) {
            return InquiryResult.paid(tid);
        }
        if ("FAIL_PAYMENT".equals(status) || "QUIT_PAYMENT".equals(status)
                || "CANCEL_PAYMENT".equals(status) || "FAIL_AUTH_PING".equals(status)) {
            return InquiryResult.FAILED;
        }
        return InquiryResult.NOT_PAID;   // READY / SELECT_METHOD 등 아직 진행 중
    }
}
