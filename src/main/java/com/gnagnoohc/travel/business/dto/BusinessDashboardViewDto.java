package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class BusinessDashboardViewDto {
    // 업소명·대표명·마감상태·뱃지 카운트는 모든 business 화면이 공유하는 값이라 사이드바 DTO를 그대로 담는다
    private BusinessSidebarContextDto sidebar;
    private String todayLabel;
    private List<BusinessReservationDto> todayReservations;
    private List<BusinessMonthlyTrendDto> monthlyTrend;
    private Integer monthlyCount;
    private Integer todayVisitors;
    private BusinessReviewSentimentCountsDto reviewSentiment;
}
