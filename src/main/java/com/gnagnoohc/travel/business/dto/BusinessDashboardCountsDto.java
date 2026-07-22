package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessDashboardCountsDto {
    private Integer monthlyCount;
    private Integer pendingCount;
    private Integer todayVisitors;
    private Integer cancelRequestCount;
}
