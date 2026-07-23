package com.gnagnoohc.travel.auth.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gnagnoohc.travel.auth.dto.KakaoUserInfo;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;

/**
 * 카카오 OAuth 서버와 통신하는 경계다.
 * 인가 코드 교환과 사용자 정보 조회까지만 담당하며 회원 DB에는 접근하지 않는다.
 */
@Component
public class KakaoApiClient {

    private static final String AUTHORIZE_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    @Autowired
    public KakaoApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${kakao.oauth.client-id:}") String clientId,
            @Value("${kakao.oauth.client-secret:}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri:}") String redirectUri) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 카카오 장애가 웹 요청 스레드를 오래 점유하지 않도록 연결과 응답 대기 시간을 제한한다.
        requestFactory.setConnectTimeout(HTTP_TIMEOUT);
        requestFactory.setReadTimeout(HTTP_TIMEOUT);
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 실제 네트워크 없이 HTTP 요청을 검증하는 단위 테스트에서만 사용한다.
     */
    KakaoApiClient(
            RestClient restClient,
            String clientId,
            String clientSecret,
            String redirectUri) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 카카오 개발자 콘솔에 등록한 redirect URI와 동일한 주소로 인가 URL을 만든다.
     */
    public String createAuthorizationUri(String state) {
        if (isBlank(clientId) || isBlank(redirectUri)) {
            throw new IllegalStateException("카카오 OAuth 설정이 완료되지 않았습니다.");
        }

        return UriComponentsBuilder.fromUriString(AUTHORIZE_URI)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    /**
     * 인가 코드를 토큰으로 교환한 뒤 사용자 정보를 조회한다.
     * 이 메서드는 Controller에서 Service보다 먼저 호출해 DB 트랜잭션 밖에서 실행한다.
     */
    public KakaoUserInfo requestUserInfo(String authorizationCode) {
        if (isBlank(authorizationCode)) {
            throw new SocialAuthException("카카오 인가 코드가 없습니다. 다시 로그인해 주세요.");
        }
        if (isBlank(clientId) || isBlank(redirectUri)) {
            throw new SocialAuthException("카카오 OAuth 설정이 완료되지 않았습니다.");
        }

        try {
            String accessToken = requestAccessToken(authorizationCode);
            KakaoUserResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new SocialAuthException("카카오 회원 식별 정보를 확인할 수 없습니다.");
            }

            KakaoAccount account = response.kakaoAccount();
            KakaoProfile profile = account == null ? null : account.profile();
            return new KakaoUserInfo(
                    response.id().toString(),
                    account == null ? null : account.email(),
                    account != null && Boolean.TRUE.equals(account.emailValid()),
                    account != null && Boolean.TRUE.equals(account.emailVerified()),
                    profile == null ? null : profile.nickname(),
                    profile == null ? null : profile.profileImageUrl());
        } catch (SocialAuthException e) {
            throw e;
        } catch (RestClientException e) {
            // 카카오 응답 원문에는 민감 정보가 포함될 수 있어 사용자나 로그에 그대로 노출하지 않는다.
            throw new SocialAuthException(
                    "카카오 인증 서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }

    private String requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        // 카카오 콘솔에서 Client Secret을 사용하도록 설정한 경우에만 전송한다.
        if (!isBlank(clientSecret)) {
            form.add("client_secret", clientSecret);
        }

        KakaoTokenResponse tokenResponse = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (tokenResponse == null || isBlank(tokenResponse.accessToken())) {
            throw new SocialAuthException("카카오 액세스 토큰을 발급받지 못했습니다.");
        }
        return tokenResponse.accessToken();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 토큰 응답은 이 Client 밖에서 사용하지 않으므로 내부 DTO로 둔다.
     */
    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken) {
    }

    /**
     * 카카오 원본 JSON 구조는 외부 연동 경계 밖으로 전달하지 않는다.
     */
    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    }

    private record KakaoAccount(
            String email,
            @JsonProperty("is_email_valid") Boolean emailValid,
            @JsonProperty("is_email_verified") Boolean emailVerified,
            KakaoProfile profile) {
    }

    private record KakaoProfile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl) {
    }
}
