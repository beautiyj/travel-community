package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessReservationStatusCountsDto {
    private Integer cancelRequestCount;
    private Integer confirmedCount;
    private Integer doneCount;
    private Integer cancelledCount;
}
