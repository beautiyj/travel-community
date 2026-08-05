package com.gnagnoohc.travel.batch.validator;

import com.gnagnoohc.travel.batch.converter.HashtagGenerator;
import com.gnagnoohc.travel.batch.dto.TourAreaBasedSyncListDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/*   1차 공공데이터 검증 & 필터링 로직
*  - contentTypeId 필터링. 5가지 코드에 포함되지 않는 경우 필터링 처리
*  - contentTypeId, title, addr1, addr2, firstimage, firstimage2, mapx, mapy, mlevel 필수값 검증
*  - 블랙리스트 키워드는 전부 제거 & 공공데이터 대중소분류 (NOTION-엑셀파일의 1차 필터링 파일 참고) 필터링 처리
*  - 부실데이터 검증용 로직 포함.
*   ㄴ minPrice null이면 적재 제외 (단, placeType="food"는 예외 허용_공공데이터에서 넘어오는 필드값 존재x)
*   ㄴ useFeeInfo(이용요금 텍스트)가 존재하면 적재 허용
* */
@Component
@RequiredArgsConstructor
public class TourValidator {
    private final HashtagGenerator hashtagGenerator;

    // 차단할 블랙리스트 키워드 필터링
    private static final List<String> BLACK_KEYWORDS = List.of(
            "유흥주점", "단란주점", "클럽", "무인", "PC방", "자판기", "휴게소", "노래방", "노래바", "카지노",
            "의원", "병원", "약국", "축제", "팝업", "스토어", "클리닉", "약방", "한의원", "치과", "페스타", "페스티벌"
    );

    // 중분류(cat2) 단위로 전체 허용
    private static final Set<String> ALLOWED_CAT2_FULL = Set.of(
            "EX01", "EX02", "EX03", "EX04",
            "HS01", "HS02", "HS03", "HS04",
            "LS01", "LS02", "LS03", "LS04",
            "NA01", "NA02", "NA03", "NA04",
            "VE02", "VE03", "VE07", "VE10"
    );
    // 소분류(cat3) 단위로 필터링 허용
    private static final Set<String> ALLOWED_CAT3_SPECIFIC = Set.of(
            "EX050100", "EX050200", "EX050300",
            "EX060100",
            "EX070100",
            "VE010200", "VE010300", "VE010800",
            "VE050100",
            "VE090100", "VE090200", "VE090400"
    );

    // 수집 대상 아이템 유효성 최종 검증 메서드
    public boolean isValid(TourAreaBasedSyncListDTO item) {
        if (item == null) return false;

        String contentTypeId = item.getContenttypeid();
        if (!List.of("12", "14", "28", "32", "39").contains(contentTypeId)) { return false; }

        // 기본 공통 필수값 체크 (대표 이미지 및 법정동 지역 코드 누락 방어용 로직)
        if (!StringUtils.hasText(item.getFirstimage()) || !StringUtils.hasText(item.getLDongRegnCd()) || !StringUtils.hasText(item.getLDongSignguCd())) { return false; }

        String title = item.getTitle();
        contentTypeId = item.getContenttypeid();
        String cat1 = item.getLclsSystm1();
        String cat2 = item.getLclsSystm2();
        String cat3 = item.getLclsSystm3();

        // 타이틀 블랙리스트 키워드 필터링용 리스트 적용 (공백 제거 및 소문자 변환으로 비교 정확도 향상)
        if (StringUtils.hasText(title)) {
            String cleanTitle = title.replaceAll("\\s+", "").toLowerCase();
            for (String keyword : BLACK_KEYWORDS) {
                if (cleanTitle.contains(keyword.toLowerCase())) { return false; }
            }
        }

        // 타입별 세부 분류 화이트리스트 / 블랙리스트 검증 로직 (카테고리 로직)
        // AC05 & VE050200 : 실제로는 STAY로 승격되는 예외, TOUR 화이트리스트(isValidTourItem)에 걸려 스킵되기 전에 먼저 STAY 검증으로 라우팅하기
        boolean isCategoryValid = false;
        if ("AC05".equals(cat2) || "VE050200".equals(cat3)) {
            isCategoryValid = isValidStayItem(cat1, cat2, cat3);
        } else if ("12".equals(contentTypeId) || "14".equals(contentTypeId) || "28".equals(contentTypeId)) {
            isCategoryValid = isValidTourItem(cat1, cat2, cat3);
        } else if ("32".equals(contentTypeId)) {
            isCategoryValid = isValidStayItem(cat1, cat2, cat3);
        } else if ("39".equals(contentTypeId)) {
            isCategoryValid = isValidFoodItem(cat1, cat2, cat3);
        }

        // 카테고리 검증 실패 시 즉시 스킵
        if (!isCategoryValid)  { return false; }

        // 해시태그 최소 3개 미만 컷오프 검증, 3개 미만 시 컷오프 처리
        String placeType = convertToPlaceType(contentTypeId, cat2, cat3);
        int estimatedTagCount = hashtagGenerator.estimateTagCount(placeType, cat1, cat2, cat3);
        if (estimatedTagCount < 3) { return false; }

        return true;
    }

    // contentTypeId 및 예외 승격 코드 기반 placeType 변환 헬퍼 메서드
    private String convertToPlaceType(String contentTypeId, String cat2, String cat3) {
        if ("AC05".equals(cat2) || "VE050200".equals(cat3) || "32".equals(contentTypeId)) {
            return "stay";
        } else if ("39".equals(contentTypeId)) {
            return "food";
        }
        return "tour";
    }

    // TOUR 계열 검증 (12, 14, 28) - 필터링 컬럼 상수 선언
    private boolean isValidTourItem(String cat1, String cat2, String cat3) {
        if (!StringUtils.hasText(cat1) || !StringUtils.hasText(cat2)) { return false; }
        // 중분류(cat2) 단위 전체 허용 목록에 있으면 통과
        if (ALLOWED_CAT2_FULL.contains(cat2)) { return true; }
        // 그 외 소분류(cat3) 단위에서 허용된 경우에만 통과 (VE010900 등 화이트리스트에 없는 항목은 여기서 자동으로 차단됨 - 별도 블랙리스트 불필요)
        return ALLOWED_CAT3_SPECIFIC.contains(cat3);
    }

    // STAY 계열 검증 (32 및 예외적으로 28로 들어오는 캠핑 AC05, 12로 들어오는 리조트 VE050200는 별도 필터링)
    // 대분류AC로 들어오는 항목은 contentTypeId와 별개로, 예외 없이 전부 숙박이므로 별도 필터링 없이 전체 허용
    private boolean isValidStayItem(String cat1, String cat2, String cat3) { return true; }

    // FOOD 계열 검증 (39) - 주점 중 클럽(FD040300), 기타주점(FD040500) 차단
    private boolean isValidFoodItem(String cat1, String cat2, String cat3) {
        if ("FD040300".equals(cat3) || "FD040500".equals(cat3)) { return false; }
        return true;
    }

    // 부실데이터 검증용 로직 - minPrice null이면 적재 제외 (단, placeType="food"는 예외 허용) & useFeeInfo(이용요금 텍스트)가 존재하면 적재 허용
    // PlaceDTO 생성(convertToPlaceDTO) 이후, DB 저장 직전 단계에서 호출되는 2차(post-convert) 검증 메서드
    public boolean isValidPrice(Integer minPrice, String placeType, String useFeeInfo) {
        if ("food".equals(placeType)) { return true; }
        if (minPrice != null && minPrice >= 0) { return true; }
        // minPrice가 null이더라도 useFeeInfo(이용요금 텍스트)가 존재하면 적재 허용!
        return StringUtils.hasText(useFeeInfo);    }

    // 부실데이터 예외 보완용(tour/stay 각 3건 추가 확보) 완화된 검증
    // 카테고리 화이트리스트/해시태그 컷오프는 건너뛰고, 블랙리스트 키워드만 필터링
    // (1차 큐 셀렉팅의 isValid()보다 훨씬 느슨한 기준 - 부족한 tour/stay를 별도로 보완하기 위함)
    public boolean isValidBlacklistOnly(TourAreaBasedSyncListDTO item) {
        if (item == null) return false;

        String contentTypeId = item.getContenttypeid();
        if (!List.of("12", "14", "28", "32", "39").contains(contentTypeId)) { return false; }

        String title = item.getTitle();
        if (StringUtils.hasText(title)) {
            String cleanTitle = title.replaceAll("\\s+", "").toLowerCase();
            for (String keyword : BLACK_KEYWORDS) {
                if (cleanTitle.contains(keyword.toLowerCase())) { return false; }
            }
        }
        return true;
    }

}