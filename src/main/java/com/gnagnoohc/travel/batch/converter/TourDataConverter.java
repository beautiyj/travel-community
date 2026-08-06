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

/* TourDataConverter.java - 공공데이터 응답 데이터 -> 실제 데이터베이스의 테이블로 최종 변환하는 로직
*
* */
@Component
@RequiredArgsConstructor
public class TourDataConverter {
    private final TourApiHelper tourApiHelper;
    private final HashtagGenerator hashtagGenerator;

    // 공공데이터 전용 가상 비즈니스 회원 PK (시스템 유령계정)
    private static final Integer PUBLIC_DATA_MEMBER_ID = 1;

    // TourLdongCodeDTO -> RegionDTO 변환
    public RegionDTO convertToRegionDTO(TourLdongCodeDTO ldongCodeDTO) {
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
        // 자식 데이터(시군구 코드가 존재하거나, 코드가 5자리 이상)라면 앞 2자리(시도 코드)를 부모 ID로 지정, 단독 시도 데이터라면 parentRegionId는 무조건 null
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
        // 플레이스 타입은 숫자로 들어오는 걸 convertContentType에서 tour/food/stay로 변환 처리
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
        // 음식점(food)이거나, 숫자로 파싱은 안 됐지만 요금안내 문구(useFeeInfo)가 존재하는 경우 minPrice를 0으로 세팅하여 DB에 적재되도록 보정
        if (minPrice == null && ("food".equals(placeType) || StringUtils.hasText(useFeeInfo))) { minPrice = 0; }
        // 공공데이터에서 넘어오는 String형 showflag -> 래퍼클래스 Integer형 변환, 0이면 문닫음(1), 아니면 영업중(0)
        int isClosedValue = "0".equals(syncItem.getShowflag()) ? 1 : 0;
        // 부가정보(주차, 휴무일, 영업시간 등) infoDetail(단일 반복정보)을 List로 감싸고, tourItem(반려동물 동반 데이터 포함)을 itemDTO로 전달
        String extraInfo = tourApiHelper.extractExtraInfo(
                introDetail,
                infoDetail != null ? List.of(infoDetail) : List.of(),
                tourItem,
                placeType
        );

        // 해시태그 로직 HashtagGenerator.java - cat1/cat2/cat3(대중소분류) 전달
        String hashtags = hashtagGenerator.generateHashtags(
                tourItem, introDetail, placeType,
                syncItem.getLclsSystm1(), syncItem.getLclsSystm2(), syncItem.getLclsSystm3()
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
                .isClosed(isClosedValue)
                .firstImage(thumbnailImage)
                .hashtags(hashtags)
                .peopleCount(1)
                .extraInfo(extraInfo)
                .build();
    }

    // 플레이스 다중 이미지 변환 시 중복 URL 제거 및 sortOrder 인덱싱 처리 (최소 이미지 수 검증 포함)
    public List<PlaceImageDTO> convertToPlaceImageDTOs(Integer placeId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty() || placeId == null) { return List.of(); }

        // 중복 이미지 제거 (Distinct) 및 유효한 이미지 URL만 필터링
        List<String> distinctUrls = imageUrls.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        // 전체 유효 이미지 대상 순번 부여
        List<PlaceImageDTO> imageDTOs = new ArrayList<>();
        for (int i = 0; i < distinctUrls.size(); i++) {
            imageDTOs.add(
                    PlaceImageDTO.builder()
                            .placeId(placeId)
                            .imageUrl(distinctUrls.get(i))
                            .sortOrder(i) // 0번은 대표, 이후 1, 2, 3... 순차 인덱싱 처리
                            .build()
            );
        }
        return imageDTOs;
    }

    public String resolveThumbnailImage(TourAreaBasedSyncListDTO syncItem) {
        if (StringUtils.hasText(syncItem.getFirstimage())) {
            return syncItem.getFirstimage();
        }
        return syncItem.getFirstimage2();
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
        //
        // TODO: 추후예정 - 이후 지도 API 활용 시 메모리에서 dist 꺼내 쓰는 방식 / dist는 특정 좌표(사용자 위치) 기준 상대적인 거리라서 기본 PlaceDTO엔 넣을 필요 없음
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