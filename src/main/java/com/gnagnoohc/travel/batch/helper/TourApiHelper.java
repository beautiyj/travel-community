package com.gnagnoohc.travel.batch.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiHelper {
    private final TourApiClient tourApiClient;
    private final ObjectMapper objectMapper;

    // TODO: 0731 minPrice null이면 적재xx 필터링하되, useFeeInfo가 있으면서 minPrice가 숫자필터링되지 않고 null인 경우만 minPrice #가격변동 으로 일괄처리 변경할 것 (대신 placeType="food"인 경우, minprice=null이어도 허용하기

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
                    // 0803 추가: 나머지 반려동물 동반 필드도 복사 - extractExtraInfo에서 활용하기 위함
                    masterItem.setAcmpyNeedMtr(petDetail.getAcmpyNeedMtr());
                    masterItem.setEtcAcmpyInfo(petDetail.getEtcAcmpyInfo());
                    masterItem.setAcmpyTypeCd(petDetail.getAcmpyTypeCd());
                    masterItem.setRelaRntlPrdlst(petDetail.getRelaRntlPrdlst());
                    masterItem.setRelaFrnshPrdlst(petDetail.getRelaFrnshPrdlst());
                    masterItem.setRelaPosesFclty(petDetail.getRelaPosesFclty());
                    masterItem.setRelaAcdntRiskMtr(petDetail.getRelaAcdntRiskMtr());
                }
            }
        } catch (Exception e) {
            log.warn("[Batch] 상세 정보 연쇄 호출 실패 - contentId: {}, 사유: {}", contentId, e.getMessage());
        }
    }

    /* 공통 헬퍼 메소드 - 법정동 시도/시군구 코드를 조합하여 DB의 region_id(Long PK)를 생성
     - 법정동코드조회 TourLdongCodeDTO 메타데이터와 실제 동기화 로직의 areaBasedSyncList2 필드 공통 헬퍼용 메소드
     - regnCd 시도코드 signguCd 시군구코드
     - 공공데이터에서 넘겨받은 코드값이 null/공백인 경우 해당 데이터 적재 안하고 스킵
     - Y(전체목록조회) 및 N(단일조회) 응답 스펙을 모두 안전하게 분기 처리하기 위한 전처리 작업
     - 예: 시도코드("11") + 시군구코드("110") -> "11110" -> Long 11110L 변환
     */
    public Integer parseRegionId(String regnCd, String signguCd) {
        // 시도 코드가 없는 경우 유효하지 않은 데이터로 판단하여 null 반환
        if (!StringUtils.hasText(regnCd)) { return null; }
        // 시도 코드 + (존재할 경우) 시군구 코드를 결합하여 rawCode 생성 : regionId 전체 pk 만들기 로직 (시도코드+시군구코드 혹은 시도코드 only)
        String rawCode = regnCd + (StringUtils.hasText(signguCd) ? signguCd : "");
        // 결합된 코드의 유효성 검사 (null, 빈 값, 공백 문자열인지 2차 검증 작업 필요)
        if (!StringUtils.hasText(rawCode)) { return null; }

        // 문자열 코드를 DB PK 용 Integer 타입으로 파싱 (숫자 변환 실패 시 안전하게 null)
        try {
            return Integer.parseInt(rawCode);
        } catch (NumberFormatException e) {
            log.warn("[Batch] 법정동 코드 숫자 변환 실패 - rawCode: {}", rawCode);
            return null;
        }
    }

    // contentTypeId 코드 형태 -> 문자열 형태 변환
    public String convertContentType(String contentTypeId, String lclsSystm2, String lclsSystm3) {
        if (contentTypeId == null) return "tour";
        if ("AC05".equals(lclsSystm2)) { return "stay"; }
        if ("VE050200".equals(lclsSystm3)) { return "stay"; }

        return switch (contentTypeId) {
            case "32"             -> "stay";
            case "39"             -> "food";
            case "12", "14", "28" -> "tour";
            default               -> "tour";
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

    // 헬퍼 메서드 - processSinglePlace 사용 용도의 이미지 정보(/detailImage2) 조회
    public List<TourDetailImageDTO> fetchDetailImages(String contentId) {
        try {
            // imageYN="Y"를 인자로 전달하여 해당 장소의 원본 및 썸네일 이미지 목록 조회
            String imageJson = tourApiClient.fetchDetailImage(contentId, "Y");
            if (StringUtils.hasText(imageJson)) {
                TourApiResponseDTO<TourDetailImageDTO> imageResponse = objectMapper.readValue(
                        imageJson, new TypeReference<TourApiResponseDTO<TourDetailImageDTO>>() {}
                );
                if (imageResponse != null && imageResponse.getResponse() != null
                        && imageResponse.getResponse().getBody() != null
                        && imageResponse.getResponse().getBody().getItems() != null
                        && !imageResponse.getResponse().getBody().getItems().getItem().isEmpty()) {
                    return imageResponse.getResponse().getBody().getItems().getItem();
                }
            }
        } catch (Exception e) {
            log.warn("[Batch] detailImage2 호출 실패 - contentId: {}", contentId);
        }
        return List.of();
    }

    /* 헬퍼 메소드 isValidItem - 수집 대상 유효성 검증 (대표 이미지 필수 존재 여부 체크)
       대표 이미지가 없으면 아예 적재x 이미지 존재 여부만 검증 */
    public boolean isValidItem(TourAreaBasedSyncListDTO item) {
        // 기존 대표 이미지 체크
        if (!StringUtils.hasText(item.getFirstimage())) {
            log.info("[Batch Skip] 대표 이미지가 없어 수집 제외 - contentId: {}, title: {}", item.getContentid(), item.getTitle());
            return false;
        }

        // 지역 코드(시도 또는 시군구)가 비어있는 부실 데이터 스킵
        if (!StringUtils.hasText(item.getLDongRegnCd()) || !StringUtils.hasText(item.getLDongSignguCd())) {
            log.info("[Batch Skip] 법정동 지역 코드가 없어 수집 제외 - contentId: {}, title: {}", item.getContentid(), item.getTitle());
            return false;
        }

        return true;
    }
    /* TODO: 0730 부가정보(휴무일, 영업시간, 주차, 문의처 등) 컬럼 추가 시 - 정제 헬퍼 메소드
           - 타입별(tour, stay, food)로 제공되는 필드가 다르므로 분기하여 단일 문장으로 가공
           - HTML 태그 제거 및 공백 정돈 처리 적용
           - 0730 추가: TourDetailInfoDTO(반복정보 리스트) / TourItemDTO(반려동물 동반 데이터) 통합
           - 각 항목은 프론트 <ul><li> 목록 렌더링을 위해 개행(\n)으로 구분
         */
    public String extractExtraInfo(TourDetailIntroDTO introDetail,
                                   List<TourDetailInfoDTO> infoList,
                                   TourItemDTO itemDTO,
                                   String placeType) {
        List<String> infoParts = new ArrayList<>();

        // HTML 태그 제거 및 공백 정리용 내부 람다 함수
        java.util.function.Function<String, String> cleanText = text ->
                StringUtils.hasText(text) ? text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim() : null;

        // 1. TourDetailIntroDTO - 소개정보(타입별 분기)
        if (introDetail != null) {
            if ("stay".equals(placeType)) {
                // [숙박 32] 입/퇴실 시간, 주차, 문의처
                String checkin = cleanText.apply(introDetail.getCheckintime());
                String checkout = cleanText.apply(introDetail.getCheckouttime());
                if (checkin != null || checkout != null) {
                    infoParts.add("[입/퇴실] " + (checkin != null ? checkin : "") + (checkout != null ? " / " + checkout : ""));
                }
                String parking = cleanText.apply(introDetail.getParkinglodging());
                if (parking != null) infoParts.add("[주차] " + parking);
                String info = cleanText.apply(introDetail.getInfocenterlodging());
                if (info != null) infoParts.add("[문의] " + info);
                // 0803 추가: 수용인원, 객실수, 객실유형, 식음료장, 부대시설, 예약안내
                String accomCount = cleanText.apply(introDetail.getAccomcountlodging());
                if (accomCount != null) infoParts.add("[수용인원] " + accomCount);
                String roomCount = cleanText.apply(introDetail.getRoomcount());
                if (roomCount != null) infoParts.add("[객실수] " + roomCount);
                String roomType = cleanText.apply(introDetail.getRoomtype());
                if (roomType != null) infoParts.add("[객실유형] " + roomType);
                String foodPlace = cleanText.apply(introDetail.getFoodplace());
                if (foodPlace != null) infoParts.add("[식음료장] " + foodPlace);
                String subFacility = cleanText.apply(introDetail.getSubfacility());
                if (subFacility != null) infoParts.add("[부대시설] " + subFacility);
                String reservation = cleanText.apply(introDetail.getReservationlodging());
                if (reservation != null) infoParts.add("[예약안내] " + reservation);

            } else if ("food".equals(placeType)) {
                // [음식점 39] 영업시간, 쉬는날, 주차, 문의처
                String opentime = cleanText.apply(introDetail.getOpentimefood());
                if (opentime != null) infoParts.add("[영업시간] " + opentime);
                String rest = cleanText.apply(introDetail.getRestdatefood());
                if (rest != null) infoParts.add("[휴무일] " + rest);
                String parking = cleanText.apply(introDetail.getParkingfood());
                if (parking != null) infoParts.add("[주차] " + parking);
                String info = cleanText.apply(introDetail.getInfocenterfood());
                if (info != null) infoParts.add("[문의] " + info);
                // 0803 추가: 대표메뉴, 취급메뉴, 좌석수, 예약안내, 포장가능, 신용카드, 할인정보, 어린이놀이방, 개업일
                String firstMenu = cleanText.apply(introDetail.getFirstmenu());
                if (firstMenu != null) infoParts.add("[대표메뉴] " + firstMenu);
                String treatMenu = cleanText.apply(introDetail.getTreatmenu());
                if (treatMenu != null) infoParts.add("[취급메뉴] " + treatMenu);
                String seat = cleanText.apply(introDetail.getSeat());
                if (seat != null) infoParts.add("[좌석수] " + seat);
                String reservationFood = cleanText.apply(introDetail.getReservationfood());
                if (reservationFood != null) infoParts.add("[예약안내] " + reservationFood);
                String packing = cleanText.apply(introDetail.getPacking());
                if (packing != null) infoParts.add("[포장가능] " + packing);
                String creditCardFood = cleanText.apply(introDetail.getChkcreditcardfood());
                if (creditCardFood != null) infoParts.add("[신용카드] " + creditCardFood);
                String discountFood = cleanText.apply(introDetail.getDiscountinfofood());
                if (discountFood != null) infoParts.add("[할인정보] " + discountFood);
                String kidsFacility = cleanText.apply(introDetail.getKidsfacility());
                if (kidsFacility != null) infoParts.add("[어린이놀이방] " + kidsFacility);
                String opendateFood = cleanText.apply(introDetail.getOpendatefood());
                if (opendateFood != null) infoParts.add("[개업일] " + opendateFood);

            } else if ("tour".equals(placeType)) {
                // [관광지/문화시설/레포츠 12, 14, 28] 쉬는날, 이용시간, 주차, 문의처
                String rest = cleanText.apply(firstHasText(introDetail.getRestdate(), introDetail.getRestdateculture(), introDetail.getRestdateleports()));
                if (rest != null) infoParts.add("[휴무일] " + rest);
                String usetime = cleanText.apply(firstHasText(introDetail.getUsetime(), introDetail.getUsetimeculture(), introDetail.getUsetimeleports()));
                if (usetime != null) infoParts.add("[이용시간] " + usetime);
                String parking = cleanText.apply(firstHasText(introDetail.getParking(), introDetail.getParkingculture(), introDetail.getParkingleports()));
                if (parking != null) infoParts.add("[주차] " + parking);
                String info = cleanText.apply(firstHasText(introDetail.getInfocenter(), introDetail.getInfocenterculture(), introDetail.getInfocenterleports()));
                if (info != null) infoParts.add("[문의] " + info);
                // 0803 추가: 수용인원, 유모차대여, 신용카드, 애완동물동반(공통) + 타입별 전용 필드(이용시기/개장일/개장기간/할인정보/관람소요시간/예약안내/규모)
                String accomCount = cleanText.apply(firstHasText(introDetail.getAccomcount(), introDetail.getAccomcountculture(), introDetail.getAccomcountleports()));
                if (accomCount != null) infoParts.add("[수용인원] " + accomCount);
                String babyCarriage = cleanText.apply(firstHasText(introDetail.getChkbabycarriage(), introDetail.getChkbabycarriageculture(), introDetail.getChkbabycarriageleports()));
                if (babyCarriage != null) infoParts.add("[유모차대여] " + babyCarriage);
                String creditCard = cleanText.apply(firstHasText(introDetail.getChkcreditcard(), introDetail.getChkcreditcardculture(), introDetail.getChkcreditcardleports()));
                if (creditCard != null) infoParts.add("[신용카드] " + creditCard);
                String pet = cleanText.apply(firstHasText(introDetail.getChkpet(), introDetail.getChkpetculture(), introDetail.getChkpetleports()));
                if (pet != null) infoParts.add("[애완동물동반] " + pet);
                // [관광지 12 전용] 이용시기, 개장일
                String useSeason = cleanText.apply(introDetail.getUseseason());
                if (useSeason != null) infoParts.add("[이용시기] " + useSeason);
                String openDate = cleanText.apply(introDetail.getOpendate());
                if (openDate != null) infoParts.add("[개장일] " + openDate);
                // [문화시설 14 전용] 할인정보, 관람소요시간
                String discount = cleanText.apply(introDetail.getDiscountinfo());
                if (discount != null) infoParts.add("[할인정보] " + discount);
                String spendTime = cleanText.apply(introDetail.getSpendtime());
                if (spendTime != null) infoParts.add("[관람소요시간] " + spendTime);
                // [레포츠 28 전용] 개장기간, 예약안내, 규모
                String openPeriod = cleanText.apply(introDetail.getOpenperiod());
                if (openPeriod != null) infoParts.add("[개장기간] " + openPeriod);
                String reservation = cleanText.apply(introDetail.getReservation());
                if (reservation != null) infoParts.add("[예약안내] " + reservation);
                String scale = cleanText.apply(introDetail.getScaleleports());
                if (scale != null) infoParts.add("[규모] " + scale);
            }
        }

        // 2. TourDetailInfoDTO - 반복정보(infoname/infotext 쌍) 리스트
        if (infoList != null) {
            for (TourDetailInfoDTO info : infoList) {
                String name = cleanText.apply(info.getInfoname());
                String text = cleanText.apply(info.getInfotext());
                if (name != null && text != null) {
                    infoParts.add("[" + name + "] " + text);
                }
            }
        }

        // 3. TourItemDTO - 반려동물 동반 데이터
        if (itemDTO != null) {
            String acmpy = cleanText.apply(itemDTO.getAcmpyPsblCpam());
            if (acmpy != null) infoParts.add("[동반가능동물] " + acmpy);
            String petInfo = cleanText.apply(itemDTO.getPetTursmInfo());
            if (petInfo != null) infoParts.add("[반려동물 관광정보] " + petInfo);
            String needMtr = cleanText.apply(itemDTO.getAcmpyNeedMtr());
            if (needMtr != null) infoParts.add("[동반시 필요사항] " + needMtr);
            String etcInfo = cleanText.apply(itemDTO.getEtcAcmpyInfo());
            if (etcInfo != null) infoParts.add("[기타 동반 정보] " + etcInfo);
            // 0803 추가: 동반유형, 관련 렌탈 품목, 관련 비치 품목, 관련 구비 시설, 관련 사고 대비사항
            String acmpyType = cleanText.apply(itemDTO.getAcmpyTypeCd());
            if (acmpyType != null) infoParts.add("[동반유형] " + acmpyType);
            String rentalPrdlst = cleanText.apply(itemDTO.getRelaRntlPrdlst());
            if (rentalPrdlst != null) infoParts.add("[관련 렌탈 품목] " + rentalPrdlst);
            String frnshPrdlst = cleanText.apply(itemDTO.getRelaFrnshPrdlst());
            if (frnshPrdlst != null) infoParts.add("[관련 비치 품목] " + frnshPrdlst);
            String posesFclty = cleanText.apply(itemDTO.getRelaPosesFclty());
            if (posesFclty != null) infoParts.add("[관련 구비 시설] " + posesFclty);
            String acdntRiskMtr = cleanText.apply(itemDTO.getRelaAcdntRiskMtr());
            if (acdntRiskMtr != null) infoParts.add("[관련 사고 대비사항] " + acdntRiskMtr);
        }

        if (infoParts.isEmpty()) return null;
        return String.join("\n", infoParts);
    }

    // 우선순위에 따라 유효한 첫 번째 문자열 반환 헬퍼
    private String firstHasText(String... values) {
        for (String val : values) {
            if (StringUtils.hasText(val)) return val;
        }
        return null;
    }

}