package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // 서비스코드용 전체 생성자
public class RegionDTO {
    private Long regionId;
    private String regionName;
    private Long parentRegionId;
}



