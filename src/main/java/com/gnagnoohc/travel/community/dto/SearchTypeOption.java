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
}
