package com.gnagnoohc.travel.batch.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO.Header;
import com.gnagnoohc.travel.batch.dto.TourAreaBasedSyncListDTO;
import com.gnagnoohc.travel.batch.dto.TourItemDTO;
import com.gnagnoohc.travel.batch.dto.TourLdongCodeDTO;
import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.RegionEntity;

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
     * TODO: TourApiService 로직 미완성, batch->place 과정 확인 필요
     * 외부 공공데이터를 수집하여 우리 DB(PLACE 테이블)에 적재하는 메인 파이프라인 0723 11:25 미완성
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

    // TourLdongCodeDTO 법정동코드 -> RegionEntity 데이터 변환
    // (lDongListYN 값 Y/N 차이에 대응)
    // 최종 배치서비스단에서 컨버터처리하는 배치함수만들때convertToRegionEntity 넣으면됨
    private RegionEntity convertToRegionEntity(TourLdongCodeDTO ldongCodeDTO) {
        // 법정동 코드, 법정동 명칭
        String rawCode;
        String rawName;

        // 1. 법정동 목록조회 여부 - Y(전체목록조회) 일 때 엔티티 변환해서 넣기
        // 법정동코드 Y일땐 필드가 lDongRegnCd, lDongSignguCd 로 넘어오니까 이게 널이 아닐 때
        if (ldongCodeDTO.getLDongRegnCd() != null && !ldongCodeDTO.getLDongRegnCd().isBlank()) {
            
            // 우리의 법정동 코드는 시도코드 + 시군구코드로 처리해서 넣는다 (11+110)
            rawCode = ldongCodeDTO.getLDongRegnCd() + 
                    (ldongCodeDTO.getLDongSignguCd() != null ? ldongCodeDTO.getLDongSignguCd() : "");
            
            // 우리의 법정동 명칭은 시도명 + " " + 시군구명으로 처리해서 넣는다 (서울+종로구)
            rawName = ldongCodeDTO.getLDongRegnNm();
            if (ldongCodeDTO.getLDongSignguNm() != null && !ldongCodeDTO.getLDongSignguNm().isBlank()) {
                rawName += " " + ldongCodeDTO.getLDongSignguNm();
            }
        } 
        // 2. 법정동 목록조회 여부 - N(코드조회)일 때 엔티티 변환 넣는 처리는
        else {
            // 필드 1:1 매핑되니까 문제없음
            rawCode = ldongCodeDTO.getCode();
            rawName = ldongCodeDTO.getName();
        }

        // 1-2. 지역테이블 PK로 처리될 rawCode가 null이거나 비어있으면 우리의 DB에 넣을 수 없으므로 null 반환
        if (rawCode == null || rawCode.isBlank()) {
            log.warn("유효하지 않은 법정동 코드 데이터: 엔티티 변환을 건너뜁니다. DTO: {}", ldongCodeDTO);
            return null; // Spring Batch에서 null을 리턴하면 해당 아이템을 Skip 처리함
        }

        // 3. 추출한 String rawCode -> Long 타입의 regionId로 변환 (NULL처리 된 정제데이터이므로 바로 주입)
        Long regionId = Long.parseLong(rawCode);

        // rawCode의 앞 2자리를 상위 parentRegionId(시/도)로 파싱 (예: "11110" -> 11L)
        Long parentRegionId = null;
        if (rawCode != null && rawCode.length() >= 5) {
            parentRegionId = Long.parseLong(rawCode.substring(0, 2));
        }

        // 4. Setter 없이 생성자로 닫혀있는 순수한 RegionEntity 객체 한 번에 생성
        return new RegionEntity(regionId, rawName, parentRegionId);
    }

    /**
     * 외부 공공데이터를 수집하여 우리 DB(PLACE 테이블)에 적재하는 메인 파이프라인
     */
    @Transactional
    public void syncTourData() {
        log.info("=== [Batch] 공공데이터 외부 수집 및 PLACE 적재 시작 ===");
        
        int pageNo = 1;
        boolean hasNext = true;

        while (hasNext) {
            try {
                // 1. [동기화 목록 API 호출]
                String jsonResponse = tourApiClient.fetchAreaBasedSyncList(pageNo, null, null, null);
                
                if (jsonResponse == null || jsonResponse.isBlank()) {
                    log.info("[Batch] 더 이상 수집할 데이터가 없습니다. 루프를 종료합니다.");
                    break;
                }

                // 2. String JSON -> TourApiResponseDTO<TourAreaBasedSyncListDTO> 매핑
                TourApiResponseDTO<TourAreaBasedSyncListDTO> response = objectMapper.readValue(
                    jsonResponse, 
                    new TypeReference<TourApiResponseDTO<TourAreaBasedSyncListDTO>>() {}
                );

                // 🎯 [오류 매뉴얼 검증 1] 공공데이터 응답 Header 결과 코드 검증
                if (response != null && response.getResponse() != null && response.getResponse().getHeader() != null) {
                    Header header = response.getResponse().getHeader();
                    String resultCode = header.getResultCode();
                    
                    // 정상 응답 코드("0000" 또는 "00")가 아닌 경우 매뉴얼 에러 로그 출력 후 루프 종료
                    if (!"0000".equals(resultCode) && !"00".equals(resultCode)) {
                        log.error("[Batch API 오류] 코드: {}, 메시지: {}", resultCode, header.getResultMsg());
                        break;
                    }
                }

                if (response == null 
                        || response.getResponse() == null 
                        || response.getResponse().getBody() == null 
                        || response.getResponse().getBody().getItems() == null 
                        || response.getResponse().getBody().getItems().getItem() == null) {
                    log.info("[Batch] 더 이상 수집할 데이터가 없습니다. 루프를 종료합니다.");
                    break;
                }

                List<TourAreaBasedSyncListDTO> syncList = response.getResponse().getBody().getItems().getItem();

                if (syncList.isEmpty()) {
                    break;
                }
                
                // 3. [데이터 변환, 상세 조립 및 PLACE 적재]
                for (TourAreaBasedSyncListDTO syncItem : syncList) {
                    
                    // SyncDTO -> TourItemDTO로 변환
                    TourItemDTO tourItem = convertToTourItemDTO(syncItem);

                    // showflag가 "0"(비표출)이 아닌 영업 중인 데이터만 상세 정보 연쇄 호출
                    if (!"0".equals(syncItem.getShowflag())) {
                        enrichTourItemDetails(tourItem);
                    }

                    // 💡 [해시태그 파싱] TourItemDTO에 가공된 hashtags 컬럼 문자열 세팅 (#food,#대표메뉴 등)
                    String generatedHashtags = generateHashtags(tourItem);
                    tourItem.setHashtags(generatedHashtags);
                    
                    // TourItemDTO 객체로 Mapper 호출 (사용자 원본 방식)
                    tourMapper.upsertPlace(tourItem);
                }

                // 4. 페이징 검증 (가져온 데이터 건수가 500개 미만이면 마지막 페이지)
                if (syncList.size() < 500) {
                    hasNext = false;
                } else {
                    pageNo++;
                }

            } catch (Exception e) {
                log.error("[Batch] {} 페이지 수집 중 에러 발생 - 파이프라인 일시 중단", pageNo, e);
                hasNext = false; 
            }
        }
        
        log.info("=== [Batch] 공공데이터 수집 및 PLACE 적재 완료 ===");
    }

    /**
     * TourAreaBasedSyncListDTO -> TourItemDTO 변환 메서드
     */
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
        
        // 원본 DTO 소문자 lDong / lcls 필드 스펙에 맞춘 Getter 호출
        item.setLDongRegnCd(syncItem.getLDongRegnCd());
        item.setLDongSignguCd(syncItem.getLDongSignguCd());
        item.setLclsSystm1(syncItem.getLclsSystm1());
        item.setLclsSystm2(syncItem.getLclsSystm2());
        item.setLclsSystm3(syncItem.getLclsSystm3());
        return item;
    }

    /**
     * 1:1 상세 API 연쇄 호출로 TourItemDTO의 상세 필드를 채우는 메서드
     */
    private void enrichTourItemDetails(TourItemDTO masterItem) {
        String contentId = masterItem.getContentid();

        try {
            // 1. 상세 공통 정보 조회 (/detailCommon2)
            String commonJson = tourApiClient.fetchDetailCommon(contentId);
            if (commonJson != null && !commonJson.isBlank()) {
                TourApiResponseDTO<TourItemDTO> commonResponse = objectMapper.readValue(
                    commonJson, 
                    new TypeReference<TourApiResponseDTO<TourItemDTO>>() {}
                );

                // 🎯 [오류 매뉴얼 검증 2] detailCommon2 Header 검증
                if (commonResponse != null && commonResponse.getResponse() != null && commonResponse.getResponse().getHeader() != null) {
                    Header header = commonResponse.getResponse().getHeader();
                    if (!"0000".equals(header.getResultCode()) && !"00".equals(header.getResultCode())) {
                        log.warn("[detailCommon2 API 오류] contentId: {}, 코드: {}, 메시지: {}", contentId, header.getResultCode(), header.getResultMsg());
                    }
                }

                if (commonResponse != null 
                        && commonResponse.getResponse() != null 
                        && commonResponse.getResponse().getBody() != null 
                        && commonResponse.getResponse().getBody().getItems() != null 
                        && !commonResponse.getResponse().getBody().getItems().getItem().isEmpty()) {
                    
                    TourItemDTO commonDetail = commonResponse.getResponse().getBody().getItems().getItem().get(0);
                    masterItem.setOverview(commonDetail.getOverview());
                    
                    if (masterItem.getTel() == null || masterItem.getTel().isBlank()) {
                        masterItem.setTel(commonDetail.getTel());
                    }
                }
            }

            // 2. 반려동물 동반 정보 조회 (/detailPetTour2)
            String petJson = tourApiClient.fetchDetailPetTour(contentId);
            if (petJson != null && !petJson.isBlank()) {
                TourApiResponseDTO<TourItemDTO> petResponse = objectMapper.readValue(
                    petJson, 
                    new TypeReference<TourApiResponseDTO<TourItemDTO>>() {}
                );

                // 🎯 [오류 매뉴얼 검증 3] detailPetTour2 Header 검증
                if (petResponse != null && petResponse.getResponse() != null && petResponse.getResponse().getHeader() != null) {
                    Header header = petResponse.getResponse().getHeader();
                    if (!"0000".equals(header.getResultCode()) && !"00".equals(header.getResultCode())) {
                        log.warn("[detailPetTour2 API 오류] contentId: {}, 코드: {}, 메시지: {}", contentId, header.getResultCode(), header.getResultMsg());
                    }
                }

                if (petResponse != null 
                        && petResponse.getResponse() != null 
                        && petResponse.getResponse().getBody() != null 
                        && petResponse.getResponse().getBody().getItems() != null 
                        && !petResponse.getResponse().getBody().getItems().getItem().isEmpty()) {
                    
                    TourItemDTO petDetail = petResponse.getResponse().getBody().getItems().getItem().get(0);
                    masterItem.setAcmpyPsblCpam(petDetail.getAcmpyPsblCpam());
                    masterItem.setRelaRntlPrdlst(petDetail.getRelaRntlPrdlst());
                    masterItem.setAcmpyNeedMtr(petDetail.getAcmpyNeedMtr());
                    masterItem.setRelaFrnshPrdlst(petDetail.getRelaFrnshPrdlst());
                    masterItem.setEtcAcmpyInfo(petDetail.getEtcAcmpyInfo());
                    masterItem.setRelaAcdntRiskMtr(petDetail.getRelaAcdntRiskMtr());
                    masterItem.setAcmpyTypeCd(petDetail.getAcmpyTypeCd());
                    masterItem.setRelaPosesFclty(petDetail.getRelaPosesFclty());
                    masterItem.setPetTursmInfo(petDetail.getPetTursmInfo());
                }
            }

        } catch (Exception e) {
            log.warn("[Batch] 상세 정보 연쇄 호출 실패 - contentId: {}, 사유: {}", contentId, e.getMessage());
        }
    }

    // =========================================================================
    // 💡 [공통 헬퍼 메서드] 배치 가공용 해시태그 및 분류 매핑 유틸 (최하단 위치)
    // =========================================================================

    /**
     * TourItemDTO의 수집 데이터를 파싱하여 DB hashtags 컬럼용 문자열 생성 (#food,#흑돼지구이,#주차가능)
     */
    private String generateHashtags(TourItemDTO item) {
        List<String> tags = new ArrayList<>();
        
        // 1. contentTypeId -> place_type 기본 해시태그 생성 (#tour, #stay, #food)
        String placeType = convertContentType(item.getContenttypeid());
        tags.add("#" + placeType);

        // 2. 관광지(12) 특성상 입장료 필드가 없으므로 기본값 세팅
        if ("tour".equals(placeType)) {
            tags.add("#무료"); // 기본 입장료 태그 보장
        }

        // 3. 반려동물 정보 존재 시 태그 자동 추가
        if (StringUtils.hasText(item.getAcmpyPsblCpam()) || StringUtils.hasText(item.getPetTursmInfo())) {
            tags.add("#반려동물동반");
        }

        // 결과 예시: "#food,#반려동물동반"
        return String.join(",", tags);
    }

    /**
     * 공공데이터 contentTypeId -> 서비스 표준 place_type (tour, stay, food) 변환
     */
    private String convertContentType(String contentTypeId) {
        if (contentTypeId == null) return "tour";
        return switch (contentTypeId) {
            case "32" -> "stay";
            case "39" -> "food";
            default   -> "tour"; // 12(관광지), 14(문화시설), 25(여행코스), 28(레포츠) -> tour로 통합
        };
    }
}