package com.gnagnoohc.travel.tour.model;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PlaceEntity {

    // admin_type 허용값 상수 (DB의 CK_PLACE_ADMIN_TYPE 제약조건과 동일한 규칙)
    public static final int ADMIN_TYPE_PUBLIC_DATA = 0;   // 공공데이터 시드(기본값)
    public static final int ADMIN_TYPE_OWNER_CLAIMED = 1; // 사업자 등록/클레임

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

    // 사업자 정보 수동 수정용 메서드
    public void updateBusinessInfo(String name, String description, String address) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.updatedAt = LocalDateTime.now();
    }

    // // 사업자 계정 매칭 + 클레임 처리용 메서드 (member_id, admin_type 동시 전환)
    // public void assignOwner(Long memberId) {
    //     this.memberId = memberId;
    //     this.adminType = ADMIN_TYPE_OWNER_CLAIMED;
    //     this.updatedAt = LocalDateTime.now();
    // }

    // 휴,폐업 상태 전환용 메서드
    public void changeOperatingStatus(boolean isClosed) {
        this.isClosed = isClosed;
        this.updatedAt = LocalDateTime.now();
    }

    // 사업자 회원이 신규 업장을 직접 등록할 때 사용하는 정적 팩토리 메서드
    // admin 도메인이 이 메서드를 호출해서 등록용 PlaceEntity를 조립함
    public static PlaceEntity createOwnerPlace(String contentId, Integer placeType, Long regionId,
                                                 Long memberId, String name, String description,
                                                 String address, BigDecimal mapx, BigDecimal mapy) {
        PlaceEntity place = new PlaceEntity();
        place.contentId = contentId;
        place.placeType = placeType;
        place.regionId = regionId;
        place.memberId = memberId;
        place.name = name;
        place.description = description;
        place.address = address;
        place.mapx = mapx;
        place.mapy = mapy;
        place.isClosed = false;
        place.adminType = ADMIN_TYPE_OWNER_CLAIMED;
        place.createdAt = LocalDateTime.now();
        place.updatedAt = LocalDateTime.now();
        return place;
    }
}