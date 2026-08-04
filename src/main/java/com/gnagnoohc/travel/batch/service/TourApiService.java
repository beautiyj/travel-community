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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    // 검증된 알짜 데이터를 모아 지역별 무작위 샘플링(최대 10개, 타입별 최소 1개 보장) 후 DB 적재
    @Transactional
    public void syncTourData(String contentTypeId, int startPageNo) {
        int pageNo = startPageNo;
        boolean hasNext = true;

        // API에서 받아온 검증 완료 항목을 일차적으로 담을 리스트
        List<TourAreaBasedSyncListDTO> rawValidList = new ArrayList<>();

        while (hasNext) {
            try {
                // TourAreaBasedSyncListDTO 동기화 목록 API 호출
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

                // DTO 수집 및 1차 검증
                for (TourAreaBasedSyncListDTO syncItem : syncList) {
                    // 1차 검증 & 필터링용 Validator
                    if (!tourValidator.isValid(syncItem)) { continue; }
                    // 대표 이미지가 없으면 연쇄 호출 및 저장 스킵
                    if (!tourApiHelper.isValidItem(syncItem)) { continue; }
                    // 휴업/폐업(showflag == 0) 데이터는 즉시 폐업(is_closed = 1) 처리하여 DB 저장
                    if ("0".equals(syncItem.getShowflag())) {
                        processClosedPlace(syncItem);
                        continue;
                    }
                    // 정상 검증 통과한 항목을 후보 리스트에 수집
                    rawValidList.add(syncItem);
                }

                log.info("[Batch Pagination Debug] 현재 요청 pageNo: {}, 검증 통과 item 누적 개수: {}", pageNo, rawValidList.size());

                pageNo++;
                // 방어용 페이징 안전장치 (필요 시 조절 가능)
                if (pageNo > 30) {
                    hasNext = false;
                }

            } catch (Exception e) {
                log.error("[Batch] {} 페이지 수집 중 에러 발생 - 일시 중단", pageNo, e);
                hasNext = false;
            }
        }
        // 필터링 후 모인 전체 데이터를 지역별로 그룹핑 ➔ 랜덤 셔플 ➔ 타입별 최소 1개 선점 ➔ 최대 10개 추출 후 DB 저장
        processRandomSamplingAndSave(rawValidList);
    }

    // TODO: 0804 샘플링 로직 완료 - 무작위 샘플링 및 DB 적재 핵심 처리 메서드
    private void processRandomSamplingAndSave(List<TourAreaBasedSyncListDTO> rawValidList) {
        if (rawValidList == null || rawValidList.isEmpty()) return;

        // 법정동 시도/시군구 코드로 지역군 그룹핑 (269개 지역 기준)
        Map<String, List<TourAreaBasedSyncListDTO>> regionGroupMap = rawValidList.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getLDongRegnCd() + (StringUtils.hasText(item.getLDongSignguCd()) ? item.getLDongSignguCd() : "")
                ));

        for (Map.Entry<String, List<TourAreaBasedSyncListDTO>> entry : regionGroupMap.entrySet()) {
            List<TourAreaBasedSyncListDTO> regionItems = entry.getValue();
            // 가나다/순차 정렬 방지를 위해 해당 지역의 수집 목록을 무작위 셔플
            Collections.shuffle(regionItems);
            // 타입(contentTypeId)별 그룹핑
            Map<String, List<TourAreaBasedSyncListDTO>> typedMap = regionItems.stream()
                    .collect(Collectors.groupingBy(TourAreaBasedSyncListDTO::getContenttypeid));
            List<TourAreaBasedSyncListDTO> selectedList = new ArrayList<>();

            // 관광지, 숙박, 맛집 타입별로 무작위 1개씩 우선 추출 (타입 비중 균형 보장)
            for (List<TourAreaBasedSyncListDTO> typeList : typedMap.values()) {
                if (!typeList.isEmpty()) {
                    selectedList.add(typeList.remove(0)); // 셔플된 목록 중 첫 번째 무작위 추출
                }
            }

            // 남은 후보군을 다시 하나로 모아 셔플 후, 지역당 최대 10개가 채워질 때까지 보충
            List<TourAreaBasedSyncListDTO> remainList = typedMap.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            Collections.shuffle(remainList);

            int needed = 10 - selectedList.size();
            for (int i = 0; i < Math.min(needed, remainList.size()); i++) {
                selectedList.add(remainList.get(i));
            }

            // 무작위 선발 완료된 지역당 최대 10개 데이터를 연쇄 수집 및 DB(PLACE, PLACE_IMAGE) 적재
            for (TourAreaBasedSyncListDTO targetItem : selectedList) {
                processSinglePlace(targetItem);
            }

            log.info("[Batch Sampling] 지역코드 {}: 총 {}개 장소 무작위 적재 완료", entry.getKey(), selectedList.size());
        }
    }

    // 폐업(showflag == 0) 데이터 전용 처리 헬퍼
    private void processClosedPlace(TourAreaBasedSyncListDTO syncItem) {
        TourItemDTO tourItem = tourDataConverter.convertToTourItemDTO(syncItem);
        PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(syncItem, tourItem, null, null, null);
        placeDto.setIsClosed(1); // 전부 폐업(Soft Off) 처리
        tourMapper.upsertPlace(placeDto);
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
    public List<PlaceImageDTO> getPlaceImages(Integer placeId) {
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