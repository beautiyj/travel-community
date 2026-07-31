package com.gnagnoohc.travel.mypage.dto;

import lombok.Data;

@Data
public class BusinessPlaceImageDto {

    private Long imageId;
    private Long placeId;
    private String imageUrl;
    private Integer sortOrder;
}
