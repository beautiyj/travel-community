package com.gnagnoohc.travel.batch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.converter.TourDataConverter;
import com.gnagnoohc.travel.batch.dto.*;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO.Header;
import com.gnagnoohc.travel.batch.helper.TourApiHelper;
import com.gnagnoohc.travel.batch.validator.TourValidator;
import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/*  TODO: 0803 최종테스트 직전 확인하기: 공공데이터 로직 확인 완료, 수정필요한부분은 메인파이프라인 지역구코드 별로 랜덤필터링 작업
    (0731최종진행용-예정) 공공데이터 받아올 때, 데이터 오염으로 어려울 경우 각 지역군을 기준으로 1차 데이터 필터링 - 각 지역별로 일부 LIMIT걸어서 가져오기, 스케줄러로 관리 진행
*/

/* batch 패키지 역할
    1. 요청 : 리퀘스트 파라미터 (통신, config의 웹클라이언트와 client의 api클라이언트파일) 공공데이터 API 호출
    2. 응답 : 리스폰스 파라미터 (수신, dto폴더의 각각 요청에 일치하도록 객체 생성) 데이터 수집/가공
    3. 적재 : 해당 응답 구조를 활용하여 service DB(PLACE) 적재

    StringUtils.hasText() : 스프링부트 프레임워크의 자바 문자열 검증 헬퍼 메서드

    Validator에서 1차 필터링 & 검증된 데이터를 TourDataConverter에서 Helper메소드 혹은 코드로 세부 필터링 진행,
    이후 공공데이터 응답 DTO -> Tour 도메인단에서 사용하는 데이터베이스 테이블에 변환(Converter) 후, Mapper를 통해 DB에 적재
    (데이터테이블에 최종 적재하는 파이프라인 로직)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {
    private final TourApiClient tourApiClient;
    private final TourMapper tourMapper;
    private final ObjectMapper objectMapper;
    private final TourDataConverter tourDataConverter;
    private final TourApiHelper tourApiHelper;
    private final TourValidator tourValidator;

    private static final List<String> TARGET_CONTENT_TYPES = List.of(
            "12", // tour 관광지
            "14", // tour 문화시설
            "28", // tour 레포츠
            "32", // stay 숙박
            "39"  // food 음식점
    );

    // contentTypeId 5가지 선별하여 데이터 가져오기 : 타깃 타입별로 순회하며 수집 배치 실행, 타입별로 pageNo=1부터 페이징 돌리는 메인 파이프라인 호출하기
    public void fetchAllTargetSyncList() {
        for (String contentTypeId : TARGET_CONTENT_TYPES) {
            syncTourData(contentTypeId, 1);
        }
    }

    // 법정동 코드 수집 및 REGION 적재 파이프라인 (코드는 전체 목록 조회 Y 옵션으로 진행)
    @Transactional
    public void syncRegionData() {
        try {
            String jsonResponse = tourApiClient.fetchLdongCode(null, "Y");
            if (!StringUtils.hasText(jsonResponse)) return;
            TourApiResponseDTO<TourLdongCodeDTO> response = objectMapper.readValue(
                    jsonResponse, new TypeReference<TourApiResponseDTO<TourLdongCodeDTO>>() { }
            );

            if (response != null && response.getResponse() != null
                    && response.getResponse().getBody() != null
                    && response.getResponse().getBody().getItems() != null) {

                List<TourLdongCodeDTO> items = response.getResponse().getBody().getItems().getItem();

                // 269건 안의 시/도 코드(lDongRegnCd)를 중복 제거해서 먼저 부모 로우(parent_region_id=null)로 upsert
                // 이렇게 해야 2단계에서 시/군/구가 부모를 FK로 참조할 때 이미 부모가 테이블에 존재하는 상태가 됨
                java.util.Map<String, String> parentRegionMap = new java.util.LinkedHashMap<>();
                for (TourLdongCodeDTO dto : items) {
                    String regnCd = dto.getLDongRegnCd();
                    String regnNm = dto.getLDongRegnNm();
                    if (StringUtils.hasText(regnCd) && !parentRegionMap.containsKey(regnCd)) {
                        parentRegionMap.put(regnCd, regnNm);
                    }
                }

                for (java.util.Map.Entry<String, String> entry : parentRegionMap.entrySet()) {
                    // 시/도 단독 데이터를 흉내내는 TourLdongCodeDTO 생성 (시군구 코드/명은 비워서 최상위 레벨로 처리되게 함)
                    TourLdongCodeDTO parentDto = new TourLdongCodeDTO();
                    parentDto.setLDongRegnCd(entry.getKey());
                    parentDto.setLDongRegnNm(entry.getValue());

                    RegionDTO parentRegionDto = tourDataConverter.convertToRegionDTO(parentDto);
                    if (parentRegionDto != null) { tourMapper.upsertRegion(parentRegionDto); }
                }

                // 269건(시/군/구)을 순회하며 upsert. 이제 부모(시/도)가 이미 존재하므로 FK 제약 통과
                for (TourLdongCodeDTO dto : items) {
                    RegionDTO regionDto = tourDataConverter.convertToRegionDTO(dto);
                    if (regionDto != null) { tourMapper.upsertRegion(regionDto); }
                }
            }
        } catch (Exception e) {
            log.error("[Batch] 법정동 코드 수집 중 오류 발생", e);
        }
    }

    // 메인 파이프라인 - 공공데이터 장소 수집 및 PLACE, PLACE_IMAGE 적재(시작할 pageNo를 외부에서 주입받을 수 있는 구조)
    // 메인 최상위 파이프라인에선 크게 검증&스킵 처리 - 연쇄 수집&변환 처리(만들어진 컨버터) - 해당 컨버터에서 가공로직 담당하는 헬퍼로 최종 적재
    @Transactional
    public void syncTourData(String contentTypeId, int startPageNo) {
        // 고정값 1 대신 전달받은 시작 페이지 사용
        int pageNo = startPageNo;
        boolean hasNext = true;

        // [주석 처리용: 지역별 수집 건수 제한을 위한 맵 초기화 (각 지역당 최대 10개 제한 테스트용)]
        /*
        java.util.Map<String, Integer> regionCountMap = new java.util.HashMap<>();
        final int MAX_COUNT_PER_REGION = 10;
        */

        while (hasNext) {
            try {
                // TourAreaBasedSyncListDTO 동기화 목록 API 호출 (실제 데이터가 들어있는 batch용 동기화 api)
                String jsonResponse = tourApiClient.fetchAreaBasedSyncList(pageNo, contentTypeId, null, null);
                if (!StringUtils.hasText(jsonResponse)) {
                    log.info("[Batch] 더 이상 수집할 데이터가 없습니다. 루프를 종료합니다.");
                    break;
                }
                // JSON 파싱
                TourApiResponseDTO<TourAreaBasedSyncListDTO> response = objectMapper.readValue(
                        jsonResponse, new TypeReference<TourApiResponseDTO<TourAreaBasedSyncListDTO>>() { }
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

                // 수집 DTO -> 서비스 PlaceDTO 변환 및 DB 적재
                for (TourAreaBasedSyncListDTO syncItem : syncList) {

                    // --- [주석 처리용: 지역별 데이터 10개 제한 필터링 로직] ---
                    /*
                    String regionKey = syncItem.getLDongRegnCd() + (syncItem.getLDongSignguCd() != null ? syncItem.getLDongSignguCd() : "");
                    int currentCount = regionCountMap.getOrDefault(regionKey, 0);
                    
                    if (currentCount >= MAX_COUNT_PER_REGION) {
                        continue; // 해당 지역이 이미 10개를 채웠다면 스킵
                    }
                    */
                    // 1차 검증 & 필터링용 Validator
                    if (!tourValidator.isValid(syncItem)) { continue; }
                    // 대표 이미지가 없으면 연쇄 호출 및 저장 과정 전체 스킵
                    if (!tourApiHelper.isValidItem(syncItem)) { continue; }
                    // 단일 아이템 수집 및 DB 적재는 헬퍼 메서드로 위임하기
                    processSinglePlace(syncItem);

                    // --- [주석 처리용: 카운터 증가 처리] ---
                    /*
                    regionCountMap.put(regionKey, currentCount + 1);
                    */
                    // -----------------------------------------------------------------
                }

                log.info("[Batch Pagination Debug] 현재 요청 pageNo: {}, 가져온 item 개수: {}", pageNo, syncList.size());

                // TODO: 테스트 시 데이터 한도 추가 필요(데이터 리밋걸기 필요함)
                if (syncList.isEmpty()) {
                    hasNext = false;
                } else if (hasNext) {
                    pageNo++;
                }

            } catch (Exception e) {
                log.error("[Batch] {} 페이지 수집 중 에러 발생 - 일시 중단", pageNo, e);
                hasNext = false;
            }
        }
    }

    // 헬퍼 메소드 - 개별 장소의 상세정보 연쇄 수집 및 DB 적재(PLACE + PLACE_IMAGE)
    // 소개정보(/detailIntro2) & 반복정보(/detailInfo2) 조회용 헬퍼 각각 호출
    private void processSinglePlace(TourAreaBasedSyncListDTO syncItem) {
        TourItemDTO tourItem = tourDataConverter.convertToTourItemDTO(syncItem);
        TourDetailIntroDTO introDetail = null;
        TourDetailInfoDTO infoDetail = null;

        // 영업중인 장소 상세정보 연쇄 수집
        if (!"0".equals(syncItem.getShowflag())) {
            tourApiHelper.enrichTourItemDetails(tourItem);
            // 소개정보(detailIntro2) 연쇄 호출 (관광지/레포츠 이용요금 등)
            introDetail = tourApiHelper.fetchDetailIntro(syncItem.getContentid(), syncItem.getContenttypeid());
            // 숙박(32) 타입인 경우 반복정보(detailInfo2) 연쇄 호출 (숙박 객실 요금 등)
            if ("32".equals(syncItem.getContenttypeid())) {
                infoDetail = tourApiHelper.fetchDetailInfo(syncItem.getContentid(), syncItem.getContenttypeid());
            }
        }

        //  상세 이미지 API를 먼저 조회하여 최소 이미지 개수(2장 이상) 검증 수행
        List<TourDetailImageDTO> detailImages = tourApiHelper.fetchDetailImages(syncItem.getContentid());
        List<String> imageUrls = new ArrayList<>();
        if (detailImages != null && !detailImages.isEmpty()) {
            imageUrls = detailImages.stream()
                    .map(TourDetailImageDTO::getOriginimgurl)
                    .filter(StringUtils::hasText)
                    .distinct() // 중복 URL 제거
                    .toList();
        }
        if (imageUrls.size() < 5)  { return; }
        // PlaceImage 판단 로직에서 썸네일(카드용) 이미지 먼저 계산 -> convertToPlaceDTO로 전달
        String thumbnailImage = tourDataConverter.resolveThumbnailImage(syncItem);

        // convertToPlaceDTO -> 서비스 PlaceDTO 변환 및 해시태그 생성
        PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(syncItem, tourItem, introDetail, infoDetail, thumbnailImage);
        // 2차 부실 가격 검증: useFeeInfo도 넘겨주어, minPrice가 null이어도 요금안내 문구가 있으면 통과시킴
        if (!tourValidator.isValidPrice(placeDto.getMinPrice(), placeDto.getUseFeeInfo(), placeDto.getPlaceType())) { return; }

        // 최종 검증을 통과한 알짜 데이터만 DB 적재
        tourMapper.upsertPlace(placeDto);

        // PLACE_IMAGE 테이블에 sortOrder 순번에 맞춰 다중 적재 (컨버터 복수형 호출 방식으로 연결)
        if (placeDto.getPlaceId() != null) {
            List<PlaceImageDTO> imageDtos = tourDataConverter.convertToPlaceImageDTOs(placeDto.getPlaceId(), imageUrls);

            for (PlaceImageDTO imageDto : imageDtos) {
                if (imageDto != null) {
                    tourMapper.insertPlaceImage(imageDto);
                }
            }
        }
    }

    // 장소 이미지 목록 조회 (대표 이미지와 중복되는 항목 자동 제거)
    public List<PlaceImageDTO> getPlaceImages(Long placeId) {
        // 해당 장소 정보(대표 이미지 확인용)와 이미지 목록을 각각 가져옴
        PlaceDTO place = tourMapper.selectPlaceById(placeId);
        List<PlaceImageDTO> images = tourMapper.getImagesByPlaceId(placeId);
        
        if (images == null || images.isEmpty()) {
            return images;
        }
        
        // 대표 이미지(firstImage)가 존재한다면, 추가 이미지 목록에서 URL이 같은 것과 서로 중복되는 항목 제거
        return images.stream()
                .filter(img -> place == null || place.getFirstImage() == null || !place.getFirstImage().equals(img.getImageUrl()))
                .distinct() // 방어 로직 추가: 이미지 목록 자체에 똑같은 URL이 연속으로 들어있는 경우도 방지
                .toList();
    }
}