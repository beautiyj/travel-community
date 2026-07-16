package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // 서비스코드용 전체 생성자
public class RegionDTO {
    private Long regionId;
    private String regionName;
    private Long parentRegionId;
}