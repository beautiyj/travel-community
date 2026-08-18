package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionDTO {
    private Integer regionId;
    private String regionName;
    private Integer parentRegionId;

    // 화면 표시용 축약 이름 getter
    public String getShortName() {
        if (regionName == null) return "";
        return regionName
                .replace("서울특별시", "서울")
                .replace("전남광주통합특별시", "전남광주통합")
                .replace("부산광역시", "부산")
                .replace("대구광역시", "대구")
                .replace("인천광역시", "인천")
                .replace("대전광역시", "대전")
                .replace("울산광역시", "울산")
                .replace("제주특별자치도", "제주")
                .replace("강원특별자치도", "강원")
                .replace("전북특별자치도", "전북")
                .replace("세종특별자치시", "세종")
                .replace("광역시", "")
                .replace("특별자치도", "")
                .replace("특별시", "");
    }
}
