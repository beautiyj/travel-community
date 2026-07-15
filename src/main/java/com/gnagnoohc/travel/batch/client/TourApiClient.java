package com.gnagnoohc.travel.batch.client;

// 한국관광공사 TourAPI 엔드포인트를 호출하여 대용량 데이터를 받아오는 통신 컴포넌트
// 공공데이터 서버 호출, JSON XML 데이터 받아오기
// XML -> JSON 변환은 주소 뒤에 필수 파라미터 세팅에 _type = "json" 꼭 추가해야 변환됨

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TourApiClient {

    private final WebClient webClient;
    
    // 공공데이터 서비스 인증키 & URL 매핑
    @Value("${tour.api.service-key}") private String serviceKey; 
    @Value("${tour.api.base-url}") private String baseUrl;

    // 공공데이터 불러올 때 같은 코드들은 상수로 정의해서 재사용하기
    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "Travel";

    // 공공데이터 기본 엔드포인트 주소 설정
    public TourApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(this.baseUrl).build();
    }

    /**
     * 1. 위치기반 관광정보 조회 API 호출 (JSON)
     * @param mapX 경도
     * @param mapY 위도
     * @param radius 반경(m)
     * @return 공공데이터 JSON 문자열
     */
    public String fetchLocationBasedTour(String mapX, String mapY, int radius) {
        try {
            // 공공데이터 서비스키 인코딩 깨짐을 방지하기 위해 URI를 수동으로 조립 필요
            String urlString = "/locationBasedList2"
                + "?serviceKey=" + serviceKey
                + "&numOfRows=10"              // 한 페이지 결과 수
                + "&pageNo=1"                  // 페이지 번호
                + "&MobileOS=" + MOBILE_OS     // OS 구분
                + "&MobileApp=" + MOBILE_APP   // 서비스명 (필수값)
                + "&mapX=" + mapX              // GPS X좌표 (경도)
                + "&mapY=" + mapY              // GPS Y좌표 (위도)
                + "&radius=" + radius          // 거리 반경
                + "&_type=json";               // JSON으로 응답 변환

            URI uri = new URI(urlString);

            // webClient 프레임워크 표준 체이닝 문법 - 외부 API 호출 후 결과 JSON을 통째로 String으로 받아 리턴
            return this.webClient.get()         // HTTP GET 방식 요청
                .uri(uri)                       // 공공데이터 URL 주소로 요청 (uri 세팅)
                .retrieve()                     // 공공데이터 서버가 응답한 결과 추출
                .bodyToMono(String.class)       // 받아온 JSON 데이터 전체를 String 변환
                .block();                       // 동기식 배치를 위해 block() 처리하여 대기(동기식 대기 형태)

        } catch (Exception e) {
            // 에러 로그는 나중에 batch_execution_log 테이블에 기록할 수 있도록 예외를 던지거나 기록 조치
            throw new RuntimeException("공공데이터 API 호출 중 에러 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 2.  지역기반관광정보조회
     */

    /**
     * 3. 키워드 관광정보 조회 API 호출 (JSON)
     * @param keyword 요청키워드
     * @return 공공데이터 JSON 문자열
     */
    public String fetchSearchKeywordTour(String keyword) {
        try {
            String urlString = "/searchKeyword2"
                + "?serviceKey=" + serviceKey
                + "&numOfRows=10"
                + "&pageNo=1"
                + "&MobileOS=" + MOBILE_OS
                + "&MobileApp=" + MOBILE_APP
                + "&keyword=" + keyword
                + "&_type=json";
            URI uri = new URI(urlString);

            return this.webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            throw new RuntimeException("공공데이터 API 호출 중 에러 발생: " + e.getMessage(), e);
        }
    }

}