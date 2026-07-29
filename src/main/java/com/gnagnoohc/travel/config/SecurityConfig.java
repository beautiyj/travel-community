package com.gnagnoohc.travel.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.gnagnoohc.travel.auth.controller.SocialOAuth2LoginHandler;
import com.gnagnoohc.travel.auth.security.SocialOAuth2AuthorizationRequestRepository;

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
        tokenClient.addParametersConverter(grantRequest -> {
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            if ("naver".equals(
                    grantRequest.getClientRegistration().getRegistrationId())) {
                String state = grantRequest.getAuthorizationExchange()
                        .getAuthorizationResponse()
                        .getState();
                if (state != null && !state.isBlank()) {
                    // 네이버 토큰 API는 callback에서 검증된 state를 토큰 요청에도 요구한다.
                    parameters.add("state", state);
                }
            }
            return parameters;
        });
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
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) {
        OidcUserService oidcUserService = new OidcUserService();
        // Google OIDC의 UserInfo 조회에도 Kakao와 같은 5초 HTTP 제한을 적용한다.
        oidcUserService.setOauth2UserService(oauth2UserService);
        return oidcUserService;
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest>
            socialAuthorizationRequestRepository() {
        return new SocialOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            SocialOAuth2LoginHandler socialOAuth2LoginHandler,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest>
                    authorizationRequestRepository,
            SecurityContextRepository securityContextRepository,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> userService,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) throws Exception {
        return http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .securityContext(context ->
                        context.securityContextRepository(securityContextRepository))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(
                                        authorizationRequestRepository))
                        // callback 경로와 ClientRegistration의 redirect-uri를 같은 규칙으로 맞춘다.
                        .redirectionEndpoint(redirection ->
                                redirection.baseUri("/auth/callback/*"))
                        .authorizedClientRepository(authorizedClientRepository)
                        .tokenEndpoint(token ->
                                token.accessTokenResponseClient(tokenClient))
                        .userInfoEndpoint(userInfo ->
                                userInfo
                                        .userService(userService)
                                        .oidcUserService(oidcUserService))
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
