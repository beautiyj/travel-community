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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final ReservationService reservationService;
    private final KakaoPayService kakaoPayService;
    private final TossPayService tossPayService;

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

        // 결제 대기 중 마감됐을 가능성에 대한 마지막 방어. 카카오/토스는 이미 승인(이체)이 끝난 뒤라 실패하면 바로 환불한다
        try {
            reservationService.assertStillAvailable(reservationId);
        } catch (IllegalStateException e) {
            cancelAtPg(paymentType, paymentKey, amount, "마감으로 자동 환불");
            throw e;
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

    /** 결제 취소(환불) — 전액 환불. 사업자/시스템 사유의 취소(거절, 자동취소 등)는 고객 잘못이 아니므로 정책 감액 없이 전액 */
    @Transactional
    public void cancel(Long paymentId, String reason) {
        cancel(paymentId, reason, null);
    }

    /**
     * 결제 취소(환불) 내부 구현. overrideAmount가 있으면 그 금액만, 없으면(null) 전액 환불.
     * 방문일 기준 환불 정책(D-3 이전 100%/D-1~2 50%/당일 0%)은 고객이 스스로 취소를 요청한
     * approveCancel()에서만 계산해서 넘겨준다 — 사업자 거절/자동취소(rejectPaid)는 항상 전액.
     */
    @Transactional
    public void cancel(Long paymentId, String reason, Integer overrideAmount) {
        Payment p = getById(paymentId);
        if (p == null) {
            throw new IllegalArgumentException("존재하지 않는 결제입니다. id=" + paymentId);
        }
        if (p.getPaymentStatus() == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }

        int refundAmount = overrideAmount != null ? overrideAmount : p.getAmount();

        cancelAtPg(p.getPaymentType(), p.getPaymentKey(), refundAmount, reason);   // 결제수단별 PG 취소

        paymentMapper.updateStatus(paymentId, PaymentStatus.CANCELED);
        reservationService.updateStatus(p.getReservationId(), ReservationStatus.CANCELED);
        log.info("[결제 취소 완료] paymentId={}, reservationId={}, 원결제액={}, 환불액={}",
                paymentId, p.getReservationId(), p.getAmount(), refundAmount);
    }

    /** 취소 요청 전 환불 예상액 미리보기. 실제 취소는 실행하지 않는 조회 전용 */
    @Transactional(readOnly = true)
    public int previewRefundAmount(Long reservationId) {
        Reservation r = reservationService.getById(reservationId);
        Payment paid = paymentMapper.findByReservationId(reservationId).stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.DONE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("결제완료 내역이 없습니다. reservationId=" + reservationId));
        return ReservationService.applyRefundPolicy(paid.getAmount(), r.getVisitDate(), LocalDate.now());
    }

    /** 결제수단에 맞는 PG 취소 API를 호출한다. cancel()의 정상 환불과 saveSuccess()의 실패 환불이 이 로직을 공통으로 사용한다 */
    private void cancelAtPg(int paymentType, String paymentKey, int amount, String reason) {
        switch (paymentType) {
            case Payment.TYPE_FREE, Payment.TYPE_BANK, Payment.TYPE_VIRTUAL_CARD -> { /* 실제 PG 거래 없음 */ }
            case Payment.TYPE_TOSS  -> tossPayService.cancel(paymentKey, reason != null ? reason : "고객 요청", amount);
            case Payment.TYPE_KAKAO -> kakaoPayService.cancel(paymentKey, amount);
            default -> throw new IllegalStateException("알 수 없는 결제수단입니다. paymentType=" + paymentType);
        }
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

        // 고객이 스스로 취소를 요청한 경우에만 방문일 기준 환불 정책을 적용한다.
        // 기준일은 승인일이 아니라 "취소 신청일"(cancelRequestedAt) — 사업자가 승인을 미룬다고 환불이 깎이면 안 됨
        int refundAmount = ReservationService.applyRefundPolicy(
                paid.getAmount(), r.getVisitDate(), r.getCancelRequestedAt().toLocalDate());
        log.info("[취소 승인] reservationId={} → 환불 진행 (paymentId={}, 환불액={})",
                reservationId, paid.getPaymentId(), refundAmount);
        cancel(paid.getPaymentId(), "관리자 취소 승인", refundAmount);   // 환불 + 결제취소 + 예약취소
    }

    /**
     * 사업자: 결제완료(PAID) 예약 거절 → 환불 실행.
     * PAID 검증·사유 저장은 reservationService.reject()가 먼저 처리한 뒤, 여기서 실제 환불(cancel)을 진행한다.
     */
    @Transactional
    public void rejectPaid(Long reservationId, String reason) {
        String finalReason = reservationService.reject(reservationId, reason);

        List<Payment> payments = paymentMapper.findByReservationId(reservationId);
        Payment paid = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.DONE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("환불할 결제완료 내역이 없습니다. reservationId=" + reservationId));

        log.info("[예약 거절] reservationId={} → 환불 진행 (paymentId={})", reservationId, paid.getPaymentId());
        cancel(paid.getPaymentId(), finalReason);
    }
}
