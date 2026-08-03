package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private Integer placeId;
    private Integer memberId;
    private Integer regionId;
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
    private Integer peopleCount;   // 정원 / 기준 인원 수 컬럼

    private String extraInfo;
    
    private String regionName;   // REGION 테이블 join 전용 - DB PLACE 테이블엔 없는 조회 전용 필드

    // 화면 표시용 요금 가공 Getter (DTO 전용)
    public String getDisplayPrice() {
        // 음식점(food)인 경우 "가격변동"
        if ("food".equalsIgnoreCase(this.placeType)) { return "가격변동"; }

        // minPrice가 0원인 경우 (무료 또는 파싱 불가능 문구)
        if (minPrice != null && minPrice == 0) {
            if (useFeeInfo != null && useFeeInfo.contains("무료")) { return "무료"; }
            return "가격변동";
        }

        // 정상 최저가가 있는 경우
        if (minPrice != null && minPrice > 0) {
            return String.format("%,d원~", minPrice); // 콤마 포맷팅 (예: 10,000원~)
        }

        // 원문 텍스트 fallback 처리
        if (useFeeInfo != null && !useFeeInfo.isBlank()) {
            return useFeeInfo;
        }

        return "가격 변동";
    }

}