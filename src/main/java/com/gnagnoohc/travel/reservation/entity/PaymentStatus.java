package com.gnagnoohc.travel.reservation.entity;

/**
 * 결제 상태. DB에는 enum 이름(영어)이 저장되고, 화면 표시는 label(한글)을 쓴다.
 * 팀 DDL 컨벤션: PAYMENT.payment_status
 */
public enum PaymentStatus {

    DONE("결제완료"),        // PG 승인 완료
    CANCELED("결제취소"),    // 환불 완료
    FAILED("결제실패");      // 승인 실패

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    /** 화면 표시용 한글 문구 (JSP: ${payment.paymentStatus.label}) */
    public String getLabel() {
        return label;
    }
}
