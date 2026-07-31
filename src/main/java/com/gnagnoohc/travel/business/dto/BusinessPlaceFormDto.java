package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 업소 등록/수정 폼(venueFormFields.jsp)의 입력값 묶음.
 * 등록과 수정이 같은 입력 필드를 쓰므로 하나로 받아 서비스까지 그대로 넘긴다.
 * (사진은 MultipartFile이라 별도 파라미터로 받는다)
 */
@Getter
@Setter
public class BusinessPlaceFormDto {
    private String name;
    private String placeType;
    // 가격은 숙박일 때만 폼에 노출되므로 두 값 모두 비어 있을 수 있다. 정리/검증은 서비스가 담당
    private String priceType;
    private Integer minPrice;
    private Long regionId;
    private String address;
    private String description;
}
