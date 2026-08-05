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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*  TODO: 0803 최종테스트 직전 확인하기: 공공데이터 로직 확인 완료, 수정필요한부분은 메인파이프라인 지역구코드 별로 랜덤필터링 작업
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

    // 0804 추가 - 부실데이터 예외 보완 레이어에서 사용할, tour/stay 각각의 원본 contentTypeId 목록
    private static final List<String> TOUR_CONTENT_TYPES = List.of("12", "14", "28");
    private static final List<String> STAY_CONTENT_TYPES = List.of("32");
    // 0804 추가 - 지역당 부실데이터로 추가 확보할 타입별 목표 건수 (큐 셀렉팅 결과와 무관하게 항상 추가됨, 10건 캡과 별개)
    private static final int LOW_QUALITY_SUPPLEMENT_COUNT = 3;

    // contentTypeId 5가지 선별하여 데이터 가져오기 : 타깃 타입별로 순회하며 수집 배치 실행, 타입별로 pageNo=1부터 페이징 돌리는 메인 파이프라인 호출하기
    // TODO: 샘플링테스트 확인 - 0804 수정: 타입별로 각각 syncTourData를 호출해 그때그때 샘플링/적재하던 기존 방식 변경
    // "타입별 최소 1개 + 지역당 총 10개" 보장이 실제로 성립하지 않아(각 호출엔 타입이 1개뿐이라 그룹핑 무의미),
    // 5개 타입을 모두 모아 후보 리스트를 합친 뒤 지역별 샘플링을 단 한 번만 수행하도록 변경
    public void fetchAllTargetSyncList() {
        List<TourAreaBasedSyncListDTO> allValidList = new ArrayList<>();
        for (String contentTypeId : TARGET_CONTENT_TYPES) {
            allValidList.addAll(collectValidItems(contentTypeId, 1));
        }
        processRandomSamplingAndSave(allValidList);
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
    // 단일 contentTypeId 단독 실행(테스트/부분 재수집용)에 한해서는 그 타입 내에서만 샘플링 진행
    // (5개 타입을 합쳐서 정식으로 돌릴 땐 fetchAllTargetSyncList를 사용할 것)
    @Transactional
    public void syncTourData(String contentTypeId, int startPageNo) {
        List<TourAreaBasedSyncListDTO> rawValidList = collectValidItems(contentTypeId, startPageNo);
        processRandomSamplingAndSave(rawValidList);
    }

    // TODO: 샘플링테스트 전 최종수정 - 0804 페이징 순회 + 1차 검증(Validator/isValidItem)까지만 수행하고, 즉시 적재하지 않고 후보 리스트로 반환
    // fetchAllTargetSyncList에서 5개 타입을 전부 모아 지역별 샘플링을 한 번에 하기 위해 syncTourData에서 분리함
    private List<TourAreaBasedSyncListDTO> collectValidItems(String contentTypeId, int startPageNo) {
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
        return rawValidList;
    }

    // TODO: 테스트용: (혹은빠른검증용)특정 지역(들)만 대상으로 지역기반 목록조회(/areaBasedList2)를 사용해 빠르게 검증용 소규모 테스트 수행
    // collectValidItems(전국 페이징 순회)는 특정 지역 몇 개만 테스트하기엔 비효율적이라 별도 경로로 분리함
    // TourApiClient.fetchAreaBasedList가 lDongRegnCd/lDongSignguCd로 직접 필터링하므로 지역 단위 호출이 가능함
    public void syncTourDataForRegions(List<Integer> regionIds) {
        List<TourAreaBasedSyncListDTO> rawValidList = new ArrayList<>();

        for (Integer regionId : regionIds) {
            String regionIdStr = String.valueOf(regionId);
            // regionId는 시도코드(2자리)+시군구코드 결합값이므로 분리 (parseRegionId의 역연산)
            if (regionIdStr.length() < 3) {
                log.warn("[Batch Region Test] 시도 단독 코드는 대상 아님 - regionId: {}", regionId);
                continue;
            }
            String regnCd = regionIdStr.substring(0, 2);
            String signguCd = regionIdStr.substring(2);

            for (String contentTypeId : TARGET_CONTENT_TYPES) {
                rawValidList.addAll(collectValidItemsForRegion(regnCd, signguCd, contentTypeId));
            }
        }
        // 지정 지역만 담긴 리스트를 그대로 기존 샘플링 로직에 전달 (내부에서 지역별로 다시 그룹핑되므로 재사용 가능)
        processRandomSamplingAndSave(rawValidList);
    }
    //TODO: 테스트용: (혹은빠른검증용) 지역기반 목록조회(/areaBasedList2)로 특정 지역+타입 조합의 1차 검증 통과 항목만 수집
    // 페이징 없이 1회 호출 (지역 단위라 데이터량이 적음), arrange="Q"로 대표이미지 보장된 항목만 응답받음
    private List<TourAreaBasedSyncListDTO> collectValidItemsForRegion(String regnCd, String signguCd, String contentTypeId) {
        List<TourAreaBasedSyncListDTO> rawValidList = new ArrayList<>();
        try {
            String jsonResponse = tourApiClient.fetchAreaBasedList(regnCd, signguCd, contentTypeId, "Q");
            if (!StringUtils.hasText(jsonResponse)) return rawValidList;

            TourApiResponseDTO<TourAreaBasedSyncListDTO> response = objectMapper.readValue(
                    jsonResponse, new TypeReference<TourApiResponseDTO<TourAreaBasedSyncListDTO>>() { }
            );
            if (response == null || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null) {
                return rawValidList;
            }

            List<TourAreaBasedSyncListDTO> syncList = response.getResponse().getBody().getItems().getItem();
            for (TourAreaBasedSyncListDTO syncItem : syncList) {
                if (!tourValidator.isValid(syncItem)) { continue; }
                if (!tourApiHelper.isValidItem(syncItem)) { continue; }
                if ("0".equals(syncItem.getShowflag())) {
                    processClosedPlace(syncItem);
                    continue;
                }
                rawValidList.add(syncItem);
            }
        } catch (Exception e) {
            log.warn("[Batch Region Test] regnCd:{} signguCd:{} contentTypeId:{} 조회 중 오류: {}", regnCd, signguCd, contentTypeId, e.getMessage());
        }
        return rawValidList;
    }

    // TODO: 0804 무작위 샘플링 및 DB 적재 핵심 처리 메서드 (타입별 보완 2단계 Fallback 확장 적용)
    // - 1차 큐 처리는 기존 흐름 유지
    // - 2단계 Fallback 시 큐를 사용하지 않고, 전체 후보군에서 stay / tour 타입을 각각 타겟팅하여
    //   중분류가 다양하게 섞이도록 랜덤하게 뽑아 부족한 수량을 강제 적재
    // TODO: 0804 무작위 샘플링 및 DB 적재 핵심 처리 메서드 (1차 큐 10건 + stay 5 / tour 5 강제 적재 + 최종 10개 채우기 패딩)
    private void processRandomSamplingAndSave(List<TourAreaBasedSyncListDTO> rawValidList) {
        if (rawValidList == null || rawValidList.isEmpty()) return;

        // 법정동 시도/시군구 코드로 지역군 그룹핑 (269개 지역 기준)
        Map<String, List<TourAreaBasedSyncListDTO>> regionGroupMap = rawValidList.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getLDongRegnCd() + (StringUtils.hasText(item.getLDongSignguCd()) ? item.getLDongSignguCd() : "")
                ));

        for (Map.Entry<String, List<TourAreaBasedSyncListDTO>> entry : regionGroupMap.entrySet()) {
            String regionKey = entry.getKey();
            List<TourAreaBasedSyncListDTO> regionItems = entry.getValue();

            // regionId(Integer) 파싱
            Integer regionId;
            try {
                regionId = Integer.parseInt(regionKey);
            } catch (NumberFormatException e) {
                log.warn("[Batch Sampling] 지역코드 파싱 실패 - regionKey: {}", regionKey);
                continue;
            }

            // 기존 적재 건수 확인
            int existingCount = tourMapper.selectPlaceCountByRegion(regionId);
            int targetGoal = 10; // 1차 기본 목표 10건

            // placeType(tour/stay/food)별 그룹핑
            Map<String, List<TourAreaBasedSyncListDTO>> typedMap = new HashMap<>();
            for (TourAreaBasedSyncListDTO item : regionItems) {
                String type = tourApiHelper.convertContentType(item.getContenttypeid(), item.getLclsSystm2(), item.getLclsSystm3());
                typedMap.computeIfAbsent(type, k -> new ArrayList<>()).add(item);
            }

            // 💡수집된 데이터 중 카테고리별 유효 개수를 미리 출력하여 확인
            log.info("[Type Check] 지역코드 {} 유효 후보 분포 - tour: {}건, stay: {}건, food: {}건 (전체: {}건)",
                    regionKey,
                    typedMap.getOrDefault("tour", Collections.emptyList()).size(),
                    typedMap.getOrDefault("stay", Collections.emptyList()).size(),
                    typedMap.getOrDefault("food", Collections.emptyList()).size(),
                    regionItems.size());

            // 가나다/순차 정렬 방지를 위해 타입별 목록 각각 무작위 셔플
            typedMap.values().forEach(Collections::shuffle);

            // 관광지(tour), 숙박(stay), 맛집(food) 타입별 우선순위 선발 + 전체 후보 통합 큐 생성
            List<TourAreaBasedSyncListDTO> candidateQueue = new ArrayList<>();

            // 타입별로 무작위 1개씩 우선 추출 (타입 비중 균형 보장)
            List<String> targetTypes = List.of("tour", "stay", "food");
            for (String type : targetTypes) {
                List<TourAreaBasedSyncListDTO> typeList = typedMap.get(type);
                if (typeList != null && !typeList.isEmpty()) {
                    candidateQueue.add(typeList.remove(0));
                }
            }

            // 남은 후보군을 다시 하나로 모아 셔플 후 큐 뒤에 이어 붙임
            List<TourAreaBasedSyncListDTO> remainList = typedMap.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            Collections.shuffle(remainList);
            candidateQueue.addAll(remainList);

            int currentTotalCount = existingCount;
            int newlySavedCount = 0;

            // =====================================================================================
            // [1단계] 기존 큐 기반 10건 채우기 처리
            // =====================================================================================
            for (TourAreaBasedSyncListDTO targetItem : candidateQueue) {
                if (newlySavedCount >= targetGoal) {
                    break;
                }

                try {
                    log.info("[PLACE SAVE TRY - Stage 1] contentId: {}, title: {}", targetItem.getContentid(), targetItem.getTitle());

                    if (processSinglePlace(targetItem)) {
                        currentTotalCount++;
                        newlySavedCount++;
                    }
                } catch (Exception e) {
                    log.error("[Batch Error - Stage 1] 지역코드 {} placeId: {} DB 적재 오류: {}",
                            regionKey, targetItem.getContentid(), e.getMessage());
                }
            }

            // 1차 큐 처리에 사용된 아이템 ID 집합 추출 (중복 방지용)
            Set<String> processedIds = candidateQueue.stream()
                    .limit(newlySavedCount)
                    .map(TourAreaBasedSyncListDTO::getContentid)
                    .collect(Collectors.toSet());

            // =====================================================================================
            // [2단계 강제 예외 처리] 큐 처리를 거치지 않고, stay 5개 및 tour 5개를 각각 검증 없이 강제 적재
            // =====================================================================================
            log.info("[Batch Fallback Triggered] 지역코드 {}: 1차 큐 수집 완료. 이후 stay 5개, tour 5개 강제 랜덤 적재를 시작합니다.", regionKey);

            // 1) STAY 타입 5개 강제 랜덤 적재
            processForcedRandomByType(regionItems, processedIds, "stay", 5);

            // 2) TOUR 타입 5개 강제 랜덤 적재
            processForcedRandomByType(regionItems, processedIds, "tour", 5);

            // =====================================================================================
            // [3단계 패딩 안전장치] 최종 적재 건수가 10개 미만인 경우, 남은 전체 풀에서 무작위로 빈자리 채우기
            // =====================================================================================
            int currentFinalCount = tourMapper.selectPlaceCountByRegion(regionId);
            if (currentFinalCount < targetGoal) {
                int paddingNeeded = targetGoal - currentFinalCount;
                log.info("[Batch Padding Triggered] 지역코드 {}: 현재 총 {}건으로 10개 미만. 남은 후보 중에서 {}개를 무작위로 마저 채웁니다.",
                        regionKey, currentFinalCount, paddingNeeded);

                List<TourAreaBasedSyncListDTO> paddingCandidates = regionItems.stream()
                        .filter(item -> !processedIds.contains(item.getContentid()))
                        .collect(Collectors.toList());

                Collections.shuffle(paddingCandidates);

                int paddedCount = 0;
                for (TourAreaBasedSyncListDTO paddingItem : paddingCandidates) {
                    if (paddedCount >= paddingNeeded) {
                        break;
                    }

                    try {
                        TourItemDTO tourItem = tourDataConverter.convertToTourItemDTO(paddingItem);
                        PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(paddingItem, tourItem, null, null, null);
                        tourMapper.upsertPlace(placeDto);

                        processedIds.add(paddingItem.getContentid());
                        paddedCount++;
                        log.info("[Padding Success] 빈자리 채우기 성공 - title: {}", paddingItem.getTitle());
                    } catch (Exception e) {
                        log.error("[Batch Error - Padding] placeId: {} 적재 오류: {}", paddingItem.getContentid(), e.getMessage());
                    }
                }
            }

            // 최종 결과 로깅
            int finalCount = tourMapper.selectPlaceCountByRegion(regionId);
            log.info("[Batch Sampling Complete] 지역코드 {}: 최종 DB 총 적재 건수 {}건 (유효 후보: {}건)", 
                    regionKey, finalCount, regionItems.size());

            // =====================================================================================
            // 0804 추가 [부실데이터 예외 보완 레이어] - 위 1~3단계(최대 10건 캡)와는 완전히 별개로,
            // tour 3건 + stay 3건을 블랙리스트만 통과한 부실 데이터라도 무조건 추가로 확보 (캡 없음, 최대 16건까지 가능)
            // 이 레이어는 isValid()/isValidItem()/이미지 5장↑/isValidPrice를 전혀 거치지 않고,
            // TourValidator.isValidBlacklistOnly() + 필수 필드(주소/대표이미지/개요)만 확인함
            // =====================================================================================
            String regnCd = regionKey.length() >= 2 ? regionKey.substring(0, 2) : regionKey;
            String signguCd = regionKey.length() > 2 ? regionKey.substring(2) : "";
            fillLowQualitySupplement(regnCd, signguCd, regionKey, "tour", TOUR_CONTENT_TYPES, LOW_QUALITY_SUPPLEMENT_COUNT);
            fillLowQualitySupplement(regnCd, signguCd, regionKey, "stay", STAY_CONTENT_TYPES, LOW_QUALITY_SUPPLEMENT_COUNT);

            int finalCountWithSupplement = tourMapper.selectPlaceCountByRegion(regionId);
            log.info("[Batch Sampling Complete + Supplement] 지역코드 {}: 부실데이터 보완 레이어 포함 최종 DB 총 적재 건수 {}건",
                    regionKey, finalCountWithSupplement);
        }
    }

    /**
     * [보조 메서드] 큐를 거치지 않고 특정 타입(stay 또는 tour)을 
     * 이미 처리된 ID 제외 후 무작위로 골라 지정된 개수만큼 검증 없이 강제 적재
     */
    private void processForcedRandomByType(
            List<TourAreaBasedSyncListDTO> regionItems, 
            Set<String> processedIds, 
            String targetType, 
            int targetCount) {
        
        // 1. 1차에 쓰이지 않았고, 해당 타입인 후보 필터링
        List<TourAreaBasedSyncListDTO> candidates = regionItems.stream()
                .filter(item -> !processedIds.contains(item.getContentid()))
                .filter(item -> {
                    String type = tourApiHelper.convertContentType(item.getContenttypeid(), item.getLclsSystm2(), item.getLclsSystm3());
                    return targetType.equalsIgnoreCase(type);
                })
                .collect(Collectors.toList());

        // 2. 무작위 셔플 (중분류 다양성 및 랜덤성 부여)
        Collections.shuffle(candidates);

        int savedCount = 0;
        for (TourAreaBasedSyncListDTO item : candidates) {
            if (savedCount >= targetCount) {
                break;
            }

            try {
                log.info("[PLACE SAVE FORCED - {} Type] contentId: {}, title: {}", targetType.toUpperCase(), item.getContentid(), item.getTitle());
                
                // 엄격한 검증을 타지 않고 곧바로 변환 및 강제 적재 수행
                TourItemDTO tourItem = tourDataConverter.convertToTourItemDTO(item);
                PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(item, tourItem, null, null, null);
                tourMapper.upsertPlace(placeDto);

                processedIds.add(item.getContentid());
                savedCount++;
                log.info("[Forced Success] 강제 적재 완료 - title: {}", item.getTitle());

            } catch (Exception e) {
                log.error("[Batch Error - Forced {}] placeId: {} 적재 오류: {}", targetType, item.getContentid(), e.getMessage());
            }
        }
        
        log.info("[Forced Result] 타입: {} - 목표 {}개 중 {}개 강제 적재 완료", targetType.toUpperCase(), targetCount, savedCount);
    }

    // 0804 추가 - 부실데이터 예외 보완 레이어 전용 메서드
    // TourApiClient.fetchAreaBasedList로 해당 지역+타입군을 직접 다시 조회 (1~3단계 큐와는 독립적인 별도 후보 풀)
    // TourValidator.isValidBlacklistOnly()(블랙리스트만) + 필수 필드(주소/대표이미지/개요) 확인만 거쳐 무조건 need개수만큼 추가 적재
    private void fillLowQualitySupplement(String regnCd, String signguCd, String regionKey,
                                           String bucketType, List<String> sourceContentTypeIds, int need) {
        List<TourAreaBasedSyncListDTO> pool = new ArrayList<>();
        for (String contentTypeId : sourceContentTypeIds) {
            try {
                String jsonResponse = tourApiClient.fetchAreaBasedList(regnCd, signguCd, contentTypeId, null);
                if (!StringUtils.hasText(jsonResponse)) continue;

                TourApiResponseDTO<TourAreaBasedSyncListDTO> response = objectMapper.readValue(
                        jsonResponse, new TypeReference<TourApiResponseDTO<TourAreaBasedSyncListDTO>>() { }
                );
                if (response == null || response.getResponse() == null
                        || response.getResponse().getBody() == null
                        || response.getResponse().getBody().getItems() == null
                        || response.getResponse().getBody().getItems().getItem() == null) {
                    continue;
                }
                pool.addAll(response.getResponse().getBody().getItems().getItem());
            } catch (Exception e) {
                log.warn("[Batch Low-Quality Supplement] regnCd:{} signguCd:{} contentTypeId:{} 조회 중 오류: {}",
                        regnCd, signguCd, contentTypeId, e.getMessage());
            }
        }

        // 블랙리스트만 통과 + 주소/대표이미지 1차 필수값 확인
        List<TourAreaBasedSyncListDTO> filtered = pool.stream()
                .filter(tourValidator::isValidBlacklistOnly)
                .filter(item -> StringUtils.hasText(item.getAddr1()))
                .filter(item -> StringUtils.hasText(item.getFirstimage()))
                .collect(Collectors.toList());
        Collections.shuffle(filtered);

        int saved = 0;
        for (TourAreaBasedSyncListDTO item : filtered) {
            if (saved >= need) break;

            try {
                TourItemDTO tourItem = tourDataConverter.convertToTourItemDTO(item);
                // description(개요) 필수 확보를 위해 detailCommon2만 연쇄 호출 (블랙리스트 외 다른 검증은 생략)
                if (!"0".equals(item.getShowflag())) {
                    tourApiHelper.enrichTourItemDetails(tourItem);
                }
                if (!StringUtils.hasText(tourItem.getOverview())) {
                    // description 필수 조건 미충족 - 이 후보는 스킵
                    continue;
                }

                PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(
                        item, tourItem, null, null, tourDataConverter.resolveThumbnailImage(item));
                tourMapper.upsertPlace(placeDto);

                saved++;
                log.info("[Low-Quality Supplement Success] 지역코드 {} 타입 {} - title: {}", regionKey, bucketType, item.getTitle());
            } catch (Exception e) {
                log.error("[Batch Error - Low-Quality Supplement] 지역코드 {} 타입 {} contentId:{} 적재 오류: {}",
                        regionKey, bucketType, item.getContentid(), e.getMessage());
            }
        }
        log.info("[Low-Quality Supplement Result] 지역코드 {} 타입 {} - 목표 {}건 중 {}건 확보", regionKey, bucketType, need, saved);
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
    // 0804 DB 적재 성공 여부를 boolean으로 리턴하도록 보정하여 정직한 카운팅 보장
    private boolean processSinglePlace(TourAreaBasedSyncListDTO syncItem) {
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

        // 상세 이미지 API를 먼저 조회하여 최소 이미지 개수(2장 이상) 검증 수행
        List<TourDetailImageDTO> detailImages = tourApiHelper.fetchDetailImages(syncItem.getContentid());
        List<String> imageUrls = new ArrayList<>();
        if (detailImages != null && !detailImages.isEmpty()) {
            imageUrls = detailImages.stream()
                    .map(TourDetailImageDTO::getOriginimgurl)
                    .filter(StringUtils::hasText)
                    .distinct() // 중복 URL 제거
                    .toList();
        }

        // 2차 상세 필터링 A: 이미지 개수 부족 시 스킵 (DB 적재 안함 -> false 반환)
        if (imageUrls.size() < 5) {
            log.warn("[PLACE SAVE SKIP] 이미지 개수 부족 (현재 {}장 / 기준 5장) - contentId: {}, title: {}",
                    imageUrls.size(), syncItem.getContentid(), syncItem.getTitle());
            return false;
        }

        // PlaceImage 판단 로직에서 썸네일(카드용) 이미지 먼저 계산 -> convertToPlaceDTO로 전달
        String thumbnailImage = tourDataConverter.resolveThumbnailImage(syncItem);

        // convertToPlaceDTO -> 서비스 PlaceDTO 변환 및 해시태그 생성
        PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(syncItem, tourItem, introDetail, infoDetail, thumbnailImage);

        // 2차 상세 필터링 B: 부실 가격 검증 실패 시 스킵 (DB 적재 안함 -> false 반환)
        if (!tourValidator.isValidPrice(placeDto.getMinPrice(), placeDto.getPlaceType(), placeDto.getUseFeeInfo())) {
            log.warn("[PLACE SAVE SKIP] 가격 검증 실패 - contentId: {}, title: {}", syncItem.getContentid(), syncItem.getTitle());
            return false;
        }

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
        // 실제 DB 저장까지 성공했으므로 true 리턴
        return true;
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