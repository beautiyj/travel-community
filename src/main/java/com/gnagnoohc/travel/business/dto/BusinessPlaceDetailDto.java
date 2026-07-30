package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BusinessPlaceDetailDto {
    private Long placeId;
    private String name;
    private String placeType;
    private String priceType;
    private Integer minPrice;
    private Long regionId;
    private String regionName;
    private String address;
    private String description;
    private boolean closed;
    private List<String> images;
}