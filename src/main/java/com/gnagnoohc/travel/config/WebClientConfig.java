package com.gnagnoohc.travel.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import lombok.extern.slf4j.Slf4j;

// config: 필요한 로직 세팅(프로그램 시작 전 세팅 개념)
// @Configuration: 스프링이 설정 파일로 인식, 내부의 @Bean들을 자동으로 등록
// @Qualifier: 나중에 다른 외부 API를 쓰기 위해 WebClient를 하나 더 만들 경우 @Bean에 이름 붙여서 구분하기 (ex) @Qualifier("tourWebClient")

@Slf4j
@Configuration
public class WebClientConfig {

    // 공공데이터 서비스 인증키 & URL 매핑
    @Value("${tour.api.service-key}") private String serviceKey;
    @Value("${tour.api.base-url}") private String baseUrl;

    // 카카오페이 인증키 (결제 + 정합성 보정 조회가 공유하는 통신 빈에서 사용)
    @Value("${kakaopay.secret-key}") private String kakaoSecretKey;

    @Bean
    @Qualifier("tourWebClient")
    public WebClient tourWebClient(WebClient.Builder builder) {

        // TODO: 0728 기존 CONFIG -> 명시적선언으로 변경, 데이터베이스 연동 후 테스트로직 이후 로그 확인-테스트 완료 시 삭제
        // 0728 서버 기동 시 키가 잘 들어오는지 확인하기 위한 로그
        log.info("Loaded Tour API ServiceKey: [{}]", serviceKey);
        log.info("Loaded Tour API BaseUrl: [{}]", baseUrl);

        
        // 공통 파라미터 맵 구성 - 중복되는 공공데이터 api 공통 요소 기입
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        
        // Spring WebClient는 기본적으로 URL을 조립할 때 파라미터를 강제로 한 번 더 인코딩하므로
        // 공공데이터 API의 이미 인코딩된 서비스키가 double-encoding되어 깨지는 것을 방지하는 코드 추가 (기본제공 디코딩키, 인코딩키면 none처리)
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        return builder
            .uriBuilderFactory(factory)
            // Map.of(...) 강제로 캐스팅, 타입 불일치 해소(타입 불일치/널세이프티 경고 방지용으로 명시해둠)
            // .defaultUriVariables((Map<String, ?>) (Map<String, String>) Map.of(
                // "serviceKey", serviceKey,
                // "MobileOS", "ETC",
                // "MobileApp", "Travel",
                // "_type", "json"
            // ))
            .build();
    }

    /**
     * 카카오페이 통신 빈. 일반 결제(ready/approve/cancel)와 정합성 보정 조회(getOrderStatus)가 공유한다.
     * host·인증(SECRET_KEY)을 여기 한 곳에서 세팅 → 서비스/배치가 중복 없이 재사용.
     */
    @Bean
    public RestClient kakaoPayRestClient() {
        return RestClient.builder()
            .baseUrl("https://open-api.kakaopay.com")
            .defaultHeader("Authorization", "SECRET_KEY " + kakaoSecretKey)
            .build();
    }
}