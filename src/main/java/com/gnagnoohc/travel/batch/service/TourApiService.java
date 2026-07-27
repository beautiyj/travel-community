package com.gnagnoohc.travel.batch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.converter.TourDataConverter;
import com.gnagnoohc.travel.batch.dto.*;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO.Header;
import com.gnagnoohc.travel.batch.helper.TourApiHelper;
import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/* batch 패키지 역할
    1. 요청 : 리퀘스트 파라미터 (통신, config의 웹클라이언트와 client의 api클라이언트파일) 공공데이터 API 호출
    2. 응답 : 리스폰스 파라미터 (수신, dto폴더의 각각 요청에 일치하도록 객체 생성) 데이터 수집/가공
    3. 적재 : 해당 응답 구조를 활용하여 service DB(PLACE) 적재

    StringUtils.hasText() : 스프링부트 프레임워크의 자바 문자열 검증 헬퍼 메서드
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

    // contentTypeId 5가지만 선별하여 데이터 가져오기
    private static final List<String> TARGET_CONTENT_TYPES = List.of(
            "12", // tour 관광지
            "14", // tour 문화시설
            "28", // tour 레포츠
            "32", // stay 숙박
            "39"  // food 음식점
    );

    // contentTypeId 5가지 선별하여 데이터 가져오기 : 타깃 타입별로 순회하며 수집 배치 실행
    public void fetchAllTargetSyncList() {
        log.info("[Batch Total] 공공데이터 5대 타깃 수집 프로세스 시작");
        for (String contentTypeId : TARGET_CONTENT_TYPES) {
            log.info("[Batch Target] contentTypeId: {} 수집 시작", contentTypeId);
            // 타입별로 pageNo=1부터 페이징 돌리는 메인 파이프라인 호출
            syncTourData(contentTypeId);
        }
        log.info("[Batch Total] 전체 타깃 수집 프로세스 완료");
    }

    // 법정동 코드 수집 및 REGION 적재 파이프라인
    @Transactional
    public void syncRegionData() {
        log.info("[Batch] 법정동 코드 수집 시작");
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
                    RegionDTO regionDto = tourDataConverter.convertToRegionDTO(dto);
                    if (regionDto != null) {
                        tourMapper.upsertRegion(regionDto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Batch] 법정동 코드 수집 중 오류 발생", e);
        }
        log.info("[Batch] 법정동 코드 수집 완료");
    }

    // 메인 파이프라인 - 공공데이터 장소 수집 및 PLACE, PLACE_IMAGE 적재
    // 메인 최상위 파이프라인에선 크게 검증&스킵 처리 - 연쇄 수집&변환 처리(만들어진 컨버터) - 해당 컨버터에서 가공로직 담당하는 헬퍼로 최종 적재
    @Transactional
    public void syncTourData(String contentTypeId) {
        log.info("[Batch] 공공데이터 외부 수집 및 PLACE 적재 시작");
        int pageNo = 1;
        boolean hasNext = true;

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

                // 수집 DTO -> 서비스 PlaceDTO 변환 및 DB 적재
                for (TourAreaBasedSyncListDTO syncItem : syncList) {
                    // 1) 대표 이미지가 없으면 연쇄 호출 및 저장 과정 전체 스킵(우리의 서비스에 적재x)
                    if (!tourApiHelper.isValidItem(syncItem)) { continue; }
                    // 2) 단일 아이템 수집 및 DB 적재는 헬퍼 메서드로 위임하기
                    processSinglePlace(syncItem);
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
        log.info("[Batch] contentTypeId: {} 수집 완료", contentTypeId);
    }

    /* 헬퍼 메소드 - 개별 장소의 상세정보 연쇄 수집 및 DB 적재(PLACE + PLACE_IMAGE)
       소개정보(/detailIntro2) & 반복정보(/detailInfo2) 조회용 헬퍼 각각 호출함 */
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
        // convertToPlaceDTO -> 서비스 PlaceDTO 변환 및 해시태그 생성
        PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(syncItem, tourItem, introDetail, infoDetail);
        tourMapper.upsertPlace(placeDto);

        // 대표 이미지가 있는 경우 PLACE_IMAGE 테이블 INSERT 적재
        if (StringUtils.hasText(syncItem.getFirstimage()) && placeDto.getPlaceId() != null) {
            // 대표 이미지 등록 - sortOrder=0 지정, TourDataConverter.convertToPlaceImageDTO 컨버터 경유
            PlaceImageDTO imageDto = tourDataConverter.convertToPlaceImageDTO(
                    placeDto.getPlaceId(), syncItem.getFirstimage(), 0
            );
            if (imageDto != null) {
                tourMapper.insertPlaceImage(imageDto);
            }
        }
    }

}