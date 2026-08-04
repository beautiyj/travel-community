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
    private String placeType;
    private String priceType;
    private Integer minPrice;
//    private Long regionId;
    private String address;
    private String description;
    // "[라벨] 값"을 줄바꿈으로 이어붙인 부가정보 한 덩어리 (공공데이터 배치와 동일 포맷). 입력이 없으면 null
    private String extraInfo;
    private String firstImage;
    private String hashtags;
}