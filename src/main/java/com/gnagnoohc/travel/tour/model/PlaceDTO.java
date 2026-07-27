package com.gnagnoohc.travel.tour.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private Long placeId;
    private Long memberId;
    private Long regionId;
    private String placeType;
    private String name;
    private String description;
    private String address;
    private BigDecimal mapx;
    private BigDecimal mapy;
    private boolean isClosed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String firstImage;
    private String hashtags;

    private Integer minPrice;       // 최저/대표 가격 (원)
    private String useFeeInfo;      // 요금 안내 원문 텍스트
}