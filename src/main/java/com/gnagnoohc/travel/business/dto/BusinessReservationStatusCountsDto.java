package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessReservationStatusCountsDto {
    private Integer pendingCount;
    private Integer confirmedCount;
    private Integer doneCount;
    private Integer cancelledCount;
}
