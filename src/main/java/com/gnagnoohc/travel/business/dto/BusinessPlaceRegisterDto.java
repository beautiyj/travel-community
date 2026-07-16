package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 사업자 직접등록 INSERT용 business 자체 DTO. tour.model.PlaceEntity(팀원 소유)는 참조하지 않는다.
@Getter
@Builder
public class BusinessPlaceRegisterDto {

    // DB CK_PLACE_ADMIN_TYPE과 동일한 의미: 0=공공데이터 시드, 1=사업자 등록
    public static final int ADMIN_TYPE_OWNER_REGISTERED = 1;

    private String contentId;
    private String contentTypeId;
    private Integer placeType;
//    private Long regionId;
    private Long memberId;
    private String name;
    private String description;
    private String address;
//    private BigDecimal mapx;
//    private BigDecimal mapy;
    private String firstImage;
    private int adminType;

    // INSERT 시 MyBatis useGeneratedKeys로 채워 받는 값. 빌더 대상 아님.
    private Long placeId;

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }
}
