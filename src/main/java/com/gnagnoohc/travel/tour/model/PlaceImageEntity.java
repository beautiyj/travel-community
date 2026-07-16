package com.gnagnoohc.travel.tour.model;

import lombok.Getter;

@Getter
public class PlaceImageEntity {
    private Long imageId;
    private Long placeId;
    private String imageUrl;
    private Integer sortOrder;
}