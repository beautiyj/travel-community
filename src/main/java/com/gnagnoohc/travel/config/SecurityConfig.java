package com.gnagnoohc.travel.config;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
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
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.support.MultipartFilter;

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
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistration() {
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setMultipartResolverBeanName("multipartResolver");

        FilterRegistrationBean<MultipartFilter> registration =
                new FilterRegistrationBean<>(multipartFilter);
        // multipart body의 CSRF parameter를 Security의 CsrfFilter보다 먼저 읽을 수 있어야 한다.
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        // 소셜 인증 결과를 지울 때 필터와 성공 처리기가 같은 저장소를 사용해야 세션에 인증이 남지 않는다.
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    public RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        // 로그인 뒤 복귀할 대상은 보호된 화면 조회 요청만 허용한다. 비동기 응답,
        // 소셜 인증 흐름, 정적 파일, 결제 콜백은 로그인 후 리다이렉트 대상이 되면 안 된다.
        requestCache.setRequestMatcher(this::isCacheableHtmlGet);
        return requestCache;
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
        // 구글 사용자 정보 조회에도 카카오와 같은 5초 통신 제한을 적용한다.
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
            RequestCache requestCache,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> userService,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            ObjectMapper objectMapper) throws Exception {
        return http
                .cors(cors -> cors.disable())
                .formLogin(form -> form.disable())
                .securityContext(context ->
                        context
                                .requireExplicitSave(true)
                                .securityContextRepository(securityContextRepository))
                .requestCache(cache -> cache.requestCache(requestCache))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(
                                        authorizationRequestRepository))
                        // 콜백 경로와 클라이언트 등록 정보의 리다이렉트 주소를 같은 규칙으로 맞춘다.
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
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD,
                                DispatcherType.INCLUDE)
                        .permitAll()
                        .requestMatchers(
                                "/",
                                "/tour/**",
                                "/auth/**",
                                "/oauth2/authorization/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/uploads/place/**",
                                "/upload/**",
                                "/uploads/mypage/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/event/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/community/list",
                                "/community/detail",
                                "/community/place/search")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/payments/kakao/success",
                                "/payments/kakao/cancel",
                                "/payments/kakao/fail",
                                "/payments/toss/success",
                                "/payments/toss/fail")
                        .permitAll()
                        .requestMatchers("/admin", "/admin/**")
                        .access(requiresAuthorities("TYPE_ADMIN", "ROLE_ADMIN"))
                        .requestMatchers("/business", "/business/**", "/api/business/**")
                        .access(requiresAuthorities("TYPE_BUSINESS", "ROLE_BUSINESS"))
                        .requestMatchers("/mypage/business-info", "/mypage/business-info/**")
                        .access(requiresAuthorities("TYPE_BUSINESS"))
                        .requestMatchers("/mypage", "/mypage/**", "/reservations", "/reservations/**", "/payments/**")
                        .access(requiresAuthorities("TYPE_GENERAL", "ROLE_USER"))
                        .requestMatchers("/community/**")
                        .authenticated()
                        .anyRequest().denyAll())
                .build();
    }

    private AuthorizationManager<org.springframework.security.web.access.intercept.RequestAuthorizationContext>
            requiresAuthorities(String... requiredAuthorities) {
        return (authentication, context) -> {
            var currentAuthentication = authentication.get();
            if (currentAuthentication == null || !currentAuthentication.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            java.util.Set<String> grantedAuthorities = currentAuthentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(java.util.stream.Collectors.toSet());
            boolean administrator = grantedAuthorities.containsAll(
                    java.util.Set.of("TYPE_ADMIN", "ROLE_ADMIN"));
            boolean authorized = administrator || grantedAuthorities.containsAll(
                    java.util.Set.of(requiredAuthorities));
            return new AuthorizationDecision(authorized);
        };
    }

    private org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint(
            ObjectMapper objectMapper) {
        LoginUrlAuthenticationEntryPoint loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/auth/login");
        return (request, response, exception) -> {
            if (apiRequestMatcher().matches(request)) {
                writeJsonError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED",
                        "로그인이 필요합니다.",
                        objectMapper);
                return;
            }
            loginEntryPoint.commence(request, response, exception);
        };
    }

    private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> {
            if (apiRequestMatcher().matches(request)) {
                writeJsonError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "ACCESS_DENIED",
                        "접근 권한이 없습니다.",
                        objectMapper);
                return;
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        };
    }

    private void writeJsonError(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            ObjectMapper objectMapper) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
    }

    private boolean isCacheableHtmlGet(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod()) || apiRequestMatcher().matches(request)) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return matchesAny(request,
                "/admin", "/admin/**",
                "/business", "/business/**",
                "/mypage", "/mypage/**",
                "/reservations", "/reservations/**",
                "/payments/**",
                "/community/**")
                && !isPublicPaymentCallback(path);
    }

    private boolean isPublicPaymentCallback(String path) {
        return "/payments/kakao/success".equals(path)
                || "/payments/kakao/cancel".equals(path)
                || "/payments/kakao/fail".equals(path)
                || "/payments/toss/success".equals(path)
                || "/payments/toss/fail".equals(path);
    }

    private boolean matchesAny(HttpServletRequest request, String... patterns) {
        for (String pattern : patterns) {
            if (new AntPathRequestMatcher(pattern).matches(request)) {
                return true;
            }
        }
        return false;
    }

    private RequestMatcher apiRequestMatcher() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher("/api/business/**"),
                new AntPathRequestMatcher("/mypage/withdraw/check-password", "POST"),
                new AntPathRequestMatcher("/mypage/withdraw", "POST"),
                new AntPathRequestMatcher("/mypage/business-info/withdraw/check-password", "POST"),
                new AntPathRequestMatcher("/mypage/business-info/withdraw", "POST"),
                new AntPathRequestMatcher("/mypage/wishlist/toggle", "POST"),
                new AntPathRequestMatcher("/mypage/wishlist/status/**", "GET"),
                new AntPathRequestMatcher("/reservations/availability", "GET"),
                new AntPathRequestMatcher("/reservations/*/refund-preview", "GET"),
                new AntPathRequestMatcher("/reservations/*/cancel-request", "POST"),
                new AntPathRequestMatcher("/payments/kakao/ready/**", "POST"),
                new AntPathRequestMatcher("/payments/toss/ready/**", "POST"),
                new AntPathRequestMatcher("/payments/vcard/pay/**", "POST"),
                new AntPathRequestMatcher("/payments/bank/ready/**", "POST"),
                new AntPathRequestMatcher("/payments/bank/confirm/**", "POST"),
                new AntPathRequestMatcher("/payments/*/cancel", "POST"),
                new AntPathRequestMatcher("/admin/business-applications/*/document", "GET"));
    }

    private SimpleClientHttpRequestFactory oauthRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 외부 소셜 인증 서버 장애가 애플리케이션 요청 스레드를 오래 점유하지 않도록 제한한다.
        requestFactory.setConnectTimeout(OAUTH_HTTP_TIMEOUT);
        requestFactory.setReadTimeout(OAUTH_HTTP_TIMEOUT);
        return requestFactory;
    }
}
