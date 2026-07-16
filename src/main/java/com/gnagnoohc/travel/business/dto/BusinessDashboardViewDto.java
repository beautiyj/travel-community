package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class BusinessDashboardViewDto {
    private String placeName;
    private String ownerName;
    private boolean isClosed;
    private String firstImage;
    private String todayLabel;
    private List<BusinessReservationDto> todayReservations;
    private List<BusinessMonthlyTrendDto> monthlyTrend;
    private Integer monthlyCount;
    private Integer pendingCount;
    private Integer todayVisitors;
    private Integer cancelRequestCount;
}
