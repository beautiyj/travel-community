package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceImageDTO {
    private Long imageId;
    private Long placeId;
    private String imageUrl;
    private Integer sortOrder;
}
