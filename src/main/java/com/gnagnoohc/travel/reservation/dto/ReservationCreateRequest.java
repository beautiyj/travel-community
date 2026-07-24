package com.gnagnoohc.travel.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "예약자 이름을 입력하세요")
    @Size(max = 50, message = "이름은 50자 이하로 입력하세요")
    private String visitorName;

    // 프론트(reservation-form.js) 검증과 동일 규칙 — 010/011/016~019, 하이픈 유무 모두 허용
    @NotBlank(message = "연락처를 입력하세요")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
             message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    private int headcount;
}
