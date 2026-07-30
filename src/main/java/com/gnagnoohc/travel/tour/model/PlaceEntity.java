package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TODO: 공공데이터 필터링 최종 마무리 후 SQL도 수정 필요함 0730기준 isClosed 자료형 int(0,1)바꾸기 및 공통sql에서 최종 작업 후 테이블생성쿼리 재전달
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceEntity {
    private Integer placeId;
    private Integer regionId;
    private Integer memberId;
    private String placeType;      // tour / food / stay 변환값으로 들어옴
    private String name;           // title공공데이터 -> name으로받아와서저장
    private String description;
    private String address;        // address (addr1+addr2)
    private BigDecimal mapx;       // mapx (DECIMAL 12,9, 경도)
    private BigDecimal mapy;       // mapy (DECIMAL 12,9, 위도)
    private boolean isClosed;      // BOOLEAN, 1:문닫음, 0:영업중
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TODO: 데베연결 후 플레이스테이블의 이미지로 썸네일이미지 들어오는지 - 이미지테이블엔 오리지널이미지 들어오는지 확인하기(확인후주석삭제)
    private String firstImage;     // first_image (VARCHAR 500, 대표 이미지 URL - 썸네일 설정)
    private String hashtags;       // hashtags (VARCHAR 300, 가공 해시태그)

    private Integer minPrice;      // 검색/필터링/정렬용 최저가 (숫자 연산용)
    private String useFeeInfo;     // 화면 표시용 요금 안내 원문 (음식점은 null 또는 대표메뉴 텍스트)

    private Integer peopleCount;   // 정원 / 기준 인원 수 컬럼

    // 사업자 정보 수동 수정용 메서드
    public void updateBusinessInfo(String name, String description, String address, Integer peopleCount) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.peopleCount = peopleCount;
        this.updatedAt = LocalDateTime.now();
    }

    // 휴,폐업 상태 전환용 메서드
    public void changeOperatingStatus(boolean isClosed) {
        this.isClosed = isClosed;
        this.updatedAt = LocalDateTime.now();
    }

    // 사업자 회원이 신규 업장을 직접 등록할 때 사용하는 정적 팩토리 메서드(더미데이터)
    public static PlaceEntity createOwnerPlace(String placeType, Integer regionId, Integer memberId, 
                                                String name, String description, String address, 
                                                BigDecimal mapx, BigDecimal mapy, 
                                                String firstImage, String hashtags, Integer peopleCount) {
        PlaceEntity place = new PlaceEntity();

        // TODO: 공공데이터의 contentId -> PlaceId설정이므로 비즈니스에선 중복 방지 난수/특수 대역 세팅하여 더미데이터 기입 필요
        int dummyPlaceId = 900_000_000 + (int)(Math.random() * 99_999_999);
        place.placeId = dummyPlaceId;

        place.placeType = placeType;
        place.regionId = regionId;
        place.memberId = memberId;
        place.name = name;
        place.description = description;
        place.address = address;
        place.mapx = mapx;
        place.mapy = mapy;
        place.isClosed = false;
        place.firstImage = firstImage;
        place.hashtags = hashtags;
        place.peopleCount = (peopleCount != null) ? peopleCount : 1; // 기본값 1 처리
        place.createdAt = LocalDateTime.now();
        place.updatedAt = LocalDateTime.now();
        return place;
    }
}