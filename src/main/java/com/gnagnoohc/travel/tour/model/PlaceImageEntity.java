package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceImageEntity {
    private Long imageId;
    private Integer placeId;
    private String imageUrl;
    private Integer sortOrder;
}