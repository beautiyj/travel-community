package com.gnagnoohc.travel.reservation.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class Reservation {

    private Long reservationId;
    private Long memberId;
    private Long placeId;
    private String visitorName;
    private String phone;
    private LocalDate visitDate;
    private int headcount;
    private ReservationStatus status;   // DB 저장은 영어(PENDING/PAID/...), 표시는 status.label

    // 결제 준비(tossReady/kakaoReady) 시 채워지는 값. 정합성 보정 배치가 이 orderId로 PG에 조회한다.
    private String  orderId;            // order_id     — 발급한 주문번호 (미시도면 null)
    private Integer paymentType;        // payment_type — 시도 중인 PG (1 토스 / 2 카카오), null=미시도
    private String  pgTid;              // pg_tid       — 카카오 tid (카카오 조회 키). 토스/즉시결제는 미사용(null)

    private LocalDateTime createdAt;

    // 취소 요청 시 기록 (관리자 검토용)
    private String cancelReason;
    private LocalDateTime cancelRequestedAt;
}
