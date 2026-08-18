package com.gnagnoohc.travel.batch.converter;

import com.gnagnoohc.travel.batch.dto.TourDetailIntroDTO;
import com.gnagnoohc.travel.batch.dto.TourItemDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* HashtagGenerator.java - 해시태그 생성 파일 (공공데이터 분류에 따른 해시태그 구분 로직)
 * - converter 변환 처리 후, 규칙에 맞춰 해시태그 생성 & 변환
 * - 해시태그 부실 데이터 분류하여 컷오프

 * [해시태그 생성 및 변환 규칙]
 * 1. 기본 타입 태그 (고정 1개)
   - tour -> #관광지 / stay -> #숙박 / food -> #맛집

 * 2. 카테고리(대/중/소분류)별 세부 태그 매핑
   - STAY (숙박) : #소분류명 (예: #호텔, #펜션)
   - TOUR (관광) : 대분류(EX, HS, LS, NA, VE) 규칙에 따라 중분류명 및 소분류명 선택 부여
     - EX(체험)/HS(역사) : #중분류명
     - LS(레저)          : #중분류명, #소분류명 (둘 다)
     - NA(자연)/VE(인문) : 지정된 규칙 세트(Cat2/Cat3)에 따라 중분류 또는 소분류 선택
   - FOOD (음식) : 기본적으로 #중분류명 + #소분류명
     - FD05(카페/찻집)   : #소분류명만 부여
     - 소분류 예외 변환 : 기타외국식(#이색양식), 피자/햄버거/샌드위치(#패스트푸드),
                         이동음식(#푸드트럭&포장마차), 기타간이음식(#간이음식), 제과(#제과, #디저트)

 * 3. 부가/특수 조건 태그
   - 반려동물 데이터 존재 시 : #반려동물동반
   - 상세 소개정보(intro) 기준 : #주차가능 (주차 가능 시), #연중무휴 (휴무일 연중무휴 시)

 * 4. 컷오프 처리
   - 중복 제거 후 최종 해시태그가 3개 미만인 부실 데이터는 적재 대상에서 제외 (null 반환)
 */
@Component
public class HashtagGenerator {

    // 콘텐츠 타입(placeType) -> 한글 해시태그로 매핑 적용
    private static final Map<String, String> BASE_TYPE_TAG = Map.of(
            "tour", "관광지",
            "stay", "숙박",
            "food", "맛집"
    );

    // 중분류(cat2) 코드 -> 한글명
    private static final Map<String, String> CAT2_NAME = Map.ofEntries(
            Map.entry("EX01", "전통체험"), Map.entry("EX02", "공예체험"), Map.entry("EX03", "농산어촌체험"),
            Map.entry("EX04", "산사체험"), Map.entry("EX05", "웰니스관광"), Map.entry("EX06", "산업관광"), Map.entry("EX07", "기타체험"),
            Map.entry("HS01", "역사유적지"), Map.entry("HS02", "역사유물"), Map.entry("HS03", "종교성지"), Map.entry("HS04", "안보관광지"),
            Map.entry("LS01", "육상레저스포츠"), Map.entry("LS02", "수상레저스포츠"), Map.entry("LS03", "항공레저스포츠"), Map.entry("LS04", "복합레저스포츠"),
            Map.entry("NA03", "자연생태"), Map.entry("NA04", "자연공원"),
            Map.entry("VE01", "랜드마크관광"), Map.entry("VE03", "도시공원"), Map.entry("VE05", "복합관광시설"),
            Map.entry("FD01", "한식"), Map.entry("FD02", "외국식"), Map.entry("FD03", "간이음식"), Map.entry("FD04", "주점")
    );

    // 소분류(cat3) 코드 -> 한글명 (Validator 화이트리스트 통과분 + STAY/FOOD 전부. 데이터 필터링 기준은 notion의 엑셀파일로 확인할 것)
    private static final Map<String, String> CAT3_NAME = Map.ofEntries(
            Map.entry("EX050100", "온천/사우나/스파"), Map.entry("EX050200", "찜질방"), Map.entry("EX050300", "한방체험"),
            Map.entry("EX060100", "근대산업유산"), Map.entry("EX070100", "유람선/잠수함관광"),
            Map.entry("LS010100", "인라인"), Map.entry("LS010200", "자전거하이킹"), Map.entry("LS010300", "카트"),
            Map.entry("LS010400", "골프"), Map.entry("LS010500", "경마"), Map.entry("LS010600", "경륜"),
            Map.entry("LS010700", "승마"), Map.entry("LS010800", "스키/스노보드"), Map.entry("LS010900", "스케이트"),
            Map.entry("LS011000", "썰매장"), Map.entry("LS011100", "수렵장"), Map.entry("LS011200", "사격장"),
            Map.entry("LS011300", "암벽등반"), Map.entry("LS011400", "서바이벌게임"), Map.entry("LS011500", "ATV"),
            Map.entry("LS011600", "MTB"), Map.entry("LS011700", "오프로드"), Map.entry("LS011800", "번지점프"),
            Map.entry("LS011900", "기타육상레저스포츠"),
            Map.entry("LS020100", "윈드서핑/제트스키"), Map.entry("LS020200", "카약/카누"), Map.entry("LS020300", "요트"),
            Map.entry("LS020400", "스노쿨링/스킨스쿠버다이빙"), Map.entry("LS020500", "민물낚시"), Map.entry("LS020600", "바다낚시"),
            Map.entry("LS020700", "수영"), Map.entry("LS020800", "래프팅"), Map.entry("LS020900", "수상오토바이"),
            Map.entry("LS021000", "수상자전거"), Map.entry("LS021100", "조정"), Map.entry("LS021200", "워터슬레드"),
            Map.entry("LS021300", "패러세일"), Map.entry("LS021400", "기타수상레저스포츠"),
            Map.entry("LS030100", "스카이다이빙"), Map.entry("LS030200", "초경량비행"), Map.entry("LS030300", "행글라이딩/패러글라이딩"),
            Map.entry("LS030400", "열기구"), Map.entry("LS030500", "무인비행장치(드론)"), Map.entry("LS030600", "기타항공레저스포츠"),
            Map.entry("LS040100", "복합레저스포츠"),
            Map.entry("NA010100", "산/고개/오름/봉우리"), Map.entry("NA010200", "숲"), Map.entry("NA010300", "폭포"),
            Map.entry("NA010400", "계곡"), Map.entry("NA010500", "약수터"),
            Map.entry("NA020100", "강"), Map.entry("NA020200", "호수"), Map.entry("NA020300", "저수지"),
            Map.entry("NA020400", "연못/늪"), Map.entry("NA020500", "섬"), Map.entry("NA020600", "염전"),
            Map.entry("NA020700", "항구/포구"), Map.entry("NA020800", "해안절경"), Map.entry("NA020900", "해변/해수욕장"),
            Map.entry("VE020100", "테마파크"), Map.entry("VE020200", "워터파크"), Map.entry("VE020300", "동물원"),
            Map.entry("VE020400", "수족관/아쿠아리움"), Map.entry("VE020500", "천문대"),
            Map.entry("VE070100", "박물관"), Map.entry("VE070200", "기념관"), Map.entry("VE070300", "전시관"),
            Map.entry("VE070400", "컨벤션센터"), Map.entry("VE070500", "과학관"), Map.entry("VE070600", "미술관/화랑"),
            Map.entry("VE090100", "한국문화원"), Map.entry("VE090200", "외국문화원"), Map.entry("VE090400", "문화전수시설"),
            Map.entry("VE100100", "스포츠경기장"), Map.entry("VE100200", "스포츠센터/수련시설"),
            Map.entry("AC010100", "호텔"), Map.entry("AC020100", "콘도"), Map.entry("AC020200", "레지던스"),
            Map.entry("AC030100", "펜션"), Map.entry("AC030200", "한옥스테이"), Map.entry("AC030300", "농어촌민박"),
            Map.entry("AC030400", "홈스테이"), Map.entry("AC040100", "모텔"),
            Map.entry("AC050100", "일반야영장"), Map.entry("AC050200", "오토캠핑장"), Map.entry("AC050300", "카라반"), Map.entry("AC050400", "글램핑장"),
            Map.entry("AC060100", "유스호스텔"), Map.entry("AC060200", "게스트하우스"),
            Map.entry("VE050200", "리조트"),
            Map.entry("FD010100", "관광식당"), Map.entry("FD010200", "모범음식점"),
            Map.entry("FD020100", "중식"), Map.entry("FD020200", "일식"), Map.entry("FD020300", "서양식"),
            Map.entry("FD020400", "기타외국식"), Map.entry("FD020500", "퓨전음식"),
            Map.entry("FD030100", "제과"), Map.entry("FD030200", "피자/햄버거/샌드위치"), Map.entry("FD030300", "치킨"),
            Map.entry("FD030400", "김밥/분식"), Map.entry("FD030500", "이동음식"), Map.entry("FD030600", "기타간이음식"),
            Map.entry("FD040100", "바/펍"), Map.entry("FD040200", "생맥주전문점"), Map.entry("FD040400", "전통주/민속주점"),
            Map.entry("FD050100", "카페"), Map.entry("FD050200", "찻집"), Map.entry("FD050300", "기타음료점")
    );

    private static final Set<String> VE_CAT2_ONLY = Set.of("VE01", "VE03", "VE05");
    private static final Set<String> VE_CAT3_ONLY = Set.of("VE02", "VE07", "VE09", "VE10");
    private static final Set<String> NA_CAT3_ONLY = Set.of("NA01", "NA02");
    private static final Set<String> NA_CAT2_ONLY = Set.of("NA03", "NA04");

    // 해시태그 생성 메인 메서드
    public String generateHashtags(TourItemDTO item, TourDetailIntroDTO intro, String placeType, String cat1, String cat2, String cat3) {
        List<String> tags = new ArrayList<>();

        // 콘텐츠 타입 기본 태그 (#관광지 / #숙박 / #맛집) - 한글 고정 태그로 저장
        String baseTag = BASE_TYPE_TAG.get(placeType);
        if (StringUtils.hasText(baseTag)) { tags.add("#" + baseTag); }

        // 타입별 중/소분류 태그 규칙 적용
        if ("stay".equals(placeType)) { addStayTags(tags, cat3); }
        else if ("tour".equals(placeType)) { addTourTags(tags, cat1, cat2, cat3); }
        else if ("food".equals(placeType)) { addFoodTags(tags, cat2, cat3); }

        if (StringUtils.hasText(item.getAcmpyPsblCpam()) || StringUtils.hasText(item.getPetTursmInfo())) { tags.add("#반려동물동반"); }

        // 소개정보(intro) 기반 주차/휴무일 부가정보 태그 자동 추가
        addExtraIntroTags(tags, intro);

        List<String> uniqueTags = tags.stream().filter(StringUtils::hasText).distinct().toList();
        // 해시태그 3개 미만일 경우 데이터베이스에 적재x
        if (uniqueTags.size() < 3) { return null; }
        return String.join(",", uniqueTags);
    }

    // STAY 계열: #소분류명 하나만 추가 (숙박 기본태그는 이미 위에서 추가됨)
    private void addStayTags(List<String> tags, String cat3) {
        addTagByCode(tags, CAT3_NAME, cat3);
    }

    // TOUR 계열: 대분류(cat1) 기준 EX/HS/LS/NA/VE 규칙 분기.
    private void addTourTags(List<String> tags, String cat1, String cat2, String cat3) {
        if (!StringUtils.hasText(cat1)) return;

        if ("EX".equals(cat1) || "HS".equals(cat1)) {
            addTagByCode(tags, CAT2_NAME, cat2);
        } else if ("LS".equals(cat1)) {
            addTagByCode(tags, CAT2_NAME, cat2);
            addTagByCode(tags, CAT3_NAME, cat3);
        } else if ("NA".equals(cat1)) {
            if (NA_CAT3_ONLY.contains(cat2)) {
                addTagByCode(tags, CAT3_NAME, cat3);
            } else if (NA_CAT2_ONLY.contains(cat2)) {
                addTagByCode(tags, CAT2_NAME, cat2);
            }
        } else if ("VE".equals(cat1)) {
            if (VE_CAT2_ONLY.contains(cat2)) {
                addTagByCode(tags, CAT2_NAME, cat2);
            } else if (VE_CAT3_ONLY.contains(cat2)) {
                addTagByCode(tags, CAT3_NAME, cat3);
            }
        }
    }

    // FOOD 계열: 기본 중분류+소분류, 단 일부 소분류는 변환/추가 예외 적용
    private void addFoodTags(List<String> tags, String cat2, String cat3) {
        if ("FD05".equals(cat2)) {
            addTagByCode(tags, CAT3_NAME, cat3);
            return;
        }

        addTagByCode(tags, CAT2_NAME, cat2);

        // 소분류 예외 변환 처리
        if ("FD020400".equals(cat3)) {
            tags.add("#이색양식");
        } else if ("FD030200".equals(cat3)) {
            tags.add("#패스트푸드");
        } else if ("FD030500".equals(cat3)) {
            tags.add("#푸드트럭&포장마차");
        } else if ("FD030600".equals(cat3)) {
            tags.add("#간이음식");
        } else if ("FD030100".equals(cat3)) {
            addTagByCode(tags, CAT3_NAME, cat3);
            tags.add("#디저트");
        } else {
            addTagByCode(tags, CAT3_NAME, cat3);
        }
    }

    // 부가정보(주차가능, 연중무휴) 태그 자동 생성 메서드 추가
    private void addExtraIntroTags(List<String> tags, TourDetailIntroDTO intro) {
        if (intro == null) return;

        // 주차 가능 여부 검사 (타입별 주차 필드 전용 헬퍼 활용)
        String parking = firstHasText(intro.getParking(), intro.getParkingculture(), intro.getParkingleports(), intro.getParkinglodging(), intro.getParkingfood());
        if (StringUtils.hasText(parking) && parking.contains("가능")) {
            tags.add("#주차가능");
        }

        // 쉬는날 연중무휴 여부 검사 (타입별 휴무일 필드 전용 헬퍼 활용)
        String restdate = firstHasText(intro.getRestdate(), intro.getRestdateculture(), intro.getRestdateleports(), intro.getRestdatefood());
        if (StringUtils.hasText(restdate) && restdate.contains("연중무휴")) {
            tags.add("#연중무휴");
        }
    }

    // 우선순위에 따라 유효한 첫 번째 문자열 반환 헬퍼
    private String firstHasText(String... values) {
        for (String val : values) {
            if (StringUtils.hasText(val)) return val;
        }
        return null;
    }

    // 코드 -> 한글명 매핑 조회 후 태그 추가 (매핑에 없으면 조용히 스킵, 원문 코드값을 그대로 태그화하지 않음)
    private void addTagByCode(List<String> tags, Map<String, String> nameMap, String code) {
        String name = nameMap.get(code);
        if (StringUtils.hasText(name)) {
            tags.add("#" + name.replaceAll(" ", ""));
        }
    }

    // 목록 단계(syncItem)에서 미리 예상되는 해시태그 개수를 계산하는 헬퍼 메서드 - 해시태그 3개 미만 시 부실데이터 처리 예정
    public int estimateTagCount(String placeType, String cat1, String cat2, String cat3) {
        List<String> tags = new ArrayList<>();

        // 기본 타입 태그 (#관광지 / #숙박 / #맛집) -> 1개
        String baseTag = BASE_TYPE_TAG.get(placeType);
        if (StringUtils.hasText(baseTag)) { tags.add("#" + baseTag); }

        // 카테고리 태그 추가 (중분류 / 소분류)
        if ("stay".equals(placeType)) { addStayTags(tags, cat3); }
        else if ("tour".equals(placeType)) { addTourTags(tags, cat1, cat2, cat3); }
        else if ("food".equals(placeType)) { addFoodTags(tags, cat2, cat3); }

        // 중복 제거 후 최종 태그 개수 반환
        return (int) tags.stream().filter(StringUtils::hasText).distinct().count();
    }

}