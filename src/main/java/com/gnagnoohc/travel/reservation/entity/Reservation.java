package com.gnagnoohc.travel.reservation.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class Reservation {

    // 팀 DB 상태값 컨벤션 (RESERVATION.status DEFAULT '예약중')
    public static final String STATUS_PENDING          = "예약중";
    public static final String STATUS_PAID             = "예약완료";   // 팀에서 다른 단어 쓰면 여기만 수정
    public static final String STATUS_CANCEL_REQUESTED = "취소요청";   // 결제완료 후 사용자가 취소 요청, 관리자 검토 대기
    public static final String STATUS_CANCELED         = "예약취소";
    public static final String STATUS_EXPIRED          = "예약만료";   // 30분 내 미결제 시 스케줄러가 전환

    private Long reservationId;
    private Long memberId;
    private Long placeId;
    private String visitorName;
    private String phone;
    private LocalDate visitDate;
    private int headcount;
    private String status;          // 예약중 / 예약완료 / 취소요청 / 예약취소 / 예약만료
    private LocalDateTime createdAt;

    // 취소 요청 시 기록 (관리자 검토용)
    private String cancelReason;
    private LocalDateTime cancelRequestedAt;
}
