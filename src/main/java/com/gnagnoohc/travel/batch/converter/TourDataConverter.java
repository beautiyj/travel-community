package com.gnagnoohc.travel.batch.converter;

import com.gnagnoohc.travel.batch.dto.*;
import com.gnagnoohc.travel.batch.helper.TourApiHelper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TourDataConverter {
    private final TourApiHelper tourApiHelper;
    private final HashtagGenerator hashtagGenerator;

    // TODO: 2개의 시스템 유령 계정(각각 1개싹 지정) & 그 외 NNNN개 소유 유령계정 1개. 총 2~3개의 유령계정 설정
    // 공공데이터 전용 가상 비즈니스 회원 PK (시스템 유령계정)
    private static final Integer PUBLIC_DATA_MEMBER_ID = 1;

    // TourLdongCodeDTO -> RegionDTO 변환
    public RegionDTO convertToRegionDTO(TourLdongCodeDTO ldongCodeDTO) {
        // 법정동코드는 무조건 Y 전체목록조회 로직으로 진행
        String regnCd = StringUtils.hasText(ldongCodeDTO.getLDongRegnCd()) ? ldongCodeDTO.getLDongRegnCd() : ldongCodeDTO.getCode();
        String signguCd = ldongCodeDTO.getLDongSignguCd();

        // regionId 생성 (시도코드 + 시군구코드 결합 또는 시도코드 단독)
        String rawCode = regnCd + (StringUtils.hasText(signguCd) ? signguCd : "");
        Integer regionId = null;
        try { regionId = Integer.parseInt(rawCode); }
        catch (NumberFormatException e) { return null; }

        // 지역 명칭 가공 처리 (시도명 + 시군구명)
        String rawName = StringUtils.hasText(ldongCodeDTO.getLDongRegnNm()) ? ldongCodeDTO.getLDongRegnNm() : ldongCodeDTO.getName();
        if (StringUtils.hasText(ldongCodeDTO.getLDongSignguNm())) { rawName += " " + ldongCodeDTO.getLDongSignguNm(); }

        // 부모 ID(parentRegionId) 설정
        // 자식 데이터(시군구 코드가 존재하거나, 코드가 5자리 이상)라면 앞 2자리(시도 코드)를 부모 ID로 지정
        // 단독 시도 데이터라면 parentRegionId는 무조건 null
        Integer parentRegionId = null;
        if (StringUtils.hasText(signguCd) || rawCode.length() >= 5) { parentRegionId = Integer.parseInt(regnCd); }
        else { parentRegionId = null; }

        return RegionDTO.builder()
                .regionId(regionId)
                .regionName(rawName)
                .parentRegionId(parentRegionId)
                .build();
    }

    // TourLclsSystmCodeDTO -> PlaceDTO 변환
    // 메타데이터인 법정동코드가 아닌, 실제정보가 필요한 동기화 API TourAreaBasedSyncListDTO를 플레이스에 넣어야 함
    // thumbnailImage를 5번째 파라미터로 받도록 변경 - PlaceImage 판단 로직(resolveThumbnailImage)에서 계산된 값을 전달받는 구조로 전환
    public PlaceDTO convertToPlaceDTO(TourAreaBasedSyncListDTO syncItem, TourItemDTO tourItem, TourDetailIntroDTO introDetail, TourDetailInfoDTO infoDetail, String thumbnailImage) {
        // 공공데이터의 contentId -> Place테이블엔 pk로 기입, 더미데이터의 경우 난수처리하여 넣을 것.
        Integer placeId = Integer.parseInt(syncItem.getContentid());
        // 공통헬퍼 메소드 parseRegionId 사용하여 동기화 로직의 법정동 시도코드/시군구코드 처리
        Integer regionId = tourApiHelper.parseRegionId(syncItem.getLDongRegnCd(), syncItem.getLDongSignguCd());
        // 플레이스 타입은 숫자로 들어오는 걸 convertContentType에서 tour/food/stay로 변환 처리 + 0730헬퍼의 예외처리용 파라미터 추가되면서 일부 수정적용
        String placeType = tourApiHelper.convertContentType(syncItem.getContenttypeid(), syncItem.getLclsSystm2(), syncItem.getLclsSystm3());
        // 주소는 addr1 + addr2 합친 전체주소 하나로 처리
        String fullAddress = syncItem.getAddr1() +
                (StringUtils.hasText(syncItem.getAddr2()) ? " " + syncItem.getAddr2() : "");
        BigDecimal mapx = StringUtils.hasText(syncItem.getMapx()) ? new BigDecimal(syncItem.getMapx()) : null;
        BigDecimal mapy = StringUtils.hasText(syncItem.getMapy()) ? new BigDecimal(syncItem.getMapy()) : null;
        // 헬퍼 메소드 extractFeeInfo - 타입별 DTO에서 요금 원문 안내 텍스트(useFeeInfo) 추출
        String useFeeInfo = tourApiHelper.extractFeeInfo(introDetail, infoDetail, placeType);
        // 헬퍼 메소드 parseMinPrice - extractFeeInfo에서 추출한 원문 텍스트를 전달하여 검색/정렬용 최저가 숫자(minPrice) 파싱
        Integer minPrice = tourApiHelper.parseMinPrice(useFeeInfo);

        // TODO: 부가정보컬럼 추가 - 부가정보(주차, 휴무일, 영업시간 등) infoDetail(단일 반복정보)을 List로 감싸고, tourItem(반려동물 동반 데이터 포함)을 itemDTO로 전달
        String extraInfo = tourApiHelper.extractExtraInfo(
                introDetail,
                infoDetail != null ? List.of(infoDetail) : List.of(),
                tourItem,
                placeType
        );

        // HashtagGenerator.java - cat1/cat2/cat3(대중소분류) 전달하도록 변경
        String hashtags = hashtagGenerator.generateHashtags(
                tourItem, introDetail, placeType,
                syncItem.getLclsSystm1(), syncItem.getLclsSystm2(), syncItem.getLclsSystm3(),
                minPrice, useFeeInfo
        );

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
                .extraInfo(extraInfo) // TODO: 부가정보컬럼 추가 시 -  0730 extraInfo 추가 세팅
                .build();
    }

    // TODO: footer에 TourDetailImageDTO - cpyrhtDivCd (저작권표기) 추가 필요 & 프론트에서 Type3의 경우 비율유지하며 적용 필요
    // TourDetailImageDTO -> PlaceImageDTO 변환
    // 대표 이미지 등록 시 sortOrder=0 지정, 상세/서브 이미지 등록 시 순번(sortOrder) 지정
//    public PlaceImageDTO convertToPlaceImageDTO(Integer placeId, String imageUrl, int sortOrder) {
//        if (!StringUtils.hasText(imageUrl) || placeId == null) {
//            return null;
//        }
//        return PlaceImageDTO.builder()
//                .placeId(placeId)
//                .imageUrl(imageUrl)
//                .sortOrder(sortOrder)
//                .build();
//    }
    // 플레이스 다중 이미지 변환 시 중복 URL 제거 및 sortOrder 인덱싱 처리 (최소 이미지 수 검증 포함)
    public List<PlaceImageDTO> convertToPlaceImageDTOs(Integer placeId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty() || placeId == null) {
            return List.of();
        }

        // 중복 이미지 제거 (Distinct) 및 유효한 이미지 URL만 필터링
        List<String> distinctUrls = imageUrls.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        // 필요 시 아래 조건으로 최소 수량 제한 가능 (현재는 전체 유효 이미지 대상 순번 부여)
        List<PlaceImageDTO> imageDTOs = new ArrayList<>();
        for (int i = 0; i < distinctUrls.size(); i++) {
            imageDTOs.add(
                    PlaceImageDTO.builder()
                            .placeId(placeId)
                            .imageUrl(distinctUrls.get(i))
                            .sortOrder(i) // 0번은 대표, 이후 1, 2, 3... 순차 인덱싱
                            .build()
            );
        }
        return imageDTOs;
    }

    // 카드형 썸네일 대표 이미지 결정: 목록 API의 썸네일(firstimage2) 우선, 없을 경우 원본(firstimage) Fallback 사용
    // PlaceImage 판단 로직을 여기서 계산해서 convertToPlaceDTO 호출 시 파라미터로 전달하는 구조
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

}