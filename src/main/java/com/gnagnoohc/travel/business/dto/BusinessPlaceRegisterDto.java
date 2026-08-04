package com.gnagnoohc.travel.business.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 사업자 직접등록 INSERT용 business 자체 DTO. tour.model.PlaceEntity(팀원 소유)는 참조하지 않는다.
@Getter
@Builder
public class BusinessPlaceRegisterDto {

    private String placeType;
    // 숙박(placeType='stay')일 때만 값이 있고, 비어 있으면 무료. 숙박이 아니면 항상 null
    private Integer minPrice;
    private Long regionId;
    private Long memberId;
    private String name;
    private String description;
    // "[라벨] 값"을 줄바꿈으로 이어붙인 부가정보 한 덩어리 (공공데이터 배치와 동일 포맷). 입력이 없으면 null
    private String extraInfo;
    private String address;
//    private BigDecimal mapx;
//    private BigDecimal mapy;
    private String firstImage;
    private String hashtags;

    // place_id는 AUTO_INCREMENT가 아니라 수동 채번(공공데이터는 contentId, 직접등록은 난수). INSERT 전에 서비스가 채워 넣는다.
    private Long placeId;
}