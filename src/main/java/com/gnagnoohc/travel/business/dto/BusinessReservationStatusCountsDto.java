package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessReservationStatusCountsDto {
    // 결제완료(PAID) = 사업자가 확정/거절을 눌러줘야 하는 대기 건
    private Integer paidCount;
    private Integer cancelRequestCount;
    private Integer confirmedCount;
    private Integer doneCount;
    private Integer cancelledCount;
}
