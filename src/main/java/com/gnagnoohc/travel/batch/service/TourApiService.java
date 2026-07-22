package com.gnagnoohc.travel.batch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO.Header;
import com.gnagnoohc.travel.batch.dto.TourAreaBasedSyncListDTO;
import com.gnagnoohc.travel.batch.dto.TourItemDTO;
import com.gnagnoohc.travel.tour.mapper.TourMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
}