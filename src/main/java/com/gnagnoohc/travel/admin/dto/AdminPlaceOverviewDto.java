package com.gnagnoohc.travel.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPlaceOverviewDto {
    private Long placeId;
    private String placeName;
    private String ownerName;
    private boolean isClosed;
}
