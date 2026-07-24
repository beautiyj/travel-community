package com.gnagnoohc.travel.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.gnagnoohc.travel.auth.controller.SocialOAuth2LoginHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Duration OAUTH_HTTP_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        // OAuth 인증 결과를 지울 때 필터와 성공 핸들러가 같은 저장소를 사용해야 세션에 인증이 남지 않는다.
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            authorizationCodeTokenResponseClient() {
        RestClient restClient = RestClient.builder()
                .requestFactory(oauthRequestFactory())
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new FormHttpMessageConverter());
                    converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();

        RestClientAuthorizationCodeTokenResponseClient tokenClient =
                new RestClientAuthorizationCodeTokenResponseClient();
        tokenClient.setRestClient(restClient);
        return tokenClient;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        RestTemplate restTemplate = new RestTemplate(oauthRequestFactory());
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
        userService.setRestOperations(restTemplate);
        return userService;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            SocialOAuth2LoginHandler socialOAuth2LoginHandler,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            SecurityContextRepository securityContextRepository,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> userService) throws Exception {
        return http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .securityContext(context ->
                        context.securityContextRepository(securityContextRepository))
                .oauth2Login(oauth2 -> oauth2
                        // callback 경로와 ClientRegistration의 redirect-uri를 같은 규칙으로 맞춘다.
                        .redirectionEndpoint(redirection ->
                                redirection.baseUri("/auth/callback/*"))
                        .authorizedClientRepository(authorizedClientRepository)
                        .tokenEndpoint(token ->
                                token.accessTokenResponseClient(tokenClient))
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(userService))
                        .successHandler(socialOAuth2LoginHandler)
                        .failureHandler(socialOAuth2LoginHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/authorization/**",
                                "/memberform",
                                "/memberinsert",
                                "/login",
                                "/mypage",
                                "/updateform",
                                "/update",
                                "/deleteform",
                                "/delete",
                                "/logout1",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**")
                        .permitAll()
                        .anyRequest().permitAll())
                .build();
    }

    private SimpleClientHttpRequestFactory oauthRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 외부 OAuth 서버 장애가 애플리케이션 요청 스레드를 오래 점유하지 않도록 제한한다.
        requestFactory.setConnectTimeout(OAUTH_HTTP_TIMEOUT);
        requestFactory.setReadTimeout(OAUTH_HTTP_TIMEOUT);
        return requestFactory;
    }
}
