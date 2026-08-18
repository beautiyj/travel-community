package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;

// 업소정보 수정 UPDATE용. BusinessPlaceRegisterDto(INSERT)와 필드가 겹치지만 PK 기준 수정이라 별도 DTO로 분리.
@Getter
@Builder
public class BusinessPlaceUpdateDto {
    private Long placeId;
    private Long memberId;
    private String name;
    private Integer placeType;
//    private Long regionId;
    private String address;
    private String description;
    private String firstImage;
}
