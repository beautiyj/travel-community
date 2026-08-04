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

    // TODO: 0804 무작위 샘플링 및 DB 적재 핵심 처리 메서드 (2단계 Fallback 확장 적용)
    // - contenttypeid가 아니라 placeType(tour/stay/food) 기준으로 그룹핑하여 타입별 최소 1개 보장
    // - 재실행/부분 재수집 시 지역당 총 10개를 넘지 않도록 기존 DB 적재 건수 확인
    // - DB 실제 성공 건수만 정직하게 카운팅 및 타입별 분포 로그 추가
    // - 1차 정교한 큐 순회 후 목표 건수(10개) 미달 시, 2차 보완(조건 완화/부실 데이터 허용) 로직으로 잔여 수량 충족
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

            // regionId(Integer) 파싱 - DB 기존 건수 조회 및 PlaceDTO 매핑 시 사용하는 동일한 PK 체계
            Integer regionId;
            try {
                regionId = Integer.parseInt(regionKey);
            } catch (NumberFormatException e) {
                log.warn("[Batch Sampling] 지역코드 파싱 실패 - regionKey: {}", regionKey);
                continue;
            }

            // 이미 목표 수량(10개)을 채운 지역이면 스킵 - 여러 타입/재실행 시 중복 초과 적재 방지
            int existingCount = tourMapper.selectPlaceCountByRegion(regionId);
            int targetGoal = 10;
            if (existingCount >= targetGoal) {
                log.info("[Batch Sampling Skip] 지역코드 {}: 이미 목표 수량 충족 - 현재 {}건", regionKey, existingCount);
                continue;
            }

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
                    candidateQueue.add(typeList.remove(0)); // 셔플된 목록 중 첫 번째 무작위 추출 후 제거
                }
            }

            // 남은 후보군을 다시 하나로 모아 셔플 후 큐 뒤에 이어 붙임
            List<TourAreaBasedSyncListDTO> remainList = typedMap.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            Collections.shuffle(remainList);
            candidateQueue.addAll(remainList);

            // 무작위 선발 후보군 순회 및 2차 필터링 통과 시 DB 적재 (목표 10개 도달 시까지 탐색)
            int currentTotalCount = existingCount;
            int newlySavedCount = 0;

            // [1단계] 기존 정교한 큐 기반 수집 시도
            for (TourAreaBasedSyncListDTO targetItem : candidateQueue) {
                // 이미 기존 건수 + 신규 적재 건수가 10개가 되었으면 추가 탐색 즉시 중단
                if (currentTotalCount >= targetGoal) {
                    break;
                }

                try {
                    log.info("[PLACE SAVE TRY - Stage 1] contentId: {}, title: {}", targetItem.getContentid(), targetItem.getTitle());

                    // processSinglePlace 가 true(실제 DB 적재 성공)를 반환할 때만 카운트 증가
                    if (processSinglePlace(targetItem)) {
                        currentTotalCount++;
                        newlySavedCount++;
                    }
                } catch (Exception e) {
                    log.error("[Batch Error - Stage 1] 지역코드 {} placeId: {} DB 적재 오류: {}",
                            regionKey, targetItem.getContentid(), e.getMessage());
                }
            }

            // [2단계 Fallback] 1차 큐를 모두 소진했음에도 목표치(10개)에 미달한 경우, 조건 완화/부실 데이터 허용 로직으로 잔여분 충족
            if (currentTotalCount < targetGoal) {
                int remainingNeeded = targetGoal - currentTotalCount;
                log.info("[Batch Fallback Triggered] 지역코드 {}: 1차 수집 완료(총 {}건). 부족한 {}개를 채우기 위해 2단계(조건 완화) 보완 수집을 시작합니다.",
                        regionKey, currentTotalCount, remainingNeeded);

                // TODO: 0804 필요에 따라 이미 1차에 사용된 아이템들을 제외하거나, 
                // 빡빡한 필터링 조건(이미지 수 등)을 해제한 relaxed 후보군 리스트를 가져와서 셔플 후 순회
                List<TourAreaBasedSyncListDTO> relaxedQueue = getRelaxedCandidateQueue(regionItems, candidateQueue);

                for (TourAreaBasedSyncListDTO relaxedItem : relaxedQueue) {
                    if (currentTotalCount >= targetGoal) {
                        break;
                    }

                    try {
                        log.info("[PLACE SAVE TRY - Stage 2 Fallback] contentId: {}, title: {}", relaxedItem.getContentid(), relaxedItem.getTitle());

                        // 조건이 완화된 단건 처리 메서드 (또는 기존 processSinglePlace 내부에서 완화 조건을 탈 수 있도록 분기 처리)
                        if (processSinglePlaceRelaxed(relaxedItem)) {
                            currentTotalCount++;
                            newlySavedCount++;
                        }
                    } catch (Exception e) {
                        log.error("[Batch Error - Stage 2 Fallback] 지역코드 {} placeId: {} DB 적재 오류: {}",
                                regionKey, relaxedItem.getContentid(), e.getMessage());
                    }
                }
            }

            // 정직한 실제 DB 성공 결과 출력
            log.info("[Batch Sampling] 지역코드 {}: 기존 {}건 + 실제 최종 적재 {}건 완료 (최종 총 {}건 / 전체 유효 후보: {}건)",
                    regionKey, existingCount, newlySavedCount, currentTotalCount, regionItems.size());
        }
    }

    /**
     * 1차 큐에 포함되지 않았거나 조건이 완화된 2단계 보완 후보군을 반환하는 메서드
     */
    private List<TourAreaBasedSyncListDTO> getRelaxedCandidateQueue(
            List<TourAreaBasedSyncListDTO> regionItems, 
            List<TourAreaBasedSyncListDTO> candidateQueue) {
        
        // 1차 큐에 들어있던 아이템들의 contentId 집합 추출
        Set<String> processedIds = candidateQueue.stream()
                .map(TourAreaBasedSyncListDTO::getContentid)
                .collect(Collectors.toSet());

        // 전체 지역 아이템 중 1차에 쓰이지 않은 나머지 항목들을 2단계 후보로 활용
        List<TourAreaBasedSyncListDTO> relaxedList = regionItems.stream()
                .filter(item -> !processedIds.contains(item.getContentid()))
                .collect(Collectors.toList());

        // 무작위 셔플
        Collections.shuffle(relaxedList);
        return relaxedList;
    }

    /**
     * 2단계 보완 시 부실 데이터를 허용하거나 검증 조건을 낮추어 처리하는 단건 메서드
     * (기존 processSinglePlace 로직에서 이미지 수 제한 등의 조건을 완화한 버전, 
     * 혹은 기존 processSinglePlace 내부에서 완화 플래그를 받을 수 있다면 그것을 활용해도 좋습니다)
     */
    private boolean processSinglePlaceRelaxed(TourAreaBasedSyncListDTO targetItem) {
        // TODO: 0804 2단계에서는 이미지 수 5장 미만 등의 부실 데이터 조건 완화 적용 후 DB 저장 시도
        // 기존 processSinglePlace 메서드와 동일하되, 내부 필터링 조건만 완화된 쿼리/로직을 태우거나
        // 기존 processSinglePlace 메서드에 boolean isRelaxed 파라미터를 추가하여 재사용할 수 있습니다.
        
        // 예시: 기존 저수준 저장 메서드를 그대로 호출하되, 내부적으로 빡빡한 필터링 검증을 스킵하도록 처리
        return processSinglePlace(targetItem); 
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