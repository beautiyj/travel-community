package com.gnagnoohc.travel.business.dto;

import com.gnagnoohc.travel.reservation.entity.PaymentStatus;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class BusinessReservationDto {
    private Long reservationId;
    private String visitorName;
    private String phone;
    private LocalDate visitDate;
    private Integer headcount;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private String placeName;
    private Integer amount;             // PAYMENT.amount (없으면 null)
    private PaymentStatus paymentStatus; // PAYMENT.payment_status (선결제 여부, 없으면 null)
}
