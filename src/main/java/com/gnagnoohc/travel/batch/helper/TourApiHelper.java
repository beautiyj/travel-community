package com.gnagnoohc.travel.batch.helper;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.dto.*;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiHelper {
    private final TourApiClient tourApiClient;
    private final ObjectMapper objectMapper;

    // TODO: 이후 useFeeInfo, minPrice 처리에서 0원 혹은 가격없음 -> TOUR 서비스단에서 무료 & 가격변동으로 텍스트 매핑처리
    // TODO: STAY/TOUR 일부는 금액 넘어옴 - 금액처리/무료/가격변동 처리, FOOD는 금액 없음 - 가격변동으로 처리 필요
    public void enrichTourItemDetails(TourItemDTO masterItem) {
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

    // TODO: 0728 18:00 테스트로직 이후 헬퍼 메소드 수정 - 법정동 조회 로직 Y/N 선택 방향 및 배치 서비스단에서 변환 처리 일괄로

    /* 공통 헬퍼 메소드 - 법정동 시도/시군구 코드를 조합하여 DB의 region_id(Long PK)를 생성
     - 법정동코드조회 TourLdongCodeDTO 메타데이터와 실제 동기화 로직의 areaBasedSyncList2 필드 공통 헬퍼용 메소드
     - regnCd 시도코드 signguCd 시군구코드
     - 공공데이터에서 넘겨받은 코드값이 null/공백인 경우 해당 데이터 적재 안하고 스킵
     - Y(전체목록조회) 및 N(단일조회) 응답 스펙을 모두 안전하게 분기 처리하기 위한 전처리 작업
     - 예: 시도코드("11") + 시군구코드("110") -> "11110" -> Long 11110L 변환
     */
    public Long parseRegionId(String regnCd, String signguCd) {
        // 시도 코드가 없는 경우 유효하지 않은 데이터로 판단하여 null 반환
        if (!StringUtils.hasText(regnCd)) { return null; }
        // 시도 코드 + (존재할 경우) 시군구 코드를 결합하여 rawCode 생성 : regionId 전체 pk 만들기 로직 (시도코드+시군구코드 혹은 시도코드 only)
        String rawCode = regnCd + (StringUtils.hasText(signguCd) ? signguCd : "");
        // 결합된 코드의 유효성 검사 (null, 빈 값, 공백 문자열인지 2차 검증 작업 필요)
        if (!StringUtils.hasText(rawCode)) { return null; }

        // 문자열 코드를 DB PK 용 Long 타입으로 파싱 (숫자 변환 실패 시 안전하게 null)
        try {
            return Long.parseLong(rawCode);
        } catch (NumberFormatException e) {
            log.warn("[Batch] 법정동 코드 숫자 변환 실패 - rawCode: {}", rawCode);
            return null;
        }
    }

    // contentTypeId 코드 형태 -> 문자열 형태 변환
    public String convertContentType(String contentTypeId) {
        if (contentTypeId == null) return "tour";
        return switch (contentTypeId) {
            case "32"             -> "stay";
            case "39"             -> "food";
            case "12", "14", "28" -> "tour"; // 관광지(12), 문화시설(14), 레포츠(28)
            default               -> "tour"; // contentTypeId 선별 처리작업은 완료했지만 예외 방어용 기본값 설정해두기
        };
    }

    /* 원문 텍스트에서 검색/정렬용 최저가(숫자) 추출 헬퍼 메소드
       extractFeeInfo처리로 들어온 숫자를 최종 가공해서 보여주는 실 금액처리 헬퍼 */
    public Integer parseMinPrice(String rawFeeInfo) {
        // 가공된 금액 넘어온 게 없으면 null
        if (!StringUtils.hasText(rawFeeInfo)) { return null; }
        // "무료", "무상", "무료입장" 등의 키워드가 포함된 경우 0원 처리
        if (rawFeeInfo.contains("무료") || rawFeeInfo.contains("무상")) { return 0; }

        // 숫자 이외의 문자 제거 등등의 전처리 -> 숫자 중에서도 최저가 파싱 (예: "성인 10,000원 / 어린이 5,000원" -> 5000)
        try {
            // 콤마 제거 전처리 (예: "\10,000" -> "\10000", "5,000원" -> "5000원")
            String cleanText = rawFeeInfo.replaceAll(",", "");

            // [패턴 A] 숫자 뒤에 '원'이 붙은 경우 (예: "10000원", "5000 원")
            // [패턴 B] 숫자 앞에 '\' 또는 '₩'가 붙은 경우 (예: "\10000", "₩5000")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:[\\\\₩]\\s*(\\d+)|(\\d+)\\s*원)");
            java.util.regex.Matcher matcher = pattern.matcher(cleanText);

            List<Integer> prices = new ArrayList<>();

            while (matcher.find()) {
                // 패턴 A(숫자+원)는 group(2), 패턴 B(원화기호+숫자)는 group(1)에 캡처됨
                String numStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (numStr != null) {
                    long parsedLong = Long.parseLong(numStr);
                    // 비정상 금액 범위 필터링 (100원 미만 잡음 / 1,000만 원 초과 방어)
                    if (parsedLong >= 100 && parsedLong <= 10_000_000) {
                        prices.add((int) parsedLong);
                    }
                }
            }
            // 추출된 진짜 금액 리스트 중 최저가 반환
            if (!prices.isEmpty()) {
                return java.util.Collections.min(prices);
            }
        } catch (NumberFormatException e) {
            log.warn("[Batch Fee Parsing Warning] 요금 숫자 변환 중 최저가 파싱 실패 - rawFeeInfo: {}", rawFeeInfo);
            return null;
        }
        return null;
    }

    /* 관광 타입별 DTO에서 요금 안내 원문 텍스트(useFeeInfo) 추출 헬퍼 메소드
       TourDetailInfoDTO - stay(roomoffseasonminfee1) / TourDetailIntroDTO - tour(usefeeleports, usefee) */
    public String extractFeeInfo(TourDetailIntroDTO introDetail, TourDetailInfoDTO infoDetail, String placeType) {
        // stay 타입: 반복정보 DTO(TourDetailInfoDTO)의 비수기 주중 요금 우선 활용
        if ("stay".equals(placeType) && infoDetail != null) {
            if (StringUtils.hasText(infoDetail.getRoomoffseasonminfee1())) {
                return infoDetail.getRoomoffseasonminfee1() + "원~";
            }
        }
        // tour 관광지/레포츠/문화시설 등 타입: 소개정보 DTO(TourDetailIntroDTO) 활용
        if (introDetail != null) {
            if (StringUtils.hasText(introDetail.getUsefeeleports())) { return introDetail.getUsefeeleports(); }
            if (StringUtils.hasText(introDetail.getUsefee())) { return introDetail.getUsefee(); }
        }
        // 음식점(39) 등 가격 정보 필드가 제공되지 않는 타입은 null 반환 (서비스단에서 '가격 변동/미제공' 처리)
        return null;
    }

    // 헬퍼 메서드 - processSinglePlace 사용 용도의 소개정보(/detailIntro2) 조회
    public TourDetailIntroDTO fetchDetailIntro(String contentId, String contentTypeId) {
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

    // 헬퍼 메서드 - processSinglePlace 사용 용도의 반복정보(/detailInfo2) 조회
    public TourDetailInfoDTO fetchDetailInfo(String contentId, String contentTypeId) {
        try {
            String infoJson = tourApiClient.fetchDetailInfo(contentId, contentTypeId);
            if (StringUtils.hasText(infoJson)) {
                TourApiResponseDTO<TourDetailInfoDTO> infoResponse = objectMapper.readValue(
                        infoJson, new TypeReference<TourApiResponseDTO<TourDetailInfoDTO>>() {}
                );
                if (infoResponse != null && infoResponse.getResponse() != null
                        && infoResponse.getResponse().getBody() != null
                        && infoResponse.getResponse().getBody().getItems() != null
                        && !infoResponse.getResponse().getBody().getItems().getItem().isEmpty()) {
                    return infoResponse.getResponse().getBody().getItems().getItem().get(0);
                }
            }
        } catch (Exception e) {
            log.warn("[Batch] detailInfo2 호출 실패 - contentId: {}", contentId);
        }
        return null;
    }

    /* 헬퍼 메소드 isValidItem - 수집 대상 유효성 검증 (대표 이미지 필수 존재 여부 체크)
       대표 이미지가 없으면 아예 적재x 이미지 존재 여부만 검증 -> 이후 PlaceImage테이블에 적용 필요
     */
    public boolean isValidItem(TourAreaBasedSyncListDTO syncItem) {
        if (syncItem == null) return false;

        if (!StringUtils.hasText(syncItem.getFirstimage())) {
            log.info("[Batch Skip] 대표 이미지가 없어 수집 제외 - contentId: {}, title: {}",
                    syncItem.getContentid(), syncItem.getTitle());
            return false;
        }
        return true;
    }
}