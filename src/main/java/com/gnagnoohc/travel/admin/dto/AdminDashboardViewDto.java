package com.gnagnoohc.travel.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AdminDashboardViewDto {
    private String placeName;
    private String ownerName;
    private boolean isClosed;
    private String todayLabel;
    private List<AdminReservationDto> todayReservations;
    private Integer monthlyCount;
    private Integer pendingCount;
    private Integer todayVisitors;
    private Integer cancelRequestCount;
}
