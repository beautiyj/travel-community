package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceEntity {

    private Long placeId;
    private Long regionId;
    private Long memberId;
    private String placeType;      // tour / food / stay 변환값으로 들어옴
    private String name;           // title공공데이터 -> name으로받아와서저장
    private String description;
    private String address;        // address (addr1+addr2)
    private BigDecimal mapx;       // mapx (DECIMAL 12,9, 경도)
    private BigDecimal mapy;       // mapy (DECIMAL 12,9, 위도)
    private boolean isClosed;      // BOOLEAN, 1:문닫음, 0:영업중 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String firstImage;     // first_image (VARCHAR 500, 대표 이미지 URL)
    private String hashtags;       // hashtags (VARCHAR 300, 가공 해시태그)

    private Integer minPrice;      // 검색/필터링/정렬용 최저가 (숫자 연산용)
    private String useFeeInfo;     // 화면 표시용 요금 안내 원문 (음식점은 null 또는 대표메뉴 텍스트)

    // 사업자 정보 수동 수정용 메서드
    public void updateBusinessInfo(String name, String description, String address) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.updatedAt = LocalDateTime.now();
    }

    // 휴,폐업 상태 전환용 메서드
    public void changeOperatingStatus(boolean isClosed) {
        this.isClosed = isClosed;
        this.updatedAt = LocalDateTime.now();
    }

    // 사업자 회원이 신규 업장을 직접 등록할 때 사용하는 정적 팩토리 메서드(더미데이터)
    public static PlaceEntity createOwnerPlace(String placeType, Long regionId, Long memberId, 
                                                String name, String description, String address, 
                                                BigDecimal mapx, BigDecimal mapy, 
                                                String firstImage, String hashtags) {
        PlaceEntity place = new PlaceEntity();
        place.placeType = placeType;
        place.regionId = regionId;
        place.memberId = memberId;          // 넘겨받은 사업자 memberId 세팅
        place.name = name;
        place.description = description;
        place.address = address;
        place.mapx = mapx;
        place.mapy = mapy;
        place.isClosed = false;
        place.firstImage = firstImage;
        place.hashtags = hashtags;
        place.createdAt = LocalDateTime.now();
        place.updatedAt = LocalDateTime.now();
        return place;
    }
}