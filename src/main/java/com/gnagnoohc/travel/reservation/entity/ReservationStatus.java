package com.gnagnoohc.travel.reservation.entity;

/**
 * 예약 상태. DB에는 enum 이름(영어)이 저장되고, 화면 표시는 label(한글)을 쓴다.
 * 팀 DDL 컨벤션: RESERVATION.status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
 */
public enum ReservationStatus {

    PENDING("예약중"),               // 예약 생성 직후, 결제 대기
    PAID("예약완료"),                // 결제 승인 완료 = 예약 확정
    COMPLETED("방문완료"),           // 방문일이 지나 스케줄러가 전환 (당일은 PAID 유지)
    CANCEL_REQUESTED("취소요청"),    // 사용자가 취소 요청, 관리자 검토 대기
    CANCELED("예약취소"),            // 관리자 승인 + 환불 완료
    EXPIRED("예약만료");             // 30분 내 미결제로 스케줄러가 전환

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    /** 화면 표시용 한글 문구 (JSP: ${r.status.label}) */
    public String getLabel() {
        return label;
    }
}
