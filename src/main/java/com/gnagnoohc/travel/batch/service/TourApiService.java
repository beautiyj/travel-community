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

import java.util.List;

/*  TODO: 0729 공공데이터 로직 확인 완료, 수정필요한부분:
1. TourValidator 처리완료

2. 이미지테이블에서 썸네일만 중복으로 들어오는 문제 해결 필요 + 그외 원본이미지 확인 필요,
    필요 시 1장만 존재하는 가게는 필터링으로 제거 & 최소 2장부터 허가하고 / 데베에는 중복으로 들어오지 않고 SORT_ORDER에만 인덱싱처리되도록 처리 필요

3. 숙박으로 지정해도 STAY가 아니라 TOUR로 들어오는 문제 확인
    (테스트로직의 문제인지, 설정 문제인지)
    3-1. 스테이 설정에서 협동조합 등의 숙박지역이 아닌 값까지 들어오는 처리 재확인 / 숙박 부분은 가격 재확인 후 필터링 처리 추가할 것

4. 공통: 해시태그처리에서 한글이 아닌 영어타입 그대로 태그처리되는 부분 확인 필요
    4-1. 해시 처리에서 관광지 1차 필터링 후, 무료해시가 많을 경우 무료해시 값은 N배수로 필터링 처리하여 수 조절할 것
    4-2. 해시태그 필터링 수정 작업 필요함 해시태그 최소3필터링 진행, 숙박의 경우 숙박타입 필수로 태그(확인필요)/맛집은 메뉴태그(확인완)/관광지는 필터링처리 이후 태그 수정작업

5. 금액 부분/요금설명 부분에서 NULL 많을 경우 필터링 처리해서 정보값 있는 데이터만 들여올 것

6. 공공데이터 받아올 때, 데이터 오염으로 어려울 경우 각 지역군을 기준으로 1차 데이터 필터링 - 각 지역별로 일부 LIMIT걸어서 가져오기, 스케줄러로 관리 진행

7. 이미지 테이블 처리 후, 저작권 분류 타입 재확인 - 필터링 처리 여부 결정

8.

이후 테스트 시 데베 내의 데이터 삭제 후 페이징처리 진행되는지도 재확인 필요(투어는 확인, 현재 스테이/푸드 부분 확인 필요)
0729 아직 DEVELOP 안받아옴 / 현재 YUN원격로직: 공공데이터 미완성 상태

*/

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
    private final TourValidator tourValidator;

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
            syncTourData(contentTypeId, 1); //0729
        }
        log.info("[Batch Total] 전체 타깃 수집 프로세스 완료");
    }

    // TODO: syncRegionData() 테스트로직 이후 최종 수정 예정, 주석코드 또한 테스트 이후 조정하여 삭제 예정
    // // 법정동 코드 수집 및 REGION 적재 파이프라인
    // @Transactional
    // public void syncRegionData() {
    //     log.info("[Batch] 법정동 코드 수집 시작");
    //     try {
    //         // 0728
    //         String jsonResponse = tourApiClient.fetchLdongCode(null, "Y");
    //         if (!StringUtils.hasText(jsonResponse)) return;
    //         TourApiResponseDTO<TourLdongCodeDTO> response = objectMapper.readValue(
    //                 jsonResponse, new TypeReference<TourApiResponseDTO<TourLdongCodeDTO>>() {}
    //         );

    //         if (response != null && response.getResponse() != null
    //                 && response.getResponse().getBody() != null
    //                 && response.getResponse().getBody().getItems() != null) {

    //             List<TourLdongCodeDTO> items = response.getResponse().getBody().getItems().getItem();

    //             // TODO: 디버깅용 임시 로그 - 원인 확인 후 삭제 필요
    //             log.info("[Batch Debug] 수집된 items 개수: {}", items.size());
    //             if (!items.isEmpty()) {
    //                 log.info("[Batch Debug] 첫 번째 dto 전체 내용: {}", items.get(0).toString());
    //             }


    //             for (TourLdongCodeDTO dto : items) {
    //                 RegionDTO regionDto = tourDataConverter.convertToRegionDTO(dto);
    //                 if (regionDto != null) {
    //                     tourMapper.upsertRegion(regionDto);
    //                 }
    //             }
    //         }
    //     } catch (Exception e) {
    //         log.error("[Batch] 법정동 코드 수집 중 오류 발생", e);
    //     }
    //     log.info("[Batch] 법정동 코드 수집 완료");
    // }

    // 법정동 코드 수집 및 REGION 적재 파이프라인
    @Transactional
    public void syncRegionData() {
        log.info("[Batch] 법정동 코드 수집 시작");
        try {
            // 0728
            String jsonResponse = tourApiClient.fetchLdongCode(null, "Y");
            if (!StringUtils.hasText(jsonResponse)) return;
            TourApiResponseDTO<TourLdongCodeDTO> response = objectMapper.readValue(
                    jsonResponse, new TypeReference<TourApiResponseDTO<TourLdongCodeDTO>>() {}
            );

            if (response != null && response.getResponse() != null
                    && response.getResponse().getBody() != null
                    && response.getResponse().getBody().getItems() != null) {

                List<TourLdongCodeDTO> items = response.getResponse().getBody().getItems().getItem();

                // TODO: 0729 FK 제약(FK_REGION_PARENT) 위반 문제 해결 -> 시/군/구(자식)가 시/도(부모)보다 먼저 upsert되며 발생
                // [수정] 1단계: 269건 안의 시/도 코드(lDongRegnCd)를 중복 제거해서 먼저 부모 로우(parent_region_id=null)로 upsert
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
                    if (parentRegionDto != null) {
                        tourMapper.upsertRegion(parentRegionDto);
                    }
                }

                // [수정] 2단계: 기존 로직 그대로 - 269건(시/군/구)을 순회하며 upsert
                // 이제 부모(시/도)가 이미 존재하므로 FK 제약 통과
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
    // 0729 운영용 메인 파이프라인 (시작할 pageNo를 외부에서 주입받을 수 있도록 구조 변경)
    @Transactional
    public void syncTourData(String contentTypeId, int startPageNo) {
        log.info("[Batch] 공공데이터 외부 수집 및 PLACE 적재 시작");
        // 0729
        int pageNo = startPageNo;
        // 고정값 1 대신 전달받은 시작 페이지 사용
        // int pageNo = 1;
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
                    // 1차 필터링: Validator 검증 통과한 데이터만 살아남음
                    if (!tourValidator.isValid(syncItem)) {
                        continue;
                    }
                    // 1) 대표 이미지가 없으면 연쇄 호출 및 저장 과정 전체 스킵(우리의 서비스에 적재x)
                    if (!tourApiHelper.isValidItem(syncItem)) { continue; }
                    // 2) 단일 아이템 수집 및 DB 적재는 헬퍼 메서드로 위임하기
                    processSinglePlace(syncItem);
                }

                log.info("[Batch Pagination Debug] 현재 요청 pageNo: {}, 가져온 item 개수: {}", pageNo, syncList.size());

                // 0729
                // if (syncList.size() < 500) {
                // if (syncList.isEmpty()) {
                //     hasNext = false;
                // } else {
                //     pageNo++;
                // }
                // 수정 후 (데이터가 텅 빌 때까지 다음 페이지로 전진하도록 변경)
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
        log.info("[Batch] contentTypeId: {} 수집 완료", contentTypeId);
    }

    // TODO: 테스트 완료 후 반드시 삭제 대상 (운영 배포 전 제거) - syncTourData 원본은 절대 건드리지 않고, 소량 검증용 오버로드로 별도 추가
    // 테스트 전용 - 지정한 limit 개수만큼만 수집하고 강제 종료 (원본 syncTourData의 무제한 페이징 대신 소량 검증용)
    // 0729 실제테스트처럼 int startPageNo 추가하여 테스트
    @Transactional
    public void syncTourDataForTest(String contentTypeId, int startPageNo, int limit) {
        log.info("[Batch Test] 공공데이터 소량 테스트 수집 시작 - contentTypeId: {}, limit: {}", contentTypeId, limit);
        int pageNo = startPageNo;
        boolean hasNext = true;
        int processedCount = 0; // 테스트용 처리 개수 카운터

        while (hasNext) {
            try {
                String jsonResponse = tourApiClient.fetchAreaBasedSyncList(pageNo, contentTypeId, null, null);
                if (!StringUtils.hasText(jsonResponse)) {
                    log.info("[Batch Test] 더 이상 수집할 데이터가 없습니다. 루프를 종료합니다.");
                    break;
                }
                TourApiResponseDTO<TourAreaBasedSyncListDTO> response = objectMapper.readValue(
                        jsonResponse, new TypeReference<TourApiResponseDTO<TourAreaBasedSyncListDTO>>() {}
                );

                if (response != null && response.getResponse() != null && response.getResponse().getHeader() != null) {
                    Header header = response.getResponse().getHeader();
                    String resultCode = header.getResultCode();
                    if (!"0000".equals(resultCode) && !"00".equals(resultCode)) {
                        log.error("[Batch Test API 오류] 코드: {}, 메시지: {}", resultCode, header.getResultMsg());
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

                for (TourAreaBasedSyncListDTO syncItem : syncList) {
                    if (!tourApiHelper.isValidItem(syncItem)) { continue; }

                    processSinglePlace(syncItem);
                    processedCount++;
                    log.info("[Batch Test] {}/{} 건 처리 완료 - contentId: {}", processedCount, limit, syncItem.getContentid());

                    // limit 도달 시 즉시 종료 (테스트용 소량 제한)
                    if (processedCount >= limit) {
                        hasNext = false;
                        break;
                    }
                }

                // if (hasNext && syncList.size() < 500) {
                //     hasNext = false;
                // } else if (hasNext) {
                //     pageNo++;
                // }
                log.info("[Batch Test Pagination] 현재 페이지({}) 처리 완료. 누적 성공 건수: {}", pageNo, processedCount);

                if (hasNext) {
                    pageNo++; // 다음 페이지로 전진
                }

            } catch (Exception e) {
                log.error("[Batch Test] {} 페이지 수집 중 에러 발생 - 일시 중단", pageNo, e);
                hasNext = false;
            }
        }
        log.info("[Batch Test] contentTypeId: {} 소량 테스트 수집 완료 - 총 {}건 처리", contentTypeId, processedCount);
    }

    /* 헬퍼 메소드 - 개별 장소의 상세정보 연쇄 수집 및 DB 적재(PLACE + PLACE_IMAGE)
       소개정보(/detailIntro2) & 반복정보(/detailInfo2) 조회용 헬퍼 각각 호출함 */
    private void processSinglePlace(TourAreaBasedSyncListDTO syncItem) {
        // [디버그 로그 추가] 공공데이터 목록 API가 실제로 어떤 시도/시군구 코드를 들고 오는지 확인
        log.info("[Batch Debug Place] title: {}, lDongRegnCd: {}, lDongSignguCd: {}", 
                syncItem.getTitle(), syncItem.getLDongRegnCd(), syncItem.getLDongSignguCd());
                
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
        // PlaceImage 판단 로직에서 썸네일(카드용) 이미지 먼저 계산 -> convertToPlaceDTO로 전달
        String thumbnailImage = tourDataConverter.resolveThumbnailImage(syncItem);

        // convertToPlaceDTO -> 서비스 PlaceDTO 변환 및 해시태그 생성
        PlaceDTO placeDto = tourDataConverter.convertToPlaceDTO(syncItem, tourItem, introDetail, infoDetail, thumbnailImage);
        
        // TODO: 0728 데이터베이스 연결 후 테스트 로직 연동 - 로그 확인, 이후 삭제 필요
        // // 0728🔥 [추천 위치] DB 적재 직전에 로그 찍기 (가공된 모든 데이터가 담겨 있으므로 확인하기 가장 좋습니다)
        // log.info("[Batch Test Log] 파싱된 장소 데이터 -> ID: {}, 타이틀: {}, 타입: {}, 가격: {}, 썸네일: {}",
        //         placeDto.getPlaceId(), placeDto.getName(), placeDto.getPlaceType(), placeDto.getMinPrice(), placeDto.getFirstImage());

        tourMapper.upsertPlace(placeDto);

        // 대표 이미지가 있는 경우 PLACE_IMAGE 테이블 INSERT 적재 (원본 이미지 저장)
        if (StringUtils.hasText(syncItem.getFirstimage()) && placeDto.getPlaceId() != null) {
            PlaceImageDTO imageDto = tourDataConverter.convertToPlaceImageDTO(
                    placeDto.getPlaceId(), syncItem.getFirstimage(), 0
            );
            if (imageDto != null) {
                tourMapper.insertPlaceImage(imageDto);
            }
        }

    }

}