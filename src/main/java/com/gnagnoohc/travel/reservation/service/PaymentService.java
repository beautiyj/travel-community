package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final ReservationService reservationService;
    private final KakaoPayService kakaoPayService;

    /** 주문번호 생성 규칙: ORDER-{예약ID}-{타임스탬프} */
    public String generateOrderId(Long reservationId) {
        return "ORDER-" + reservationId + "-" + System.currentTimeMillis();
    }

    /** 결제 성공: payment 저장 + 예약 상태 '예약완료' */
    @Transactional
    public Payment saveSuccess(Long reservationId, int amount, String paymentKey,
                               String orderId, int paymentType) {
        Payment p = new Payment();
        p.setReservationId(reservationId);
        p.setAmount(amount);
        p.setPaymentKey(paymentKey);
        p.setPaymentStatus(Payment.STATUS_DONE);
        p.setOrderId(orderId);
        p.setPaidAt(LocalDateTime.now());
        p.setPaymentType(paymentType);
        paymentMapper.insert(p);

        reservationService.updateStatus(reservationId, Reservation.STATUS_PAID);
        log.info("[결제 저장 완료] paymentId={}, reservationId={}, amount={}, orderId={} → 예약 상태 '{}'",
                p.getPaymentId(), reservationId, amount, orderId, Reservation.STATUS_PAID);
        return p;
    }

    /** 결제 취소(환불): PG사 취소 API 호출 + payment '결제취소' + 예약 '예약취소' */
    @Transactional
    public void cancel(Long paymentId, String reason) {
        Payment p = getById(paymentId);
        if (p == null) {
            throw new IllegalArgumentException("존재하지 않는 결제입니다. id=" + paymentId);
        }
        if (Payment.STATUS_CANCELED.equals(p.getPaymentStatus())) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }

        // payment_key에 카카오 tid가 저장되어 있음
        kakaoPayService.cancel(p.getPaymentKey(), p.getAmount());

        paymentMapper.updateStatus(paymentId, Payment.STATUS_CANCELED);
        reservationService.updateStatus(p.getReservationId(), Reservation.STATUS_CANCELED);
        log.info("[결제 취소 완료] paymentId={}, reservationId={}, 환불액={}",
                paymentId, p.getReservationId(), p.getAmount());
    }

    @Transactional(readOnly = true)
    public Payment getById(Long paymentId) {
        return paymentMapper.findById(paymentId);
    }
}
