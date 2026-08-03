package com.gnagnoohc.travel.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 대시보드에 표시할 사업자 인증 신청 상태별 건수입니다.
 */
@Getter
@Setter
public class AdminDashboardSummaryDto {

	private long totalCount;
	private long pendingCount;
	private long approvedCount;
	private long rejectedCount;
}
