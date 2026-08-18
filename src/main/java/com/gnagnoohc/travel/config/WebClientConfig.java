package com.gnagnoohc.travel.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

// config: 필요한 로직 세팅(프로그램 시작 전 세팅 개념)
// @Configuration: 스프링이 설정 파일로 인식, 내부의 @Bean들을 자동으로 등록
// @Qualifier: 나중에 다른 외부 API를 쓰기 위해 WebClient를 하나 더 만들 경우 @Bean에 이름 붙여서 구분하기 (ex) @Qualifier("tourWebClient")

@Configuration
public class WebClientConfig {

    // 공공데이터 서비스 인증키 & URL 매핑
    @Value("${tour.api.service-key}") private String serviceKey; 
    @Value("${tour.api.base-url}") private String baseUrl;

    @Bean
    @Qualifier("tourWebClient")
    public WebClient tourWebClient(WebClient.Builder builder) {

        // 공통 파라미터 맵 구성 - 중복되는 공공데이터 api 공통 요소 기입
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        
        // Spring WebClient는 기본적으로 URL을 조립할 때 파라미터를 강제로 한 번 더 인코딩하므로
        // 공공데이터 API의 이미 인코딩된 서비스키가 double-encoding되어 깨지는 것을 방지하는 코드 추가 (기본제공 디코딩키, 인코딩키면 none처리)
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        return builder
            .uriBuilderFactory(factory)
            // Map.of(...) 강제로 캐스팅, 타입 불일치 해소(타입 불일치/널세이프티 경고 방지용으로 명시해둠)
            .defaultUriVariables((Map<String, ?>) (Map<String, String>) Map.of(
                "serviceKey", serviceKey,
                "MobileOS", "ETC",
                "MobileApp", "Travel",
                "_type", "json"
            ))
            .build();
    }
}