package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 사업자 직접등록 INSERT용 business 자체 DTO. tour.model.PlaceEntity(팀원 소유)는 참조하지 않는다.
@Getter
@Builder
public class BusinessPlaceRegisterDto {

    private String placeType;
    // 가격 유형 FIXED(가격입력)/VARIABLE(가격변동)/FREE(무료). 숙박이 아니면 항상 FREE
    private String priceType;
    // FIXED일 때만 값이 있고 나머지는 null (DB CHECK 제약과 동일한 규칙)
    private Integer minPrice;
//    private Long regionId;
    private Long memberId;
    private String name;
    private String description;
    private String address;
//    private BigDecimal mapx;
//    private BigDecimal mapy;
    private String firstImage;
    private String hashtags;

    // INSERT 시 MyBatis useGeneratedKeys로 채워 받는 값. 빌더 대상 아님.
    private Long placeId;

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }
}