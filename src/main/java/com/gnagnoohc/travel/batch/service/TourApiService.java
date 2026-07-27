package com.gnagnoohc.travel.batch.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.dto.*;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO.Header;
import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/* batch 패키지 역할
    1. 요청 : 리퀘스트 파라미터 (통신, config의 웹클라이언트와 client의 api클라이언트파일) 공공데이터 API 호출
    2. 응답 : 리스폰스 파라미너 (수신, dto폴더의 각각 요청에 일치하도록 객체 생성) 데이터 수집/가공
    3. 적재 : 해당 응답 구조를 활용하여 service DB(PLACE) 적재
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {

    private final TourApiClient tourApiClient;
    private final TourMapper tourMapper;
    private final ObjectMapper objectMapper;

    /**
     * 외부 공공데이터를 수집하여 우리 DB(PLACE 테이블)에 적재하는 메인 파이프라인
     */

    /*  TourItemDTO 공통필드로 처리되는 공공데이터
        - 위치기반 관광정보조회 /locationBasedList2
        - 지역기반 관광정보조회 /areaBasedList2
        - 키워드 검색 조회 /searchKeyword2
        - 숙박정보조회 /searchStay2
        - 공통정보조회 /detailCommon2
        - 반려동물 동반 여행 정보 /detailPetTour2
        TourItemDTO 공통필드 + 추가필드 TourDetailIntroDTO
        - 소개정보조회 /detailIntro2
        TourItemDTO 공통필드 + 추가필드 TourDetailInfoDTO
        - 반복정보조회 /detailInfo2
        TourItemDTO 공통필드 + 추가필드 TourDetailImageDTO
        - 이미지정보조회 /detailImage2
        TourItemDTO 공통필드선언되어있는걸안쓰고 필드에 공통필드까지선언된 TourAreaBasedSyncListDTO
        - 관광정보 동기화 목록 조회 /areaBasedSyncList2 - 배치 수집 전용 API (DB 최신상태 유지용 API)
        필드 TourLdongCodeDTO
        - 법정동코드조회 /ldongCode2        
        필드 TourLclsSystmCodeDTO.java
        - 분류체계 코드조회 /lclsSystmCode2
    */
// 공공데이터 전용 가상 비즈니스 회원 PK (시스템 유령계정)
    private static final Long PUBLIC_DATA_MEMBER_ID = 1L;

    /**
     * [1] 법정동 코드 수집 및 REGION 적재 파이프라인
     */
    @Transactional
    public void syncRegionData() {
        log.info("=== [Batch] 법정동 코드 수집 시작 ===");
        try {
            String jsonResponse = tourApiClient.fetchLdongCode("1", "1000");
            if (!StringUtils.hasText(jsonResponse)) return;

            TourApiResponseDTO<TourLdongCodeDTO> response = objectMapper.readValue(
                jsonResponse, new TypeReference<TourApiResponseDTO<TourLdongCodeDTO>>() {}
            );

            if (response != null && response.getResponse() != null
                    && response.getResponse().getBody() != null
                    && response.getResponse().getBody().getItems() != null) {

                List<TourLdongCodeDTO> items = response.getResponse().getBody().getItems().getItem();
                for (TourLdongCodeDTO dto : items) {
                    RegionDTO regionDto = convertToRegionDTO(dto);
                    if (regionDto != null) {
                        tourMapper.upsertRegion(regionDto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Batch] 법정동 코드 수집 중 오류 발생", e);
        }
        log.info("=== [Batch] 법정동 코드 수집 완료 ===");
    }

    /**
     * [2] 메인 파이프라인 - 공공데이터 장소 수집 및 PLACE, PLACE_IMAGE 적재
     */
    @Transactional
    public void syncTourData() {
        log.info("=== [Batch] 공공데이터 외부 수집 및 PLACE 적재 시작 ===");
        int pageNo = 1;
        boolean hasNext = true;

        while (hasNext) {
            try {
                // 1. 동기화 목록 API 호출
                String jsonResponse = tourApiClient.fetchAreaBasedSyncList(pageNo, null, null, null);
                if (!StringUtils.hasText(jsonResponse)) {
                    log.info("[Batch] 더 이상 수집할 데이터가 없습니다. 루프를 종료합니다.");
                    break;
                }

                // 2. JSON 파싱
                TourApiResponseDTO<TourAreaBasedSyncListDTO> response = objectMapper.readValue(
                    jsonResponse, new TypeReference<TourApiResponseDTO<TourAreaBasedSyncListDTO>>() {}
                );

                if (response != null && response.getResponse() != null && response.getResponse().getHeader() != null) {
                    Header header = response.getResponse().getHeader();
                    String resultCode = header.getResultCode();
                    if (!"0000".equals(resultCode) && !"00".equals(resultCode)) {
                        log.error("[Batch API 오류] 코드: {}, 메시지: {}", resultCode, header.getResultMsg());
                        break;
                    }
                }

                if (response == null || response.getResponse() == null
                        || response.getResponse().getBody() == null
                        || response.getResponse().getBody().getItems() == null
                        || response.getResponse().getBody().getItems().getItem() == null) {
                    break;
                }

                List<TourAreaBasedSyncListDTO> syncList = response.getResponse().getBody().getItems().getItem();
                if (syncList.isEmpty()) break;

                // 3. 수집 DTO -> 서비스 PlaceDTO 변환 및 DB 적재
                for (TourAreaBasedSyncListDTO syncItem : syncList) {
                    TourItemDTO tourItem = convertToTourItemDTO(syncItem);
                    TourDetailIntroDTO introDetail = null;

                    // 영업중인 장소 상세정보 연쇄 수집
                    if (!"0".equals(syncItem.getShowflag())) {
                        enrichTourItemDetails(tourItem);
                        // 💡 소개정보(detailIntro2) 연쇄 호출로 요금/메뉴/숙소타입 파싱용 DTO 획득
                        introDetail = fetchDetailIntro(syncItem.getContentid(), syncItem.getContenttypeid());
                    }

                    // [수집 DTO -> 서비스 PlaceDTO 변환 및 해시태그 생성]
                    PlaceDTO placeDto = convertToPlaceDTO(syncItem, tourItem, introDetail);

                    // [우리 DB PLACE 테이블 UPSERT 적재]
                    tourMapper.upsertPlace(placeDto);

                    // [대표 이미지가 있는 경우 PLACE_IMAGE 테이블 INSERT 적재]
                    if (StringUtils.hasText(tourItem.getFirstimage()) && placeDto.getPlaceId() != null) {
                        PlaceImageDTO imageDto = PlaceImageDTO.builder()
                                .placeId(placeDto.getPlaceId())
                                .imageUrl(tourItem.getFirstimage())
                                .sortOrder(0)
                                .build();
                        tourMapper.insertPlaceImage(imageDto);
                    }
                }

                if (syncList.size() < 500) {
                    hasNext = false;
                } else {
                    pageNo++;
                }

            } catch (Exception e) {
                log.error("[Batch] {} 페이지 수집 중 에러 발생 - 일시 중단", pageNo, e);
                hasNext = false;
            }
        }
        log.info("=== [Batch] 공공데이터 수집 및 PLACE 적재 완료 ===");
    }

    private RegionDTO convertToRegionDTO(TourLdongCodeDTO ldongCodeDTO) {
        String rawCode;
        String rawName;

        if (ldongCodeDTO.getLDongRegnCd() != null && !ldongCodeDTO.getLDongRegnCd().isBlank()) {
            rawCode = ldongCodeDTO.getLDongRegnCd() + 
                    (ldongCodeDTO.getLDongSignguCd() != null ? ldongCodeDTO.getLDongSignguCd() : "");
            rawName = ldongCodeDTO.getLDongRegnNm();
            if (ldongCodeDTO.getLDongSignguNm() != null && !ldongCodeDTO.getLDongSignguNm().isBlank()) {
                rawName += " " + ldongCodeDTO.getLDongSignguNm();
            }
        } else {
            rawCode = ldongCodeDTO.getCode();
            rawName = ldongCodeDTO.getName();
        }

        if (rawCode == null || rawCode.isBlank()) return null;

        Long regionId = Long.parseLong(rawCode);
        Long parentRegionId = null;

        if (rawCode.length() >= 5) {
            parentRegionId = Long.parseLong(rawCode.substring(0, 2));
        }

        return RegionDTO.builder()
                .regionId(regionId)
                .regionName(rawName)
                .parentRegionId(parentRegionId)
                .build();
    }

    private PlaceDTO convertToPlaceDTO(TourAreaBasedSyncListDTO syncItem, TourItemDTO tourItem, TourDetailIntroDTO introDetail) {
        Long regionId = null;
        if (StringUtils.hasText(syncItem.getLDongRegnCd())) {
            String rawCode = syncItem.getLDongRegnCd() + 
                    (StringUtils.hasText(syncItem.getLDongSignguCd()) ? syncItem.getLDongSignguCd() : "");
            if (StringUtils.hasText(rawCode)) {
                regionId = Long.parseLong(rawCode);
            }
        }

        String placeType = convertContentType(syncItem.getContenttypeid());

        String fullAddress = syncItem.getAddr1() + 
                (StringUtils.hasText(syncItem.getAddr2()) ? " " + syncItem.getAddr2() : "");
        BigDecimal mapx = StringUtils.hasText(syncItem.getMapx()) ? new BigDecimal(syncItem.getMapx()) : null;
        BigDecimal mapy = StringUtils.hasText(syncItem.getMapy()) ? new BigDecimal(syncItem.getMapy()) : null;

        // 💡 TourItemDTO + TourDetailIntroDTO 조합으로 해시태그 생성
        String hashtags = generateHashtags(tourItem, introDetail, placeType);

        return PlaceDTO.builder()
                .memberId(PUBLIC_DATA_MEMBER_ID)
                .regionId(regionId)
                .placeType(placeType)
                .name(syncItem.getTitle())
                .description(tourItem.getOverview())
                .address(fullAddress)
                .mapx(mapx)
                .mapy(mapy)
                .isClosed("0".equals(syncItem.getShowflag()))
                .firstImage(syncItem.getFirstimage())
                .hashtags(hashtags)
                .build();
    }

    private TourItemDTO convertToTourItemDTO(TourAreaBasedSyncListDTO syncItem) {
        TourItemDTO item = new TourItemDTO();
        item.setContentid(syncItem.getContentid());
        item.setContenttypeid(syncItem.getContenttypeid());
        item.setCreatedtime(syncItem.getCreatedtime());
        item.setModifiedtime(syncItem.getModifiedtime());
        item.setTitle(syncItem.getTitle());
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

    private void enrichTourItemDetails(TourItemDTO masterItem) {
        String contentId = masterItem.getContentid();
        try {
            // detailCommon2 호출
            String commonJson = tourApiClient.fetchDetailCommon(contentId);
            if (StringUtils.hasText(commonJson)) {
                TourApiResponseDTO<TourItemDTO> commonResponse = objectMapper.readValue(
                    commonJson, new TypeReference<TourApiResponseDTO<TourItemDTO>>() {}
                );

                if (commonResponse != null && commonResponse.getResponse() != null
                        && commonResponse.getResponse().getBody() != null
                        && commonResponse.getResponse().getBody().getItems() != null
                        && !commonResponse.getResponse().getBody().getItems().getItem().isEmpty()) {

                    TourItemDTO commonDetail = commonResponse.getResponse().getBody().getItems().getItem().get(0);
                    masterItem.setOverview(commonDetail.getOverview());
                    if (!StringUtils.hasText(masterItem.getTel())) {
                        masterItem.setTel(commonDetail.getTel());
                    }
                }
            }

            // detailPetTour2 호출
            String petJson = tourApiClient.fetchDetailPetTour(contentId);
            if (StringUtils.hasText(petJson)) {
                TourApiResponseDTO<TourItemDTO> petResponse = objectMapper.readValue(
                    petJson, new TypeReference<TourApiResponseDTO<TourItemDTO>>() {}
                );

                if (petResponse != null && petResponse.getResponse() != null
                        && petResponse.getResponse().getBody() != null
                        && petResponse.getResponse().getBody().getItems() != null
                        && !petResponse.getResponse().getBody().getItems().getItem().isEmpty()) {

                    TourItemDTO petDetail = petResponse.getResponse().getBody().getItems().getItem().get(0);
                    masterItem.setAcmpyPsblCpam(petDetail.getAcmpyPsblCpam());
                    masterItem.setPetTursmInfo(petDetail.getPetTursmInfo());
                }
            }
        } catch (Exception e) {
            log.warn("[Batch] 상세 정보 연쇄 호출 실패 - contentId: {}, 사유: {}", contentId, e.getMessage());
        }
    }

    // 💡 소개정보(/detailIntro2) 조회를 위한 별도 헬퍼 메서드
    private TourDetailIntroDTO fetchDetailIntro(String contentId, String contentTypeId) {
        try {
            String introJson = tourApiClient.fetchDetailIntro(contentId, contentTypeId);
            if (StringUtils.hasText(introJson)) {
                TourApiResponseDTO<TourDetailIntroDTO> introResponse = objectMapper.readValue(
                    introJson, new TypeReference<TourApiResponseDTO<TourDetailIntroDTO>>() {}
                );
                if (introResponse != null && introResponse.getResponse() != null
                        && introResponse.getResponse().getBody() != null
                        && introResponse.getResponse().getBody().getItems() != null
                        && !introResponse.getResponse().getBody().getItems().getItem().isEmpty()) {
                    return introResponse.getResponse().getBody().getItems().getItem().get(0);
                }
            }
        } catch (Exception e) {
            log.warn("[Batch] detailIntro2 호출 실패 - contentId: {}", contentId);
        }
        return null;
    }

    // 💡 TourItemDTO + TourDetailIntroDTO 기반 동적 해시태그 생성
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

    private String convertContentType(String contentTypeId) {
        if (contentTypeId == null) return "tour";
        return switch (contentTypeId) {
            case "32" -> "stay";
            case "39" -> "food";
            default   -> "tour";
        };
    }
}