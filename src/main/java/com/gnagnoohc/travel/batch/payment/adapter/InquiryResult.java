package com.gnagnoohc.travel.batch.payment.adapter;

/**
 * PG 조회 결과 — 상태값과, PAID일 때 PG가 돌려준 paymentKey를 함께 담는다.
 *
 * <p>paymentKey를 실어 나르는 이유: 유령 결제 보정 시 PAYMENT 행을 저장하려면
 * {@code payment_key}(NOT NULL) 컬럼을 채워야 하는데, 콜백을 놓친 상황에선
 * 이 값을 조회로 되받아오는 수밖에 없기 때문이다.
 */
public record InquiryResult(Status status, String paymentKey) {

    public enum Status {
        PAID,       // 실제 결제됨 → 예약 PAID로 보정 (유령 결제 구제)
        FAILED,     // PG가 실패/중단이라 응답 → 실패 이력 후 정리 대상
        NOT_PAID    // 아직 결제 안 됨/알 수 없음 → 그대로 두고 다음 회차에 재확인
    }

    public static InquiryResult paid(String paymentKey) {
        return new InquiryResult(Status.PAID, paymentKey);
    }

    public static final InquiryResult FAILED   = new InquiryResult(Status.FAILED, null);
    public static final InquiryResult NOT_PAID = new InquiryResult(Status.NOT_PAID, null);
}
