package com.gnagnoohc.travel.tour.model;

import lombok.Getter;

// REGION 테이블과 1:1 매핑 + 마이바티스 인식용 생성자 엔티티 

@Getter
public class RegionEntity {
    private Long regionId;
    private String regionName;
    private Long parentRegionId;

    // 마이바티스 인식용 생성자
    public RegionEntity(Long regionId, String regionName, Long parentRegionId) {
        this.regionId = regionId;
        this.regionName = regionName;
        this.parentRegionId = parentRegionId;
    }
}