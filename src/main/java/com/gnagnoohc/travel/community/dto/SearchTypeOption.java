package com.gnagnoohc.travel.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 커뮤니티 검색 타입 드롭다운(dropdownSelector.jsp)이 기대하는 code/name 쌍.
// JSP EL이 getCode()/getName()으로 접근하므로 record가 아닌 일반 빈으로 작성
@Getter
@AllArgsConstructor
public class SearchTypeOption {
    private final String code;
    private final String name;

    // dropdownSelector.jsp가 RegionDTO와 공용으로 item.regionId 등을 먼저 평가함(EL은 getter가
    // 없으면 PropertyNotFoundException을 던지므로, null 반환용 getter로 존재만 시켜서 code/name 폴백을 타게 함)
    public Integer getRegionId() { return null; }
    public String getShortName() { return null; }
    public String getRegionName() { return null; }
}
