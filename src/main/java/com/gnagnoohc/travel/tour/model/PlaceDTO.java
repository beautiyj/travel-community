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
    
    // 0801
    // 기존 필드들 아래에 추가
    private String regionName;   // REGION 테이블 join 전용 - DB PLACE 테이블엔 없는 조회 전용 필드

    // 클래스 안에 메서드로 추가 (Lombok @Data가 자동 생성 안 해주는 커스텀 getter)
    public String getDisplayPrice() {
        if (minPrice != null) {
            return minPrice + "원~";
        } else if (useFeeInfo != null && !useFeeInfo.isBlank()) {
            return useFeeInfo;
        }
        return "정보 없음";
    }

}