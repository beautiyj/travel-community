package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 가상 결제수단(무통장입금·가상카드) — 실제 금융망 연동 없이 규칙 기반 판정만 한다.
 * 판정만 담당하고, 실제 결제 저장·예약 PAID 전환은 기존 {@link PaymentService#saveSuccess}를 재사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualPaymentService {

    /**
     * 성공 처리되는 테스트 카드번호 규칙: "42"로 시작하고 나머지가 전부 "1".
     * (예: 4211111111111111 / 짧게 421 도 성공 — 테스트 편의). 하이픈·공백은 제거 후 판정.
     */
    private static final String SUCCESS_CARD_REGEX = "42[1]+";

    private final ReservationService reservationService;
    private final PaymentService paymentService;

    /**
     * 가상카드 결제: 카드번호가 성공 테스트값이면 즉시 결제완료, 아니면 실패 예외.
     * (동기 처리 — PG·배치 무관)
     */
    public Payment payByVirtualCard(Long reservationId, String cardNumber) {
        Reservation r = reservationService.getById(reservationId);
        requirePending(r);

        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");
        if (!digits.matches(SUCCESS_CARD_REGEX)) {
            throw new IllegalStateException("결제에 실패했습니다. 유효하지 않은 카드번호입니다.");
        }

        int amount = reservationService.calculateAmount(r);
        String orderId = paymentService.generateOrderId(reservationId);
        Payment p = paymentService.saveSuccess(
                reservationId, amount, "VCARD-" + orderId, orderId, Payment.TYPE_VIRTUAL_CARD);
        log.info("[가상카드 결제완료] reservationId={}, orderId={}", reservationId, orderId);
        return p;
    }

    /**
     * 무통장 입금확인(셀프): 입력한 입금자명이 예약자명과 일치하면 결제완료로 처리한다.
     * (실제 은행 입금 대신 규칙 매칭으로 시뮬레이션)
     */
    public Payment confirmBankTransfer(Long reservationId, String depositorName) {
        Reservation r = reservationService.getById(reservationId);
        requirePending(r);

        String expected = normalize(r.getVisitorName());
        String actual = normalize(depositorName);
        if (expected.isEmpty() || !expected.equals(actual)) {
            throw new IllegalStateException("입금자명이 예약자명과 일치하지 않습니다. 확인 후 다시 시도해 주세요.");
        }

        int amount = reservationService.calculateAmount(r);
        // 준비 단계(bankReady)에서 저장해둔 orderId 재사용 (없으면 새로 발급)
        String orderId = r.getOrderId() != null ? r.getOrderId() : paymentService.generateOrderId(reservationId);
        Payment p = paymentService.saveSuccess(
                reservationId, amount, "BANK-" + orderId, orderId, Payment.TYPE_BANK);
        log.info("[무통장 입금확인] reservationId={}, orderId={}, 입금자={}", reservationId, orderId, actual);
        return p;
    }

    private void requirePending(Reservation r) {
        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("결제 대기 중인 예약만 결제할 수 있습니다. 현재 상태: " + r.getStatus().getLabel());
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s", "");
    }
}
