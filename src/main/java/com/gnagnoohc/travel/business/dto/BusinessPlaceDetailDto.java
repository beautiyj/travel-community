package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BusinessPlaceDetailDto {
    private Long placeId;
    private String name;
    private String placeType;
    private Integer minPrice;
    private Long regionId;
    private String regionName;
    private String address;
    private String addressDetail;
    private String description;
    private String hashtags;
    private Integer closed;
    private List<String> images;

    // DB에 저장된 원본 ("[라벨] 값"을 줄바꿈으로 이어붙인 한 덩어리)
    private String extraInfo;
    // 위 원본을 "라벨 -> 값"으로 풀어둔 것. 저장 순서가 유지된다.
    // 읽기 뷰의 목록 출력과 수정 폼의 기존값 채우기에 모두 쓴다. (BusinessPlaceService.findDetail이 채운다)
    private Map<String, String> extraInfoMap;
}