package com.gnagnoohc.travel.batch.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// batch: 데이터 수집 로직
// 중복되는 파라미터는 Config에서 생성한 빈 주입하는 방식으로 사용!

// 한국관광공사 TourAPI 엔드포인트를 호출하여 대용량 데이터를 받아오는 통신 컴포넌트
// 공공데이터 서버 호출, JSON XML 데이터 받아오기
// XML -> JSON 변환은 주소 뒤에 필수 파라미터 세팅에 _type = "json" 꼭 추가해야 변환됨

@Component
public class TourApiClient {

    private final WebClient webClient;

    // config의 @Bean 주입 - 공통적으로 적용되는 api키, url 엔드포인트를 포함하여 중복파라미터 생략 가능
    public TourApiClient(@Qualifier("tourWebClient") WebClient tourWebClient) {
        this.webClient = tourWebClient;
    }

    /**
     * 위치기반 관광정보조회 /locationBasedList2
     * @param mapX GPS X좌표 (필수)
     * @param mapY GPS Y좌표 (필수)
     * @param radius 반경 m (필수)
     * @param arrange 정렬 구분 (옵션, A=제목순, C=수정일순, D=등록일순, E=거리순) - 선택 사항 (추후 제거 가능)
     * @return 공공데이터 JSON 문자열
     */
    public String fetchLocationBasedTour(String mapX, String mapY, int radius, String arrange) {
        try {
            // 쿼리스트링 빌더를 통해 메서드 전용 특수 변수들과 필수 페이징 정보만 매핑(공통은 config 이동)
            return this.webClient.get()                    // HTTP GET 방식 요청
                .uri(uriBuilder -> uriBuilder
                    .path("/locationBasedList2")
                    .queryParam("numOfRows", "20")
                    .queryParam("pageNo", "1")
                    .queryParam("arrange", arrange)
                    .queryParam("mapX", mapX)
                    .queryParam("mapY", mapY)
                    .queryParam("radius", radius)
                    .build())
                .retrieve()                                // 공공데이터 서버가 응답한 결과 추출
                .bodyToMono(String.class)    // 받아온 JSON 데이터 전체를 String 변환
                .block();                                  // 동기식 배치를 위해 block() 처리하여 대기
        } catch (Exception e) {
            // 에러 로그는 나중에 batch_execution_log 테이블에 기록할 수 있도록 예외를 던지거나 기록 조치
            throw new RuntimeException("fetchLocationBasedTour 공공데이터 API 호출 중 에러 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 지역기반 관광정보조회 /areaBasedList2
     * @param lDongRegnCd 법정동 시도 코드 (옵션)
     * @param lDongSignguCd 법정동 시군구 코드 (옵션, 시도코드 필수)
     * @param contentTypeId 관광타입 ID (옵션)
     * @param arrange 정렬 구분 (옵션)
     */
    public String fetchAreaBasedList(String lDongRegnCd, String lDongSignguCd, String contentTypeId, String arrange) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/areaBasedList2")
                    .queryParam("numOfRows", "20")
                    .queryParam("pageNo", "1")
                    .queryParam("arrange", arrange)
                    .queryParam("contentTypeId", contentTypeId)
                    .queryParam("lDongRegnCd", lDongRegnCd)
                    .queryParam("lDongSignguCd", lDongSignguCd)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchAreaBasedList 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 키워드 검색 조회 /searchKeyword2
     * @param keyword 요청 키워드 (필수)
     * @param arrange 정렬 구분 (옵션)
     */
    public String fetchSearchKeywordTour(String keyword, String arrange) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/searchKeyword2")
                    .queryParam("numOfRows", "20")
                    .queryParam("pageNo", "1")
                    .queryParam("arrange", arrange)
                    .queryParam("keyword", keyword)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchSearchKeywordTour 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 숙박정보조회 /searchStay2
     * contentTypeId 숙박에서만 유효
     * @param 법정동 시도/시군구 및 분류체계 대/중/소분류 조건 필터링 (옵션)
     */
    public String fetchSearchStay(String lDongRegnCd, String lDongSignguCd, 
                                String lclsSystm1, String lclsSystm2, String lclsSystm3, String arrange) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/searchStay2")
                    .queryParam("numOfRows", "20")
                    .queryParam("pageNo", "1")
                    .queryParam("arrange", arrange)             // 정렬필터 (A, C, D 등)
                    .queryParam("lDongRegnCd", lDongRegnCd)     // 법정동 시도
                    .queryParam("lDongSignguCd", lDongSignguCd) // 법정동 시군구
                    .queryParam("lclsSystm1", lclsSystm1)       // 대분류
                    .queryParam("lclsSystm2", lclsSystm2)       // 중분류
                    .queryParam("lclsSystm3", lclsSystm3)       // 소분류
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchSearchStay 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 공통정보조회 /detailCommon2
     * @param contentId 콘텐츠 ID
     */
    public String fetchDetailCommon(String contentId) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/detailCommon2")
                    .queryParam("contentId", contentId)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchDetailCommon 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 소개정보조회 /detailIntro2
     * contentTypeId 각 타입마다 응답 항목 다르게 제공
     * @param contentId 콘텐츠 ID (필수)
     * @param contentTypeId 관광타입 ID (필수)
     */
    public String fetchDetailIntro(String contentId, String contentTypeId) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/detailIntro2")
                    .queryParam("contentId", contentId)
                    .queryParam("contentTypeId", contentTypeId)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchDetailIntro 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 반복정보조회 /detailInfo2
     * contentTypeId 숙박, 여행코스를 제외한 나머지 타입은 다양한 정보를 반복형태로 제공(반복정보유형 가이드 참고)
     * @param contentId 콘텐츠 ID (필수)
     * @param contentTypeId 관광타입 ID (필수)
     */
    public String fetchDetailInfo(String contentId, String contentTypeId) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/detailInfo2")
                    .queryParam("contentId", contentId)
                    .queryParam("contentTypeId", contentTypeId)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchDetailInfo 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 이미지정보조회 /detailImage2
     * contentTypeId 음식점 타입은 음식 메뉴 이미지로 제공
     * @param contentId 콘텐츠 ID (필수)
     * @param imageYN 이미지조회 구분 Y/N (옵션, Y=콘텐츠이미지조회 N="음식점"타입의음식메뉴이미지)
     */
    public String fetchDetailImage(String contentId, String imageYN) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/detailImage2")
                    .queryParam("contentId", contentId)
                    .queryParam("imageYN", imageYN)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchDetailImage 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 관광정보 동기화 목록 조회 /areaBasedSyncList2 - 배치 수집 전용 API (DB 최신상태 유지용 API)
     * 파라미터에 따라 제목순, 수정일순(최신순), 등록일순 정렬 검색 제공
     * @param modifiedtime 콘텐츠변경일자 (옵션)
     * @param showflag 콘텐츠표출여부 1/0 (옵션, 1=표출 0=비표출)
     */
    public String fetchAreaBasedSyncList(String modifiedtime, String showflag, String arrange) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/areaBasedSyncList2")
                    .queryParam("numOfRows", "20")
                    .queryParam("pageNo", "1")
                    .queryParam("arrange", arrange)
                    .queryParam("modifiedtime", modifiedtime)
                    .queryParam("showflag", showflag)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchAreaBasedSyncList 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 반려동물 동반 여행 정보 /detailPetTour2
     * @param contentId 콘텐츠 ID (옵션, 미기입 시 반려동물 모두 출력)
     */
    public String fetchDetailPetTour(String contentId) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/detailPetTour2")
                    .queryParam("numOfRows", "10")
                    .queryParam("pageNo", "1")
                    .queryParam("contentId", contentId)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchDetailPetTour 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 법정동코드조회 /ldongCode2
     * 요청변수(lDongListYn-목록조회 여부) 통해서 전체 법정동코드 정보 조회 가능
     * @param lDongRegnCd 시도코드 (옵션, 입력이 없을시 전체 시도목록 호출)
     * @param lDongListYn 법정동 목록조회 여부 Y/N (옵션, N=코드조회 Y=전체목록조회)
     */
    public String fetchLdongCode(String lDongRegnCd, String lDongListYn) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/ldongCode2")
                    .queryParam("numOfRows", "50")
                    .queryParam("pageNo", "1")
                    .queryParam("lDongRegnCd", lDongRegnCd)
                    .queryParam("lDongListYn", lDongListYn)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchLdongCode 에러: " + e.getMessage(), e);
        }
    }

    /**
     * 분류체계 코드조회 /lclsSystmCode2
     * 요청변수(lclsSystmListYn-목록조회 여부) 통해서 전체 분류체계코드 정보 조회 가능
     * @param lclsSystm1 대분류코드 (옵션)
     * @param lclsSystm2 중분류코드 (옵션, lclsSystm1 필수)
     * @param lclsSystmListYn 분류체계 목록조회 여부 Y/N (옵션, lclsSystm1 lclsSystm2 필수, N=코드조회 Y=전체목록조회)
     */
    public String fetchLclsSystmCode(String lclsSystm1, String lclsSystm2, String lclsSystmListYn) {
        try {
            return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/lclsSystmCode2")
                    .queryParam("numOfRows", "50")
                    .queryParam("pageNo", "1")
                    .queryParam("lclsSystm1", lclsSystm1)
                    .queryParam("lclsSystm2", lclsSystm2)
                    .queryParam("lclsSystmListYn", lclsSystmListYn)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("fetchLclsSystmCode 에러: " + e.getMessage(), e);
        }
    }
}