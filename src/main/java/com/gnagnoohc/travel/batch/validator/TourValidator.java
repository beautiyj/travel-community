package com.gnagnoohc.travel.batch.validator;

import com.gnagnoohc.travel.batch.dto.TourAreaBasedSyncListDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

// 공공데이터 필터링 처리/검증하는 로직. Converter로 보내기 전 블랙리스트/화이트리스트 1차 필터링 처리 필요 -> Helper & Converter에서 세부 2차 필터링 작업 진행.
/* TODO: contentTypeId 세부분류 필터링 필요 (+ 1차 필터링으로 기타분류를 제외하더라고 특정 항목의 기타 제외, 필터링되도록 추가작업 필요)
 *  TOUR관광지 : 12관광지. 14문화시설, 28레포츠
 *   - 12 - 대분류 EX체험관광 - EX01, EX02, EX03, EX04, EX05-EX050100/EX050200/EX050300, EX06-EX060100, EX07-EX070100
 *   - 12 - 대분류 HS역사관광 - HS01, HS02, HS03, HS04
 *   - 28 - 대분류 LS레저스포트 - LS01, LS02, LS03, LS04
 *   - 12 - 대분류 NA자연관광 - NA01, NA02, NA03, NA04
 *   - 12 - 대분류 VE문화관광 - VE01-VE010200/VE010300/VE010800, VE02, VE03, VE05-VE050100, VE07, VE09-VE090100/VE090200/VE090400
 *   - 28 - 대분류 VE문화관광 - VE10
 *
 *   -> 12 - 대분류 EX체험관광 - EX02-EX020400의경우 해시태그 기타말고 세부분류 필요 ex 라탄,캔들 등)
 *   -> 12 - 대분류 HS역사관광 - HS01-HS011200, HS02-HS020400, HS03-HS030400, HS04-HS040400 기타컬럼 해시세분화필터링
 *   -> 28 - 대분류 LS레저스포트 - LS01-LS011900, LS02-LS021400, LS03-LS030600 기타컬럼 해시세분화필터링
 *
 *  STAY숙박 : 32숙박
 *   - 32 - 대분류 AC숙박 - AC01, AC02, AC03, AC04, AC06
 *   - 28 - 대분류 AC숙박 - AC05
 *   - 12 - 대분류 VE문화관광 - VE05-VE050200
 *
 *   -> 해시태그로 추가할 것: #관광지 #소분류명
 *   -> AC05(캠핑)중분류는 32가 아닌 28레포츠로 들어오므로 소분류명 4개는 숙박으로 처리 필요
 *
 *  FOOD맛집 : 39음식점
 *   - 39 - 대분류 FD음식 - FD01, FD02, FD03, FD04-FD040100/FD040200/FD040300/FD040400, FD05-FD050100/FD050200
 *
 *   -> 중분류(lclsSystem2) 중분류명으로 1차 해시태그 + 소분류 코드(clsSystem3) 2차 해시태그 로직 구현 필요
 *   -> 2차 해시태그 중, 피자, 햄버거, 샌드위치 및 유사음식/기타간이음식/기타외국식 등 소분류명은 #패스트푸드 #푸드트럭 등으로 변환 혹은 세부필터링으로 태그별도추가 필요
 *   -> 중분류/소분류 필터링 처리에서도 유흥주점/단란주점/무인가게/PC방/자판기/휴게소 등 제거 필터링 처리 필요
 * */

@Component
public class TourValidator {

    // 차단할 블랙리스트 키워드 (유흥, 무인, 편의시설 등) 1차 필터링
    private static final List<String> BLACK_KEYWORDS = List.of(
            "유흥주점", "단란주점", "클럽", "무인", "PC방", "자판기", "휴게소", "노래방"
    );

    // [TOUR / 문화시설 / 레포츠] 허용할 소분류(Cat3) 및 중분류 화이트리스트
    // 소분류 단위로 떨어지는 것들은 코드로 관리, 대/중분류 단위로 다 받는 것은 별도 분기 처리
    private static final Set<String> ALLOWED_TOUR_CAT3 = Set.of(
            // EX 체험관광 중 허용 항목 (EX020400 등 기타는 키워드/세부 필터링으로 태그 처리하되 코드는 포괄 허용 혹은 차단)
            // HS 역사관광 (HS01, HS02, HS03, HS04 전체 허용 등은 아래 메서드에서 대분류/중분류 단위로 제어)
            // VE 문화관광 중 허용 코드 (VE010900 기타 건축/조형물은 명시적으로 제외!)
            "VE010200", "VE010300", "VE010800",
            "VE050100",
            "VE090100", "VE090200", "VE090400"
    );

    // [핵심] 수집 대상 아이템 유효성 최종 검증 메서드
    public boolean isValid(TourAreaBasedSyncListDTO item) {
        if (item == null) return false;

        // 1. 기본 공통 필수값 체크 (대표 이미지 및 법정동 지역 코드 누락 방어)
        if (!StringUtils.hasText(item.getFirstimage()) ||
                !StringUtils.hasText(item.getLDongRegnCd()) ||
                !StringUtils.hasText(item.getLDongSignguCd())) {
            return false;
        }

        String title = item.getTitle();
        String contentTypeId = item.getContenttypeid();
        String cat1 = item.getLclsSystm1(); // 대분류
        String cat2 = item.getLclsSystm2(); // 중분류
        String cat3 = item.getLclsSystm3(); // 소분류

        // 2. 타이틀 블랙리스트 키워드 필터링 (유흥, 무인, PC방 등)
        if (StringUtils.hasText(title)) {
            for (String keyword : BLACK_KEYWORDS) {
                if (title.contains(keyword)) {
                    return false; // 즉시 스킵
                }
            }
        }

        // 3. 타입별 세부 분류 화이트리스트 / 블랙리스트 검증 로직
        if ("12".equals(contentTypeId) || "14".equals(contentTypeId) || "28".equals(contentTypeId)) {
            return isValidTourItem(cat1, cat2, cat3);
        } else if ("32".equals(contentTypeId)) {
            return isValidStayItem(cat1, cat2, cat3);
        } else if ("39".equals(contentTypeId)) {
            return isValidFoodItem(cat1, cat2, cat3);
        }

        return false; // 정의되지 않은 타입은 차단
    }

    // TOUR 계열 검증 (12, 14, 28)
    private boolean isValidTourItem(String cat1, String cat2, String cat3) {
        if (!StringUtils.hasText(cat1)) return false;

        // 예: 랜드마크(VE01) 중 '기타 건축/조형물(VE010900)'은 명시적으로 차단
        if ("VE010900".equals(cat3)) {
            return false;
        }

        // 그 외 대분류별(EX, HS, LS, NA, VE 등) 정책에 따른 허용/불가 판정 로직 작성
        return true;
    }

    // STAY 계열 검증 (32 및 예외적으로 28로 들어오는 캠핑 AC05)
    private boolean isValidStayItem(String cat1, String cat2, String cat3) {
        // 숙박 필터링 규칙 적용 (협동조합 등 비숙박 시설 컷오프)
        return true;
    }

    // FOOD 계열 검증 (39) - 주점 중 클럽(FD040300), 기타주점(FD040500) 차단 등
    private boolean isValidFoodItem(String cat1, String cat2, String cat3) {
        // 주점 세부 코드 중 위험한 것들 차단
        if ("FD040300".equals(cat3) || "FD040500".equals(cat3)) {
            return false;
        }
        return true;
    }
}
