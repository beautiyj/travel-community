package com.gnagnoohc.travel.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMonthlyTrendDto {
    private String monthLabel;
    private Integer bookingCount;
    private Long revenue;
}
