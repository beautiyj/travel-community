package com.gnagnoohc.travel.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 사업자 인증 신청 목록과 상세 화면에서 사용하는 조회 DTO다.
 * 심사 잠금 조회에서는 신청 ID, 회원 ID, 상태 값만 채워서 사용한다.
 */
@Getter
@Setter
public class AdminBusinessApplicationDto {

	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

	private int businessApplicationId;
	private int memberId;
	private String name;
	private LocalDate birth;
	private String phone;
	private String applicationStatus;
	private String rejectionReason;
	private LocalDateTime appliedAt;
	private LocalDateTime reviewedAt;
	private Integer reviewedBy;

	public String getAppliedAtDisplay() {
		return formatDateTime(appliedAt);
	}

	public String getReviewedAtDisplay() {
		return formatDateTime(reviewedAt);
	}

	private String formatDateTime(LocalDateTime value) {
		return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
	}
}
