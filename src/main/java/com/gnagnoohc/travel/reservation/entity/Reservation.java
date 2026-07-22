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
    private LocalDateTime createdAt;

    // 취소 요청 시 기록 (관리자 검토용)
    private String cancelReason;
    private LocalDateTime cancelRequestedAt;
}
