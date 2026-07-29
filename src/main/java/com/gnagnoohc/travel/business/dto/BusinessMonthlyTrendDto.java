package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessMonthlyTrendDto {
    private String monthLabel;
    private Integer bookingCount;
    private Long revenue;
}
