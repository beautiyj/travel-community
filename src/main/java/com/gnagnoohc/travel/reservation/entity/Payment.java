package com.gnagnoohc.travel.reservation.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Payment {

    public static final int TYPE_KAKAO = 2;

    private Long paymentId;
    private Long reservationId;
    private int amount;
    private String paymentKey;      // 토스: paymentKey / 카카오: tid
    private PaymentStatus paymentStatus;   // DB 저장은 영어(DONE/CANCELED/FAILED), 표시는 paymentStatus.label
    private String orderId;
    private LocalDateTime paidAt;
    private int paymentType;        // 1 = 토스, 2 = 카카오
}
