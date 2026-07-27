package com.gnagnoohc.travel.batch.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.batch.client.TourApiClient;
import com.gnagnoohc.travel.batch.dto.*;
import com.gnagnoohc.travel.batch.dto.TourApiResponseDTO.Header;
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

    // contentTypeId 5가지만 선별하여 데이터 가져오기
    private static final List<String> TARGET_CONTENT_TYPES = List.of(
            "12", // tour 관광지
            "14", // tour 문화시설
            "28", // tour 레포츠
            "32", // stay 숙박
            "39"  // food 음식점
    );

    // 공공데이터 전용 가상 비즈니스 회원 PK (시스템 유령계정)
    private static final Long PUBLIC_DATA_MEMBER_ID = 1L;

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
                    RegionDTO regionDto = convertToRegionDTO(dto);
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
                    if (!isValidItem(syncItem)) { continue; }
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

    // TourLdongCodeDTO -> RegionDTO 변환
    private RegionDTO convertToRegionDTO(TourLdongCodeDTO ldongCodeDTO) {
        // 공통 헬퍼 parseRegionId를 통해 regionId(Long) 추출 (Y/N 응답 필드 분기 처리)
        // regnCd는 y일 때 lDongRegnCd 시도코드 n일때 일반 code (시군구코드는 자동으로 y일 때만 들어오는 응답)
        String regnCd = StringUtils.hasText(ldongCodeDTO.getLDongRegnCd()) ? ldongCodeDTO.getLDongRegnCd() : ldongCodeDTO.getCode();
        String signguCd = ldongCodeDTO.getLDongSignguCd();

        Long regionId = parseRegionId(regnCd, signguCd);
        if (regionId == null) return null;

        // 법정동 명칭 시도명/시군구명 - 법정동 명칭은 "서울시종로구"가 아닌 "서울시 종로구"와 같이 공백(띄어쓰기)을 포함하도록 가공 처리
        // 법정동 명칭은 동기화 로직에 없고 메타데이터인 법정동 목록 조회에만 존재
        // rawName는 y일 때 lDongSignguCd 시도코드 n일때 일반 name (시군구명칭은 자동으로 y일 때만 들어오는 응답)
        String rawName = StringUtils.hasText(ldongCodeDTO.getLDongRegnNm()) ? ldongCodeDTO.getLDongRegnNm() : ldongCodeDTO.getName();
        if (StringUtils.hasText(ldongCodeDTO.getLDongSignguNm())) { rawName += " " + ldongCodeDTO.getLDongSignguNm(); }

        /* 상위 지역(시/도)과 하위 지역(시/군/구)의 계층 구조(부모-자식 관계) 설정
        코드 길이가 5자리 이상(시/군/구 데이터)일 때만 부모 ID 파싱하기
        - 2자리(시/도 단독, 예: "11" 서울): 최상위 지역이므로 부모가 없음 -> parentRegionId = null
        - 5자리 이상(시/군/구 결합, 예: "11110" 종로구): 앞 2자리("11")를 추출하여 부모 시/도 PK 세팅 -> parentRegionId = 11L */
        String rawCodeStr = regionId.toString();
        Long parentRegionId = (rawCodeStr.length() >= 5) ? Long.parseLong(rawCodeStr.substring(0, 2)) : null;

        // 최종 RegionDTO 빌드 및 적재
        return RegionDTO.builder()
                .regionId(regionId)
                .regionName(rawName)
                .parentRegionId(parentRegionId)
                .build();
    }

    // TODO: 이후 useFeeInfo, minPrice 처리에서 0원 혹은 가격없음 -> TOUR 서비스단에서 무료 & 가격변동으로 텍스트 매핑처리
    // TODO: PlaceImage 컨버터 처리에서, 기본 썸네일 값은 convertToPlaceDTO쪽으로 전달 필요
    // TourLclsSystmCodeDTO -> PlaceDTO 변환
    // 메타데이터인 법정동코드가 아닌, 실제정보가 필요한 동기화 API TourAreaBasedSyncListDTO를 플레이스에 넣어야 함
    private PlaceDTO convertToPlaceDTO(TourAreaBasedSyncListDTO syncItem, TourItemDTO tourItem, TourDetailIntroDTO introDetail, TourDetailInfoDTO infoDetail) {
        // 공공데이터의 contentId -> Place테이블엔 pk로 기입, 더미데이터의 경우 난수처리하여 넣을 것.
        Long placeId = Long.parseLong(syncItem.getContentid());
        // 공통헬퍼 메소드 parseRegionId 사용하여 동기화 로직의 법정동 시도코드/시군구코드 처리
        Long regionId = parseRegionId(syncItem.getLDongRegnCd(), syncItem.getLDongSignguCd());
        // 플레이스 타입은 숫자로 들어오는 걸 convertContentType에서 tour/food/stay로 변환 처리
        String placeType = convertContentType(syncItem.getContenttypeid());
        // 주소는 addr1 + addr2 합친 전체주소 하나로 처리
        String fullAddress = syncItem.getAddr1() + 
                (StringUtils.hasText(syncItem.getAddr2()) ? " " + syncItem.getAddr2() : "");
        BigDecimal mapx = StringUtils.hasText(syncItem.getMapx()) ? new BigDecimal(syncItem.getMapx()) : null;
        BigDecimal mapy = StringUtils.hasText(syncItem.getMapy()) ? new BigDecimal(syncItem.getMapy()) : null;
        // 헬퍼 메소드 extractFeeInfo - 타입별 DTO에서 요금 원문 안내 텍스트(useFeeInfo) 추출
        String useFeeInfo = extractFeeInfo(introDetail, infoDetail, placeType);
        // 헬퍼 메소드 parseMinPrice - extractFeeInfo에서 추출한 원문 텍스트를 전달하여 검색/정렬용 최저가 숫자(minPrice) 파싱
        Integer minPrice = parseMinPrice(useFeeInfo);
        // 카드형 썸네일 대표 이미지 결정: 목록 API의 썸네일(firstimage2) 우선, 없을 경우 원본(firstimage) Fallback 사용
        String thumbnailImage = StringUtils.hasText(syncItem.getFirstimage2())
                ? syncItem.getFirstimage2()
                : syncItem.getFirstimage();
        // 해시태그는 generateHashtags에서 처리 (TourItemDTO + TourDetailIntroDTO 조합으로 해시태그 생성)
        String hashtags = generateHashtags(tourItem, introDetail, placeType);

        return PlaceDTO.builder()
                .placeId(placeId)
                .regionId(regionId)
                .memberId(PUBLIC_DATA_MEMBER_ID)
                .placeType(placeType)
                .name(syncItem.getTitle())
                .description(tourItem.getOverview())
                .address(fullAddress)
                .mapx(mapx)
                .mapy(mapy)
                .useFeeInfo(useFeeInfo)
                .minPrice(minPrice)
                .isClosed("0".equals(syncItem.getShowflag()))
                .firstImage(thumbnailImage) // 조인 없는 카드 리스트용 1차 썸네일 세팅
                .hashtags(hashtags)
                .build();
    }

    // TODO: footer에 TourDetailImageDTO - cpyrhtDivCd (저작권표기) 추가 필요 & 프론트에서 Type3의 경우 비율유지하며 적용 필요
    // TourDetailImageDTO -> PlaceImageDTO 변환
    // 대표 이미지 등록 시 sortOrder=0 지정, 상세/서브 이미지 등록 시 순번(sortOrder) 지정
    private PlaceImageDTO convertToPlaceImageDTO(Long placeId, String imageUrl, int sortOrder) {
        if (!StringUtils.hasText(imageUrl) || placeId == null) {
            return null;
        }
        return PlaceImageDTO.builder()
                .placeId(placeId)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }

    // TourAreaBasedSyncListDTO 동기화 목록에서 가져온 장소 하나의 기본 정보를
    // 일부 공통 정보가 담긴 TourItemDTO에서 1차 처리 -> 최종 컨버터로는 TourItemDTO만 전달하는 방식
    private TourItemDTO convertToTourItemDTO(TourAreaBasedSyncListDTO syncItem) {
        TourItemDTO item = new TourItemDTO();
        item.setContentid(syncItem.getContentid());
        item.setContenttypeid(syncItem.getContenttypeid());
        item.setCreatedtime(syncItem.getCreatedtime());
        item.setModifiedtime(syncItem.getModifiedtime());
        item.setTitle(syncItem.getTitle());
        // TODO: 이후 지도 API 활용 시 메모리에서 dist 꺼내 쓰는 방식 / dist는 특정 좌표(사용자 위치) 기준 상대적인 거리라서 기본 PlaceDTO엔 넣을 필요 없음
        // dist 처리 (문자열로 들어올 경우 Double로 파싱, 없으면 null)
        if (StringUtils.hasText(syncItem.getDist())) {
            try { item.setDist(Double.parseDouble(syncItem.getDist())); }
            catch (NumberFormatException e) { item.setDist(null); }
        }
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
        item.setLDongRegnCd(syncItem.getLDongRegnCd());
        item.setLDongSignguCd(syncItem.getLDongSignguCd());
        item.setLclsSystm1(syncItem.getLclsSystm1());
        item.setLclsSystm2(syncItem.getLclsSystm2());
        item.setLclsSystm3(syncItem.getLclsSystm3());
        return item;
    }

    // TODO: TourItemDTO의 overview는 각 조건성 정보를 병합하는 enrichTourItemDetails에서 처리하고 있으나 그 외 응답DTO 재확인하여 파라미터 추가할 거 있는지 확인 필요
    private void enrichTourItemDetails(TourItemDTO masterItem) {
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


    /* 공통 헬퍼 메소드 - 법정동 시도/시군구 코드를 조합하여 DB의 region_id(Long PK)를 생성
     - 법정동코드조회 TourLdongCodeDTO 메타데이터와 실제 동기화 로직의 areaBasedSyncList2 필드 공통 헬퍼용 메소드
     - regnCd 시도코드 signguCd 시군구코드
     - 공공데이터에서 넘겨받은 코드값이 null/공백인 경우 해당 데이터 적재 안하고 스킵
     - Y(전체목록조회) 및 N(단일조회) 응답 스펙을 모두 안전하게 분기 처리하기 위한 전처리 작업
     - 예: 시도코드("11") + 시군구코드("110") -> "11110" -> Long 11110L 변환
     */
    private Long parseRegionId(String regnCd, String signguCd) {
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
    private String convertContentType(String contentTypeId) {
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
    private Integer parseMinPrice(String rawFeeInfo) {
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
    private String extractFeeInfo(TourDetailIntroDTO introDetail, TourDetailInfoDTO infoDetail, String placeType) {
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
    private TourDetailIntroDTO fetchDetailIntro(String contentId, String contentTypeId) {
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
    private TourDetailInfoDTO fetchDetailInfo(String contentId, String contentTypeId) {
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
    private boolean isValidItem(TourAreaBasedSyncListDTO syncItem) {
        if (syncItem == null) return false;

        if (!StringUtils.hasText(syncItem.getFirstimage())) {
            log.info("[Batch Skip] 대표 이미지가 없어 수집 제외 - contentId: {}, title: {}",
                    syncItem.getContentid(), syncItem.getTitle());
            return false;
        }
        return true;
    }

    /* 헬퍼 메소드 - 개별 장소의 상세정보 연쇄 수집 및 DB 적재(PLACE + PLACE_IMAGE)
       소개정보(/detailIntro2) & 반복정보(/detailInfo2) 조회용 헬퍼 각각 호출함 */
    private void processSinglePlace(TourAreaBasedSyncListDTO syncItem) {
        TourItemDTO tourItem = convertToTourItemDTO(syncItem);
        TourDetailIntroDTO introDetail = null;
        TourDetailInfoDTO infoDetail = null;

        // 영업중인 장소 상세정보 연쇄 수집
        if (!"0".equals(syncItem.getShowflag())) {
            enrichTourItemDetails(tourItem);
            // 소개정보(detailIntro2) 연쇄 호출 (관광지/레포츠 이용요금 등)
            introDetail = fetchDetailIntro(syncItem.getContentid(), syncItem.getContenttypeid());
            // 숙박(32) 타입인 경우 반복정보(detailInfo2) 연쇄 호출 (숙박 객실 요금 등)
            if ("32".equals(syncItem.getContenttypeid())) {
                infoDetail = fetchDetailInfo(syncItem.getContentid(), syncItem.getContenttypeid());
            }
        }
        // convertToPlaceDTO -> 서비스 PlaceDTO 변환 및 해시태그 생성
        PlaceDTO placeDto = convertToPlaceDTO(syncItem, tourItem, introDetail, infoDetail);
        tourMapper.upsertPlace(placeDto);

        // 대표 이미지가 있는 경우 PLACE_IMAGE 테이블 INSERT 적재
        if (StringUtils.hasText(syncItem.getFirstimage()) && placeDto.getPlaceId() != null) {
            PlaceImageDTO imageDto = PlaceImageDTO.builder()
                    .placeId(placeDto.getPlaceId())
                    .imageUrl(syncItem.getFirstimage())
                    .sortOrder(0)
                    .build();
            tourMapper.insertPlaceImage(imageDto);
        }
    }

    // generateHashtags 생성 헬퍼 메소드 - TourItemDTO + TourDetailIntroDTO 기반 동적 해시태그 생성
    private String generateHashtags(TourItemDTO item, TourDetailIntroDTO intro, String placeType) {
        List<String> tags = new ArrayList<>();
        if (StringUtils.hasText(placeType)) tags.add("#" + placeType);

        if (intro != null) {
            if ("tour".equals(placeType)) {
                parseTourHashtags(intro, tags);
            } else if ("stay".equals(placeType)) {
                parseStayHashtags(intro, tags);
            } else if ("food".equals(placeType)) {
                parseFoodHashtags(intro, tags);
            }
        } else if ("tour".equals(placeType)) {
            tags.add("#무료");
        }

        if (StringUtils.hasText(item.getAcmpyPsblCpam()) || StringUtils.hasText(item.getPetTursmInfo())) {
            tags.add("#반려동물동반");
        }

        List<String> uniqueTags = tags.stream().filter(StringUtils::hasText).distinct().toList();
        return String.join(",", uniqueTags);
    }

    private void parseTourHashtags(TourDetailIntroDTO intro, List<String> tags) {
        String useFee = intro.getUsefee();
        if (StringUtils.hasText(useFee)) {
            String cleanFee = useFee.replaceAll("<[^>]*>", "").trim();
            if (cleanFee.contains("무료") || cleanFee.contains("없음") || "0".equals(cleanFee)) {
                tags.add("#무료");
            } else {
                String shortFee = cleanFee.length() > 10 ? cleanFee.substring(0, 10) : cleanFee;
                tags.add("#" + shortFee.replaceAll(" ", ""));
            }
        } else {
            tags.add("#무료");
        }
    }

    private void parseStayHashtags(TourDetailIntroDTO intro, List<String> tags) {
        if (StringUtils.hasText(intro.getRoomtype())) {
            tags.add("#" + intro.getRoomtype().replaceAll(" ", ""));
        }
        if (StringUtils.hasText(intro.getRoomcount())) {
            tags.add("#객실" + intro.getRoomcount() + "개");
        }
    }

    private void parseFoodHashtags(TourDetailIntroDTO intro, List<String> tags) {
        String menu = StringUtils.hasText(intro.getFirstmenu()) ? intro.getFirstmenu() : intro.getTreatmenu();
        if (StringUtils.hasText(menu)) {
            String cleanMenu = menu.replaceAll("<[^>]*>", "").trim();
            String[] menuArray = cleanMenu.split("[,/\\n]");
            for (int i = 0; i < Math.min(menuArray.length, 2); i++) {
                String singleMenu = menuArray[i].trim().replaceAll(" ", "");
                if (StringUtils.hasText(singleMenu) && singleMenu.length() <= 12) {
                    tags.add("#" + singleMenu);
                }
            }
        }
    }

}