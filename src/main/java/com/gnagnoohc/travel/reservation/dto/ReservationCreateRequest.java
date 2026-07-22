package com.gnagnoohc.travel.reservation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class ReservationCreateRequest {
    private Long placeId;
    private String visitorName;
    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    private int headcount;
}
