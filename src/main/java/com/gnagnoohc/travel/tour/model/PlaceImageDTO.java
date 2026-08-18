package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceImageDTO {
    private Long imageId;
    private Long placeId;
    private String imageUrl;
    private Integer sortOrder;
}
