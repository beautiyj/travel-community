package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessExtraInfoOptionDto;
import com.gnagnoohc.travel.business.dto.BusinessExtraInfoSectionDto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 업소 부가정보(PLACE.extra_info) 입력 옵션 목록과 저장 포맷 변환을 담당한다.
 *
 * 저장 포맷은 공공데이터 배치(TourApiHelper.extractExtraInfo)와 동일하다.
 *   [라벨1] 값1\n[라벨2] 값2\n...
 * 값이 빈 라벨은 저장하지 않고, 값이 있는 라벨만 아래 목록 순서대로 이어붙인다.
 * 프론트는 \n으로 split해 목록으로 렌더링하므로, 배치로 들어온 장소와 사업자 직접등록 장소가
 * 화면에서 똑같이 보인다.
 *
 * 라벨 문구는 "부가정보(extraInfo) 입력 옵션 명세서 v2"에서 확정된 값이다. 공공데이터 표기와
 * 어긋나면 같은 유형의 장소가 화면에서 다르게 보이므로 임의로 바꾸지 않는다.
 * (설명·샘플값은 화면 안내용이라 자유롭게 다듬어도 된다)
 */
public final class BusinessExtraInfoCatalog {

    /*
     * 관광지는 place_type='tour' 하나로 관광지/문화시설/레포츠를 함께 다루기 때문에
     * 명세서에서 세부유형이 표시된 6개 라벨을 소제목으로 갈라 놓았다. 저장 포맷과는 무관한
     * 화면상 구분이라, 어떤 섹션에서 고르든 [라벨] 값 형태로 똑같이 저장된다.
     */
    private static final List<BusinessExtraInfoSectionDto> TOUR_SECTIONS = List.of(
            section(null,
                    option("휴무일", "쉬는 날", "매주 월요일"),
                    option("이용시간", "운영·관람 시간", "09:00~18:00"),
                    option("주차", "주차 가능 여부·안내", "가능 / 불가능 / 인근 공영주차장 이용"),
                    option("문의", "문의 전화번호", "02-741-0466"),
                    option("수용인원", "동시 수용 가능 인원", "최대 200명"),
                    option("유모차대여", "유모차 대여 가능 여부", "가능"),
                    option("신용카드", "신용카드 결제 가능 여부", "가능"),
                    option("애완동물동반", "반려동물 동반 가능 여부", "불가능")),
            section("관광지 전용",
                    option("이용시기", "계절·시즌 안내", "3월~11월"),
                    option("개장일", "최초 개장일", "2015-05-01")),
            section("문화시설 전용",
                    option("할인정보", "할인 안내", "경로·장애인 50% 할인"),
                    option("관람소요시간", "평균 관람 소요시간", "약 1시간")),
            section("레포츠 전용",
                    option("개장기간", "운영 기간", "4월~10월"),
                    option("예약안내", "예약 필요 여부·방법", "홈페이지 사전예약 필수"),
                    option("규모", "시설 규모", "부지면적 5,000㎡"))
    );

    private static final List<BusinessExtraInfoSectionDto> STAY_SECTIONS = List.of(
            section(null,
                    option("입/퇴실", "체크인·체크아웃 시간", "16:00 / 10:30"),
                    option("주차", "주차 가능 여부·안내", "가능"),
                    option("문의", "문의 전화번호", "064-739-4499"),
                    option("수용인원", "최대 수용 가능 인원", "최대 4인"),
                    option("객실수", "전체 객실 수", "12실"),
                    option("객실유형", "제공하는 객실 유형", "스탠다드 / 디럭스 / 스위트"),
                    option("식음료장", "조식·식당 등 식음료 시설", "조식 레스토랑 운영"),
                    option("부대시설", "기타 부대시설 안내", "수영장, 세미나실"),
                    option("예약안내", "예약 방법", "홈페이지·전화 예약"))
    );

    private static final List<BusinessExtraInfoSectionDto> FOOD_SECTIONS = List.of(
            section(null,
                    option("영업시간", "영업 시간", "09:00~19:00"),
                    option("휴무일", "쉬는 날", "연중무휴"),
                    option("주차", "주차 가능 여부·안내", "가능"),
                    option("문의", "문의 전화번호", "033-333-5841"),
                    option("대표메뉴", "대표 메뉴", "한우 갈비탕"),
                    option("취급메뉴", "취급하는 전체 메뉴 카테고리", "갈비탕, 냉면, 수육"),
                    option("좌석수", "좌석 규모", "40석"),
                    option("예약안내", "예약 가능 여부·방법", "전화 예약 가능"),
                    option("포장가능", "포장 가능 여부", "가능"),
                    option("신용카드", "신용카드 결제 가능 여부", "가능"),
                    option("할인정보", "할인 안내", "단체 10% 할인"),
                    option("어린이놀이방", "어린이 놀이시설 여부", "있음"),
                    option("개업일", "개업일", "2020-03-01"))
    );

    // 반려동물 동반 정보는 업종과 무관한 공통 항목이라 타입별 목록과 분리해 별도 섹션으로 노출한다
    private static final List<BusinessExtraInfoOptionDto> PET_OPTIONS = List.of(
            option("동반가능동물", "동반 가능한 동물 종류", "소형견만 가능"),
            option("반려동물 관광정보", "반려동물 동반 관광 관련 안내", "반려동물 동반 산책로 운영"),
            option("동반시 필요사항", "목줄, 이동장 등 필요사항", "목줄 필수"),
            option("기타 동반 정보", "기타 주의사항", "대형견 동반 불가"),
            option("동반유형", "동반 유형 구분", "실내 동반 가능"),
            option("관련 렌탈 품목", "대여 가능한 반려동물 용품", "유모차 대여 가능"),
            option("관련 비치 품목", "비치된 반려동물 용품", "배변봉투 비치"),
            option("관련 구비 시설", "반려동물 관련 구비 시설", "놀이터, 급수대"),
            option("관련 사고 대비사항", "사고 대비 안내", "배상책임보험 가입")
    );

    // 폼에 그려지는 순서를 고정하기 위해 순서가 유지되는 맵을 쓴다
    private static final Map<String, List<BusinessExtraInfoSectionDto>> SECTIONS_BY_PLACE_TYPE;

    static {
        Map<String, List<BusinessExtraInfoSectionDto>> byType = new LinkedHashMap<>();
        byType.put("stay", STAY_SECTIONS);
        byType.put("food", FOOD_SECTIONS);
        byType.put("tour", TOUR_SECTIONS);
        SECTIONS_BY_PLACE_TYPE = Collections.unmodifiableMap(byType);
    }

    // "[라벨] 값" 한 줄을 라벨과 값으로 되돌린다. 값에 ]가 들어가도 첫 ]까지만 라벨로 본다.
    private static final Pattern ENTRY_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]\\s*(.*)$");

    private BusinessExtraInfoCatalog() {
    }

    private static BusinessExtraInfoOptionDto option(String label, String description, String sample) {
        return new BusinessExtraInfoOptionDto(label, description, sample);
    }

    private static BusinessExtraInfoSectionDto section(String title, BusinessExtraInfoOptionDto... options) {
        return new BusinessExtraInfoSectionDto(title, List.of(options));
    }

    /**
     * "업종 코드 -> 섹션 목록" 전체.
     * 폼은 업종을 바꿔도 바로 갈아끼울 수 있게 세 업종을 한 번에 그려두므로 통째로 필요하다.
     */
    public static Map<String, List<BusinessExtraInfoSectionDto>> sectionsByPlaceType() {
        return SECTIONS_BY_PLACE_TYPE;
    }

    /** 반려동물 동반 정보(전 업종 공통 섹션) 항목 목록. */
    public static List<BusinessExtraInfoOptionDto> petOptions() {
        return PET_OPTIONS;
    }

    /** 업종의 항목을 섹션 구분 없이 펼친 목록. 알 수 없는 업종이면 빈 목록. */
    public static List<BusinessExtraInfoOptionDto> optionsFor(String placeType) {
        return SECTIONS_BY_PLACE_TYPE.getOrDefault(placeType, List.of()).stream()
                .flatMap(section -> section.getOptions().stream())
                .toList();
    }

    /**
     * 해당 업종의 폼에서 보낼 수 있는 라벨 전체(업종별 + 반려동물 공통).
     * 폼을 조작해 임의의 라벨을 밀어 넣으면 공공데이터 표기와 어긋난 값이 저장되므로 서버에서 걸러낸다.
     */
    public static Set<String> allowedLabels(String placeType) {
        Set<String> labels = optionsFor(placeType).stream()
                .map(BusinessExtraInfoOptionDto::getLabel)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        PET_OPTIONS.forEach(opt -> labels.add(opt.getLabel()));
        return labels;
    }

    /**
     * 폼에서 라벨/값을 짝지어 받아 extra_info 저장 문자열로 조립한다.
     * - 값이 비어 있는 라벨은 건너뛴다 (고르지 않은 항목은 disabled라 아예 오지도 않는다)
     * - 저장할 항목이 하나도 없으면 null (컬럼을 비워 둔다)
     *
     * labels/values는 폼의 hidden input과 text input이 같은 순서로 실려 오는 병렬 리스트다.
     * 길이가 어긋나면 라벨과 값이 밀려 엉뚱하게 저장되므로 조용히 넘기지 않고 예외로 막는다.
     */
    public static String assemble(String placeType, List<String> labels, List<String> values) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        if (values == null || labels.size() != values.size()) {
            throw new IllegalArgumentException("부가정보 입력값을 올바르게 전달받지 못했습니다.");
        }

        Set<String> allowed = allowedLabels(placeType);
        // 같은 라벨이 두 번 오면 화면에도 두 줄로 보이므로 마지막 값만 남긴다 (입력 순서는 유지)
        Map<String, String> collected = new LinkedHashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i) == null ? "" : labels.get(i).trim();
            String value = values.get(i) == null ? "" : values.get(i).trim();
            if (label.isEmpty() || value.isEmpty()) {
                continue;
            }
            if (!allowed.contains(label)) {
                throw new IllegalArgumentException("허용되지 않는 부가정보 항목입니다: " + label);
            }
            collected.put(label, value);
        }

        if (collected.isEmpty()) {
            return null;
        }
        return collected.entrySet().stream()
                .map(entry -> "[" + entry.getKey() + "] " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 저장된 extra_info를 "라벨 -> 값" 맵으로 되돌린다 (수정 폼에 기존 값을 채우기 위함).
     * "[라벨] 값" 형식이 아닌 줄은 건너뛴다.
     */
    public static Map<String, String> parse(String extraInfo) {
        Map<String, String> parsed = new LinkedHashMap<>();
        if (extraInfo == null || extraInfo.isBlank()) {
            return parsed;
        }
        for (String line : extraInfo.split("\n")) {
            Matcher matcher = ENTRY_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                parsed.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }
        return parsed;
    }
}
