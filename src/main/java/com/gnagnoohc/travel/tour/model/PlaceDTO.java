package com.gnagnoohc.travel.tour.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private Long placeId;
    private String contentId;
    private String contentTypeId;
    private Integer placeType;
    private Long regionId;
    private Long memberId;
    private String name;
    private String description;
    private String address;
    private BigDecimal mapx;
    private BigDecimal mapy;
    private boolean isClosed;
    private int adminType;
    private String firstImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}