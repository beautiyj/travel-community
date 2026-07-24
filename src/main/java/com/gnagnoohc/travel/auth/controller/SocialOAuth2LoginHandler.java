package com.gnagnoohc.travel.auth.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;
import com.gnagnoohc.travel.auth.service.SocialAuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Spring OAuth2 Client가 검증한 사용자 정보를 우리 서비스의 세션 로그인 또는 추가 가입으로 연결한다.
 * 현재 실행 가능한 제공자는 카카오뿐이며 Google/Naver는 설정 전까지 이 경계에서 허용하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SocialOAuth2LoginHandler
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SocialOAuth2LoginHandler.class);
    private static final String KAKAO_REGISTRATION_ID = "kakao";
    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final String PENDING_SOCIAL_SIGNUP = "pendingSocialSignup";
    private static final int SIGNUP_VALID_MINUTES = 10;

    private final SocialAuthService socialAuthService;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final SecurityContextRepository securityContextRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String redirectPath = "/auth/login?socialError=true";
        OAuth2AuthenticationToken oauthToken = null;

        try {
            if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                throw new SocialAuthException("유효하지 않은 소셜 인증 결과입니다.", false);
            }
            oauthToken = token;
            if (!KAKAO_REGISTRATION_ID.equals(token.getAuthorizedClientRegistrationId())) {
                throw new SocialAuthException("현재 지원하지 않는 소셜 로그인 제공자입니다.");
            }

            HttpSession session = request.getSession(true);
            // 새 인증 결과가 확정되기 전에 남아 있던 미완료 가입 정보는 재사용하지 않는다.
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);

            KakaoProfile kakaoProfile = extractKakaoProfile(token.getPrincipal());
            LoginMemberDto loginMember = socialAuthService.findSocialLoginMember(
                    KAKAO_PROVIDER,
                    kakaoProfile.providerUserId());

            if (loginMember != null) {
                session.setAttribute("loginMember", loginMember);
                redirectPath = "/";
            } else {
                if (!kakaoProfile.hasVerifiedEmail()) {
                    throw new SocialAuthException("카카오 계정의 검증된 이메일 제공 동의가 필요합니다.");
                }

                PendingSocialSignup pendingSignup = new PendingSocialSignup(
                        KAKAO_PROVIDER,
                        kakaoProfile.providerUserId(),
                        kakaoProfile.email(),
                        kakaoProfile.nickname(),
                        kakaoProfile.profileImageUrl(),
                        // 전역 CSRF가 비활성화된 현재 구조에서 소셜 가입 POST만 별도 nonce로 검증한다.
                        UUID.randomUUID().toString(),
                        true,
                        LocalDateTime.now().plusMinutes(SIGNUP_VALID_MINUTES));
                session.setAttribute(PENDING_SOCIAL_SIGNUP, pendingSignup);
                redirectPath = "/auth/social/signup";
            }
        } catch (SocialAuthException e) {
            removePendingSignup(request);
            if (!e.isUserVisible()) {
                log.error("소셜 로그인 처리 중 내부 오류가 발생했습니다.", e);
            }
        } catch (Exception e) {
            removePendingSignup(request);
            // OAuth 응답 원문이나 토큰은 로그에 남기지 않고 예외 종류만 서버 로그에서 확인한다.
            log.error("소셜 로그인 처리 중 예기치 않은 오류가 발생했습니다.", e);
        } finally {
            cleanupOAuthAuthentication(oauthToken, request, response);
        }

        response.sendRedirect(request.getContextPath() + redirectPath);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        removePendingSignup(request);
        clearSecurityContext(request, response);
        response.sendRedirect(request.getContextPath() + "/auth/login?socialError=true");
    }

    private KakaoProfile extractKakaoProfile(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerUserId = stringValue(attributes.get("id"));
        if (providerUserId == null || !providerUserId.matches("^[0-9]+$")) {
            throw new SocialAuthException("유효하지 않은 카카오 회원 식별 정보입니다.");
        }

        Map<?, ?> account = mapValue(attributes.get("kakao_account"));
        Map<?, ?> profile = account == null ? null : mapValue(account.get("profile"));
        return new KakaoProfile(
                providerUserId,
                account == null ? null : stringValue(account.get("email")),
                account != null && Boolean.TRUE.equals(account.get("is_email_valid")),
                account != null && Boolean.TRUE.equals(account.get("is_email_verified")),
                profile == null ? null : stringValue(profile.get("nickname")),
                profile == null ? null : stringValue(profile.get("profile_image_url")));
    }

    private void cleanupOAuthAuthentication(
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            if (authentication != null) {
                authorizedClientRepository.removeAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication,
                        request,
                        response);
            }
        } catch (RuntimeException e) {
            log.error("소셜 로그인 임시 토큰 정리에 실패했습니다.", e);
        } finally {
            clearSecurityContext(request, response);
        }
    }

    private void clearSecurityContext(
            HttpServletRequest request,
            HttpServletResponse response) {
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        try {
            SecurityContextHolder.setContext(emptyContext);
            // 애플리케이션 인증은 loginMember 세션만 사용하므로 Spring OAuth 인증은 즉시 제거한다.
            securityContextRepository.saveContext(emptyContext, request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void removePendingSignup(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);
        }
    }

    private Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private record KakaoProfile(
            String providerUserId,
            String email,
            boolean emailValid,
            boolean emailVerified,
            String nickname,
            String profileImageUrl) {

        private boolean hasVerifiedEmail() {
            return email != null && !email.isBlank() && emailValid && emailVerified;
        }
    }
}
