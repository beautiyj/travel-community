package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 사업자 직접등록 INSERT용 business 자체 DTO. tour.model.PlaceEntity(팀원 소유)는 참조하지 않는다.
@Getter
@Builder
public class BusinessPlaceRegisterDto {

    private Integer placeType;
//    private Long regionId;
    private Long memberId;
    private String name;
    private String description;
    private String address;
//    private BigDecimal mapx;
//    private BigDecimal mapy;
    private String firstImage;

    // INSERT 시 MyBatis useGeneratedKeys로 채워 받는 값. 빌더 대상 아님.
    private Long placeId;

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }
}
