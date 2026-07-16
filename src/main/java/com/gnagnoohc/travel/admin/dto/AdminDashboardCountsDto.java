package com.gnagnoohc.travel.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardCountsDto {
    private Integer monthlyCount;
    private Integer pendingCount;
    private Integer todayVisitors;
    private Integer cancelRequestCount;
}
