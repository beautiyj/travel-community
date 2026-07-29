package com.gnagnoohc.travel.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 헤더 지역선택 드롭다운(dropdownSelector.jsp)이 기대하는 code/name 쌍.
// JSP EL이 getCode()/getName()으로 접근하므로 record가 아닌 일반 빈으로 작성
@Getter
@AllArgsConstructor
public class HeaderRegionOption {
    private final String code;
    private final String name;
}
