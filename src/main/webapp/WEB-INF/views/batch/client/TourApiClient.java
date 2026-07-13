package com.gnagnoohc.travel.batch.client;

// 한국관광공사 TourAPI 엔드포인트를 호출하여 대용량 데이터를 받아오는 통신 컴포넌트
// 공공데이터 서버 호출, JSON XML 데이터 받아오기
// XML -> JSON 변환은 주소 뒤에 필수 파라미터 세팅에 _type = "json" 꼭 추가해야 변환됨

@Component
public class TourApiClient {

    private final WebClient webClient;
    
    // 공공데이터 서비스 인증키 & URL 매핑
    @Value("${tour.api.service-key}") private String serviceKey; 
    @Value("${tour.api.base-url}") private String baseUrl;

    public TourApiClient(WebClient.Builder webClientBuilder) {
        // 공공데이터 기본 엔드포인트 주소 설정
        this.webClient = webClientBuilder.baseUrl(this.baseUrl).build();
    }

    /**
     * 위치기반 관광정보 조회 API 호출 (JSON 수신 강제)
     * @param mapX 경도
     * @param mapY 위도
     * @param radius 반경(m)
     * @return 공공데이터가 던져준 순수 JSON 문자열
     */
    public String fetchLocationBasedTour(String mapX, String mapY, int radius) {
        try {
            // 💡 중요: 공공데이터 특유의 서비스키 인코딩 깨짐을 방지하기 위해 URI를 수동으로 안전하게 조립합니다.
            String urlString = "/locationBasedList2"
                    + "?serviceKey=" + serviceKey  // 인증키
                    + "&numOfRows=100"             // 한 페이지 결과 수
                    + "&pageNo=1"                  // 페이지 번호
                    + "&MobileOS=ETC"              // OS 구분 (필수 필수값)
                    + "&MobileApp=TravelApp"       // 서비스명 (필수 필수값)
                    + "&mapX=" + mapX              // GPS X좌표 (경도)
                    + "&mapY=" + mapY              // GPS Y좌표 (위도)
                    + "&radius=" + radius          // 거리 반경
                    + "&_type=json";               // ⭐ 팀장님이 짚으신 치트키: 무조건 JSON으로 응답 유도

            URI uri = new URI(urlString);

            // 외부 API 호출 후 결과 JSON을 통째로 String으로 받아 리턴
            return this.webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 동기식 배치를 위해 block() 처리하여 대기

        } catch (Exception e) {
            // 에러 로그는 나중에 batch_execution_log 테이블에 기록할 수 있도록 예외를 던지거나 기록 조치
            throw new RuntimeException("공공데이터 API 호출 중 에러 발생: " + e.getMessage(), e);
        }
    }
}