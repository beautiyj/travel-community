package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessPlaceOverviewDto {
    private Long placeId;
    private String placeName;
    private String ownerName;
    private boolean isClosed;
    private String firstImage;
}
