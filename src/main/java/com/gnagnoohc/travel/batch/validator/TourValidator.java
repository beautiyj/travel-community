package com.gnagnoohc.travel.batch.validator;

import com.gnagnoohc.travel.batch.dto.TourAreaBasedSyncListDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

// TODO: 0731 테스트로직 확인 완료, 블랙리스트 추가필요(의원,병원,축제,팝업,스토어,약국 등등 + 블랙리스트안먹히는문제) + 필터링필요: 기본적으로 공공데이터로 들어오는 데이터는 전부 MIN_PRICE NOT NULL 설정 필요
// TODO: (최종필터링작업필요) 필터링 작업 진행 후, 지역-시군구 별로 랜덤 50-100개씩만 들여오는 최종 필터링 작업 처리할 것

@Component
public class TourValidator {
    // 차단할 블랙리스트 키워드 필터링
    private static final List<String> BLACK_KEYWORDS = List.of(
            "유흥주점", "단란주점", "클럽", "무인", "PC방", "자판기", "휴게소", "노래방", "노래바", "카지노",
            "의원", "병원", "약국", "축제", "팝업", "스토어", "클리닉", "약방", "한의원", "치과", "페스타", "페스티벌"
    );

    // 중분류(cat2) 단위로 전체 허용 - 이 중분류에 속한 소분류는 전부 통과
    private static final Set<String> ALLOWED_CAT2_FULL = Set.of(
            "EX01", "EX02", "EX03", "EX04",
            "HS01", "HS02", "HS03", "HS04",
            "LS01", "LS02", "LS03", "LS04",
            "NA01", "NA02", "NA03", "NA04",
            "VE02", "VE03", "VE07", "VE10"
    );
    // 소분류(cat3) 단위로 필터링 허용 - 위 중분류 전체허용에 안 걸리는 중분류(EX05/06/07, VE01/05/09) 중 일부만 통과
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

        // 기본 공통 필수값 체크 (대표 이미지 및 법정동 지역 코드 누락 방어용 로직)
        if (!StringUtils.hasText(item.getFirstimage()) || !StringUtils.hasText(item.getLDongRegnCd()) || !StringUtils.hasText(item.getLDongSignguCd())) { return false; }

        // MIN_PRICE NOT NULL 검증 (기본적으로 공공데이터로 들어오는 데이터는 전부 MIN_PRICE 필수)
        // if (item.getMinPrice() == null || item.getMinPrice() < 0) { return false; }

        String title = item.getTitle();
        String contentTypeId = item.getContenttypeid();
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

        // 타입별 세부 분류 화이트리스트 / 블랙리스트 검증 로직
        // AC05 & VE050200 : 실제로는 STAY로 승격되는 예외, TOUR 화이트리스트(isValidTourItem)에 걸려 스킵되기 전에 먼저 STAY 검증으로 라우팅하기
        if ("AC05".equals(cat2) || "VE050200".equals(cat3)) { return isValidStayItem(cat1, cat2, cat3); }
        if ("12".equals(contentTypeId) || "14".equals(contentTypeId) || "28".equals(contentTypeId)) { return isValidTourItem(cat1, cat2, cat3); }
        else if ("32".equals(contentTypeId)) { return isValidStayItem(cat1, cat2, cat3); }
        else if ("39".equals(contentTypeId)) { return isValidFoodItem(cat1, cat2, cat3); }
        return false;
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
}
