package com.gnagnoohc.travel.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

// 지역선택 속성
@ControllerAdvice
public class GlobalHeaderAttributes {

    private static final List<HeaderRegionOption> REGIONS = List.of(
            new HeaderRegionOption("서울", "서울"),
            new HeaderRegionOption("부산", "부산"),
            new HeaderRegionOption("제주", "제주")
    );

    @ModelAttribute("regionList")
    public List<HeaderRegionOption> regionList() {
        return REGIONS;
    }
}
