package com.gnagnoohc.travel.mypage.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class BusinessPlaceDto {

    private Long placeId;
    private Long memberId;
    private Long regionId;
    private String placeType;
    private String name;
    private String description;
    private String address;
    private Boolean closed;
    private String firstImage;
    private String hashtags;
    private Integer minPrice;
    private List<BusinessPlaceImageDto> images = new ArrayList<>();
}
