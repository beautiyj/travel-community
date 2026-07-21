// package com.gnagnoohc.travel.batch.service;

// import java.util.List;

// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.gnagnoohc.travel.batch.client.TourApiClient;
// import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO;
// import com.gnagnoohc.travel.batch.dto.TourItemDTO;
// import com.gnagnoohc.travel.batch.mapper.TourItemMapper;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// // 역할: 공공데이터 수집, TourItemMapper를 통해 현 프로젝트 테이블에 공공데이터 넣는 로직
// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class TourApiService {

//     private final TourApiClient tourApiClient;
//     private final TourItemMapper tourItemMapper; // 🎯 PLACE 테이블로 밀어 넣는 망치

//     /**
//      * 🚀 외부 공공데이터를 수집하여 우리 DB(PLACE 테이블)에 적재하는 메인 파이프라인
//      */
//     @Transactional
//     public void syncTourData() {
//         log.info("=== [Batch] 공공데이터 외부 수집 및 PLACE 적재 시작 ===");
        
//         int pageNo = 1;
//         int numOfRows = 50; 
//         boolean hasNext = true;

//         while (hasNext) {
//             try {
//                 // 1. [목록 호출] 외부 API로부터 기본 리스트 수집
//                 TourApiResponseDTO response = tourApiClient.fetchAreaBasedList(pageNo, numOfRows);
                
//                 if (response == null || response.getBody() == null || response.getBody().getItems() == null) {
//                     log.info("[Batch] 더 이상 수집할 데이터가 없습니다. 루프를 종료합니다.");
//                     break;
//                 }

//                 List<TourItemDTO> itemList = response.getBody().getItems();
                
//                 // 2. [상세 조립 및 PLACE 적재]
//                 for (TourItemDTO item : itemList) {
                    
//                     // 🎯 만약 동기화 리스트(B안)를 쓸 때 showflag가 "0"인 녀석을 만나면 
//                     // 우리 PLACE 테이블 규격인 isClosed = true (휴폐업) 상태를 세팅
//                     // (※ 현재 일반 목록 수집 단계라면 기본값인 false로 돕니다)
//                     if ("0".equals(item.getShowflag())) {
//                         item.setIsClosed(true); 
//                     } else {
//                         item.setIsClosed(false);
//                         // 영업 중인 데이터만 상세 정보를 추가로 호출하여 채움
//                         enrichTourItemDetails(item);
//                     }
                    
//                     // 🎯 [최종 목적] 완전히 채워진 DTO를 우리 PLACE 테이블에 알박기(UPSERT)
//                     tourItemMapper.upsertPlace(item);
//                 }

//                 // 3. 페이징 검증
//                 if (itemList.size() < numOfRows) {
//                     hasNext = false;
//                 } else {
//                     pageNo++;
//                 }

//             } catch (Exception e) {
//                 log.error("[Batch] {} 페이지 수집 중 에러 발생 - 파이프라인 일시 중단", pageNo, e);
//                 hasNext = false; 
//             }
//         }
        
//         log.info("=== [Batch] 공공데이터 수집 및 PLACE 적재 완료 ===");
//     }

//     /**
//      * 🔄 1:1 상세 API 연쇄 호출로 만능 그릇의 살을 채우는 메서드
//      */
//     private void enrichTourItemDetails(TourItemDTO masterItem) {
//         String contentId = masterItem.getContentid();

//         try {
//             // 상세 공통 정보 조회 (/detailCommon2) ➔ description(overview), address(addr1) 등 보정
//             TourItemDTO commonDetail = tourApiClient.fetchDetailCommon(contentId);
//             if (commonDetail != null) {
//                 masterItem.setOverview(commonDetail.getOverview());
//                 if (masterItem.getTel() == null || masterItem.getTel().isBlank()) {
//                     masterItem.setTel(commonDetail.getTel());
//                 }
//             }

//             // 반려동물 동반 정보 조회 (/detailPetTour2) ➔ 만능 그릇의 반려동물 필드 채우기
//             TourItemDTO petDetail = tourApiClient.fetchDetailPetTour(contentId);
//             if (petDetail != null) {
//                 masterItem.setAcmpyPsblCpam(petDetail.getAcmpyPsblCpam());
//                 masterItem.setRelaRntlPrdlst(petDetail.getRelaRntlPrdlst());
//                 masterItem.setAcmpyNeedMtr(petDetail.getAcmpyNeedMtr());
//                 masterItem.setRelaFrnshPrdlst(petDetail.getRelaFrnshPrdlst());
//                 masterItem.setEtcAcmpyInfo(petDetail.getEtcAcmpyInfo());
//                 masterItem.setRelaAcdntRiskMtr(petDetail.getRelaAcdntRiskMtr());
//                 masterItem.setAcmpyTypeCd(petDetail.getAcmpyTypeCd());
//                 masterItem.setRelaPosesFclty(petDetail.getRelaPosesFclty());
//                 masterItem.setPetTursmInfo(petDetail.getPetTursmInfo());
//             }

//         } catch (Exception e) {
//             log.warn("[Batch] 상세 정보 연쇄 호출 실패 - contentId: {}, 사유: {}", contentId, e.getMessage());
//         }
//     }
// }
