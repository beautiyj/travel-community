package com.gnagnoohc.travel.batch.converter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gnagnoohc.travel.batch.dto.*;
import com.gnagnoohc.travel.batch.helper.TourApiHelper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TourDataConverter {
    private final TourApiHelper tourApiHelper;

    // TODO: 2개의 시스템 유령 계정(각각 1개싹 지정) & 그 외 NNNN개 소유 유령계정 1개. 총 2~3개의 유령계정 설정
    // 공공데이터 전용 가상 비즈니스 회원 PK (시스템 유령계정)
    private static final Integer PUBLIC_DATA_MEMBER_ID = 1;

    // // TourLdongCodeDTO -> RegionDTO 변환
    // public RegionDTO convertToRegionDTO(TourLdongCodeDTO ldongCodeDTO) {
    //     // 공통 헬퍼 parseRegionId를 통해 regionId(Long) 추출 (Y/N 응답 필드 분기 처리)
    //     // regnCd는 y일 때 lDongRegnCd 시도코드 n일때 일반 code (시군구코드는 자동으로 y일 때만 들어오는 응답)
    //     String regnCd = StringUtils.hasText(ldongCodeDTO.getLDongRegnCd()) ? ldongCodeDTO.getLDongRegnCd() : ldongCodeDTO.getCode();
    //     String signguCd = ldongCodeDTO.getLDongSignguCd();

    //     Integer regionId = tourApiHelper.parseRegionId(regnCd, signguCd);
    //     if (regionId == null) return null;

    //     // 법정동 명칭 시도명/시군구명 - 법정동 명칭은 "서울시종로구"가 아닌 "서울시 종로구"와 같이 공백(띄어쓰기)을 포함하도록 가공 처리
    //     // 법정동 명칭은 동기화 로직에 없고 메타데이터인 법정동 목록 조회에만 존재
    //     // rawName는 y일 때 lDongSignguCd 시도코드 n일때 일반 name (시군구명칭은 자동으로 y일 때만 들어오는 응답)
        
    //     // 0729
    //     // String rawName = StringUtils.hasText(ldongCodeDTO.getLDongRegnNm()) ? ldongCodeDTO.getLDongRegnNm() : ldongCodeDTO.getName();
    //     // [수정] 시도 명칭과 시군구 명칭 조합 안전하게 처리
    //     String rawName = StringUtils.hasText(ldongCodeDTO.getLDongRegnNm()) ? ldongCodeDTO.getLDongRegnNm() : ldongCodeDTO.getName();

    //     if (StringUtils.hasText(ldongCodeDTO.getLDongSignguNm())) { rawName += " " + ldongCodeDTO.getLDongSignguNm(); }

    //     /* 상위 지역(시/도)과 하위 지역(시/군/구)의 계층 구조(부모-자식 관계) 설정
    //     코드 길이가 5자리 이상(시/군/구 데이터)일 때만 부모 ID 파싱하기
    //     - 2자리(시/도 단독, 예: "11" 서울): 최상위 지역이므로 부모가 없음 -> parentRegionId = null
    //     - 5자리 이상(시/군/구 결합, 예: "11110" 종로구): 앞 2자리("11")를 추출하여 부모 시/도 PK 세팅 -> parentRegionId = 11L */
    //     String rawCodeStr = regionId.toString();

    //     // 0729
    //     // Integer parentRegionId = (rawCodeStr.length() >= 5) ? Integer.parseInt(rawCodeStr.substring(0, 2)) : null;
    //     // [수정] regionId가 시도 코드 자체인 경우(예: 11, 26 등 길이가 2~3자리인 경우) 부모는 무조건 null
    //     // 5자리 이상일 때만 앞 2자리를 부모 ID로 추출
    //     Integer parentRegionId = null;
    //     if (rawCodeStr.length() >= 5) {
    //         parentRegionId = Integer.parseInt(rawCodeStr.substring(0, 2));
    //     } else {
    //         parentRegionId = null; // 시/도 단독 레벨은 부모가 없음
    //     }

    //     // 최종 RegionDTO 빌드 및 적재
    //     return RegionDTO.builder()
    //             .regionId(regionId)
    //             .regionName(rawName)
    //             .parentRegionId(parentRegionId)
    //             .build();
    // }
    //0729
    // TourLdongCodeDTO -> RegionDTO 변환
    public RegionDTO convertToRegionDTO(TourLdongCodeDTO ldongCodeDTO) {
        String regnCd = StringUtils.hasText(ldongCodeDTO.getLDongRegnCd()) ? ldongCodeDTO.getLDongRegnCd() : ldongCodeDTO.getCode();
        String signguCd = ldongCodeDTO.getLDongSignguCd();

        // 1. regionId 생성 (시도코드 + 시군구코드 결합 또는 시도코드 단독)
        String rawCode = regnCd + (StringUtils.hasText(signguCd) ? signguCd : "");
        Integer regionId = null;
        try {
            regionId = Integer.parseInt(rawCode);
        } catch (NumberFormatException e) {
            return null;
        }

        // 2. 지역 명칭 가공 (시도명 + 시군구명)
        String rawName = StringUtils.hasText(ldongCodeDTO.getLDongRegnNm()) ? ldongCodeDTO.getLDongRegnNm() : ldongCodeDTO.getName();
        if (StringUtils.hasText(ldongCodeDTO.getLDongSignguNm())) { 
            rawName += " " + ldongCodeDTO.getLDongSignguNm(); 
        }

        // 3. 부모 ID(parentRegionId) 설정
        // 만약 자식 데이터(시군구 코드가 존재하거나, 코드가 5자리 이상)라면 앞 2자리(시도 코드)를 부모 ID로 지정
        // 단독 시도 데이터라면 parentRegionId는 무조건 null
        Integer parentRegionId = null;
        if (StringUtils.hasText(signguCd) || rawCode.length() >= 5) {
            try {
                // 시도 코드(regnCd)를 정수로 변환하여 부모 ID로 확실히 지정
                parentRegionId = Integer.parseInt(regnCd);
            } catch (NumberFormatException e) {
                parentRegionId = Integer.parseInt(rawCode.substring(0, 2));
            }
        } else {
            parentRegionId = null; // 최상위 시/도 부모 없음
        }

        return RegionDTO.builder()
                .regionId(regionId)
                .regionName(rawName)
                .parentRegionId(parentRegionId)
                .build();
    }

    // TODO: 이후 useFeeInfo, minPrice 처리에서 0원 혹은 가격없음 -> TOUR 서비스단에서 무료 & 가격변동으로 텍스트 매핑처리
    // TourLclsSystmCodeDTO -> PlaceDTO 변환
    // 메타데이터인 법정동코드가 아닌, 실제정보가 필요한 동기화 API TourAreaBasedSyncListDTO를 플레이스에 넣어야 함
    // thumbnailImage를 5번째 파라미터로 받도록 변경 - PlaceImage 판단 로직(resolveThumbnailImage)에서 계산된 값을 전달받는 구조로 전환
    public PlaceDTO convertToPlaceDTO(TourAreaBasedSyncListDTO syncItem, TourItemDTO tourItem, TourDetailIntroDTO introDetail, TourDetailInfoDTO infoDetail, String thumbnailImage) {
        // 공공데이터의 contentId -> Place테이블엔 pk로 기입, 더미데이터의 경우 난수처리하여 넣을 것.
        Integer placeId = Integer.parseInt(syncItem.getContentid());
        // 공통헬퍼 메소드 parseRegionId 사용하여 동기화 로직의 법정동 시도코드/시군구코드 처리
        Integer regionId = tourApiHelper.parseRegionId(syncItem.getLDongRegnCd(), syncItem.getLDongSignguCd());
        // 플레이스 타입은 숫자로 들어오는 걸 convertContentType에서 tour/food/stay로 변환 처리
        String placeType = tourApiHelper.convertContentType(syncItem.getContenttypeid());
        // 주소는 addr1 + addr2 합친 전체주소 하나로 처리
        String fullAddress = syncItem.getAddr1() +
                (StringUtils.hasText(syncItem.getAddr2()) ? " " + syncItem.getAddr2() : "");
        BigDecimal mapx = StringUtils.hasText(syncItem.getMapx()) ? new BigDecimal(syncItem.getMapx()) : null;
        BigDecimal mapy = StringUtils.hasText(syncItem.getMapy()) ? new BigDecimal(syncItem.getMapy()) : null;
        // 헬퍼 메소드 extractFeeInfo - 타입별 DTO에서 요금 원문 안내 텍스트(useFeeInfo) 추출
        String useFeeInfo = tourApiHelper.extractFeeInfo(introDetail, infoDetail, placeType);
        // 헬퍼 메소드 parseMinPrice - extractFeeInfo에서 추출한 원문 텍스트를 전달하여 검색/정렬용 최저가 숫자(minPrice) 파싱
        Integer minPrice = tourApiHelper.parseMinPrice(useFeeInfo);
        // 해시태그는 generateHashtags에서 처리 (TourItemDTO + TourDetailIntroDTO 조합으로 해시태그 생성)
        String hashtags = generateHashtags(tourItem, introDetail, placeType);

        return PlaceDTO.builder()
                .placeId(placeId)
                .regionId(regionId)
                .memberId(PUBLIC_DATA_MEMBER_ID)
                .placeType(placeType)
                .name(syncItem.getTitle())
                .description(tourItem.getOverview())
                .address(fullAddress)
                .mapx(mapx)
                .mapy(mapy)
                .useFeeInfo(useFeeInfo)
                .minPrice(minPrice)
                .isClosed("0".equals(syncItem.getShowflag()))
                .firstImage(thumbnailImage) // 조인 없는 카드 리스트용 1차 썸네일 세팅
                .hashtags(hashtags)
                .peopleCount(1)
                .build();
    }

    // TODO: footer에 TourDetailImageDTO - cpyrhtDivCd (저작권표기) 추가 필요 & 프론트에서 Type3의 경우 비율유지하며 적용 필요
    // TourDetailImageDTO -> PlaceImageDTO 변환
    // 대표 이미지 등록 시 sortOrder=0 지정, 상세/서브 이미지 등록 시 순번(sortOrder) 지정
    public PlaceImageDTO convertToPlaceImageDTO(Integer placeId, String imageUrl, int sortOrder) {
        if (!StringUtils.hasText(imageUrl) || placeId == null) {
            return null;
        }
        return PlaceImageDTO.builder()
                .placeId(placeId)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }

    // 카드형 썸네일 대표 이미지 결정: 목록 API의 썸네일(firstimage2) 우선, 없을 경우 원본(firstimage) Fallback 사용
    // PlaceImage 판단 로직을 여기서 계산해서 convertToPlaceDTO 호출 시 파라미터로 전달하는 구조 (TODO 반영)
    public String resolveThumbnailImage(TourAreaBasedSyncListDTO syncItem) {
        return StringUtils.hasText(syncItem.getFirstimage2())
                ? syncItem.getFirstimage2()
                : syncItem.getFirstimage();
    }

    // TourAreaBasedSyncListDTO 동기화 목록에서 가져온 장소 하나의 기본 정보를
    // 일부 공통 정보가 담긴 TourItemDTO에서 1차 처리 -> 최종 컨버터로는 TourItemDTO만 전달하는 방식
    public TourItemDTO convertToTourItemDTO(TourAreaBasedSyncListDTO syncItem) {
        TourItemDTO item = new TourItemDTO();
        item.setContentid(syncItem.getContentid());
        item.setContenttypeid(syncItem.getContenttypeid());
        item.setCreatedtime(syncItem.getCreatedtime());
        item.setModifiedtime(syncItem.getModifiedtime());
        item.setTitle(syncItem.getTitle());
        // TODO: 이후 지도 API 활용 시 메모리에서 dist 꺼내 쓰는 방식 / dist는 특정 좌표(사용자 위치) 기준 상대적인 거리라서 기본 PlaceDTO엔 넣을 필요 없음
        // dist 처리 (문자열로 들어올 경우 Double로 파싱, 없으면 null)
        if (StringUtils.hasText(syncItem.getDist())) {
            try { item.setDist(Double.parseDouble(syncItem.getDist())); }
            catch (NumberFormatException e) { item.setDist(null); }
        }
        item.setAddr1(syncItem.getAddr1());
        item.setAddr2(syncItem.getAddr2());
        item.setFirstimage(syncItem.getFirstimage());
        item.setFirstimage2(syncItem.getFirstimage2());
        item.setCpyrhtDivCd(syncItem.getCpyrhtDivCd());
        item.setMapx(syncItem.getMapx());
        item.setMapy(syncItem.getMapy());
        item.setMlevel(syncItem.getMlevel());
        item.setTel(syncItem.getTel());
        item.setZipcode(syncItem.getZipcode());
        item.setLDongRegnCd(syncItem.getLDongRegnCd());
        item.setLDongSignguCd(syncItem.getLDongSignguCd());
        item.setLclsSystm1(syncItem.getLclsSystm1());
        item.setLclsSystm2(syncItem.getLclsSystm2());
        item.setLclsSystm3(syncItem.getLclsSystm3());
        return item;
    }

    // generateHashtags 생성 헬퍼 메소드 - TourItemDTO + TourDetailIntroDTO 기반 동적 해시태그 생성
    private String generateHashtags(TourItemDTO item, TourDetailIntroDTO intro, String placeType) {
        List<String> tags = new ArrayList<>();
        if (StringUtils.hasText(placeType)) tags.add("#" + placeType);

        if (intro != null) {
            if ("tour".equals(placeType)) {
                parseTourHashtags(intro, tags);
            } else if ("stay".equals(placeType)) {
                parseStayHashtags(intro, tags);
            } else if ("food".equals(placeType)) {
                parseFoodHashtags(intro, tags);
            }
        } else if ("tour".equals(placeType)) {
            tags.add("#무료");
        }

        if (StringUtils.hasText(item.getAcmpyPsblCpam()) || StringUtils.hasText(item.getPetTursmInfo())) {
            tags.add("#반려동물동반");
        }

        List<String> uniqueTags = tags.stream().filter(StringUtils::hasText).distinct().toList();
        return String.join(",", uniqueTags);
    }

    private void parseTourHashtags(TourDetailIntroDTO intro, List<String> tags) {
        String useFee = intro.getUsefee();
        if (StringUtils.hasText(useFee)) {
            String cleanFee = useFee.replaceAll("<[^>]*>", "").trim();
            if (cleanFee.contains("무료") || cleanFee.contains("없음") || "0".equals(cleanFee)) {
                tags.add("#무료");
            } else {
                String shortFee = cleanFee.length() > 10 ? cleanFee.substring(0, 10) : cleanFee;
                tags.add("#" + shortFee.replaceAll(" ", ""));
            }
        } else {
            tags.add("#무료");
        }
    }

    private void parseStayHashtags(TourDetailIntroDTO intro, List<String> tags) {
        if (StringUtils.hasText(intro.getRoomtype())) {
            tags.add("#" + intro.getRoomtype().replaceAll(" ", ""));
        }
        if (StringUtils.hasText(intro.getRoomcount())) {
            tags.add("#객실" + intro.getRoomcount() + "개");
        }
    }

    private void parseFoodHashtags(TourDetailIntroDTO intro, List<String> tags) {
        String menu = StringUtils.hasText(intro.getFirstmenu()) ? intro.getFirstmenu() : intro.getTreatmenu();
        if (StringUtils.hasText(menu)) {
            String cleanMenu = menu.replaceAll("<[^>]*>", "").trim();
            String[] menuArray = cleanMenu.split("[,/\\n]");
            for (int i = 0; i < Math.min(menuArray.length, 2); i++) {
                String singleMenu = menuArray[i].trim().replaceAll(" ", "");
                if (StringUtils.hasText(singleMenu) && singleMenu.length() <= 12) {
                    tags.add("#" + singleMenu);
                }
            }
        }
    }
}