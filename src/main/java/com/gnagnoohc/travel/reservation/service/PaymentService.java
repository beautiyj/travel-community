package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.PaymentStatus;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import com.gnagnoohc.travel.reservation.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 결제 성공: payment 저장 + 예약 상태 PAID */
    @Transactional
    public Payment saveSuccess(Long reservationId, int amount, String paymentKey,
                               String orderId, int paymentType) {
        // 이중결제 방지: 같은 주문번호가 이미 저장돼 있으면 새로 저장하지 않고 기존 결제 반환 (멱등 처리)
        Payment existing = paymentMapper.findByOrderId(orderId);
        if (existing != null) {
            log.info("[중복 승인 요청 무시] orderId={} 이미 처리됨 (paymentId={})",
                    orderId, existing.getPaymentId());
            return existing;
        }

        Payment p = new Payment();
        p.setReservationId(reservationId);
        p.setAmount(amount);
        p.setPaymentKey(paymentKey);
        p.setPaymentStatus(PaymentStatus.DONE);
        p.setOrderId(orderId);
        p.setPaidAt(LocalDateTime.now());
        p.setPaymentType(paymentType);
        paymentMapper.insert(p);

        reservationService.updateStatus(reservationId, ReservationStatus.PAID);
        log.info("[결제 저장 완료] paymentId={}, reservationId={}, amount={}, orderId={} → 예약 상태 '{}'",
                p.getPaymentId(), reservationId, amount, orderId, ReservationStatus.PAID);
        return p;
    }

    /** 결제 취소(환불): PG사 취소 API 호출 + payment CANCELED + 예약 CANCELED */
    @Transactional
    public void cancel(Long paymentId, String reason) {
        Payment p = getById(paymentId);
        if (p == null) {
            throw new IllegalArgumentException("존재하지 않는 결제입니다. id=" + paymentId);
        }
        if (p.getPaymentStatus() == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }

        // payment_key에 카카오 tid가 저장되어 있음
        kakaoPayService.cancel(p.getPaymentKey(), p.getAmount());

        paymentMapper.updateStatus(paymentId, PaymentStatus.CANCELED);
        reservationService.updateStatus(p.getReservationId(), ReservationStatus.CANCELED);
        log.info("[결제 취소 완료] paymentId={}, reservationId={}, 환불액={}",
                paymentId, p.getReservationId(), p.getAmount());
    }

    @Transactional(readOnly = true)
    public Payment getById(Long paymentId) {
        return paymentMapper.findById(paymentId);
    }

    /**
     * 관리자: 취소 요청 승인 → 이때 실제 환불 실행.
     * CANCEL_REQUESTED 상태의 예약에 대해, 결제완료 건을 찾아 환불(cancel)한다.
     * 환불 API가 실패하면 예외가 전파되어 트랜잭션이 롤백되고 CANCEL_REQUESTED 상태가 유지된다.
     */
    @Transactional
    public void approveCancel(Long reservationId) {
        Reservation r = reservationService.getById(reservationId);
        if (r.getStatus() != ReservationStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("취소 요청 상태인 예약만 승인할 수 있습니다. 현재 상태: " + r.getStatus().getLabel());
        }

        List<Payment> payments = paymentMapper.findByReservationId(reservationId);
        Payment paid = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.DONE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("환불할 결제완료 내역이 없습니다. reservationId=" + reservationId));

        log.info("[취소 승인] reservationId={} → 환불 진행 (paymentId={})", reservationId, paid.getPaymentId());
        cancel(paid.getPaymentId(), "관리자 취소 승인");   // 환불 + 결제취소 + 예약취소
    }
}
