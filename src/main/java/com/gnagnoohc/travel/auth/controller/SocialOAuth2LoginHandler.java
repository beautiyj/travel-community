package com.gnagnoohc.travel.auth.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.PendingSocialAuthIntent;
import com.gnagnoohc.travel.auth.dto.PendingSocialLink;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;
import com.gnagnoohc.travel.auth.service.SocialAuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Spring OAuth2 Client가 검증한 사용자를 서비스의 세션 로그인 또는 추가 가입으로 연결한다.
 * 외부 제공자별 응답 차이는 이 클래스에서 공통 프로필로 변환하고 회원 처리는 서비스에 맡긴다.
 */
@Component
@RequiredArgsConstructor
public class SocialOAuth2LoginHandler
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SocialOAuth2LoginHandler.class);
    private static final String KAKAO_REGISTRATION_ID = "kakao";
    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final String GOOGLE_REGISTRATION_ID = "google";
    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final String PENDING_SOCIAL_SIGNUP = "pendingSocialSignup";
    private static final String PENDING_SOCIAL_LINK = "pendingSocialLink";
    private static final String PENDING_SOCIAL_AUTH_INTENT = "pendingSocialAuthIntent";
    private static final String LOGIN_INTENT = "LOGIN";
    private static final String SIGNUP_INTENT = "SIGNUP";
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
        LoginMemberDto completedLogin = null;
        String intent = null;

        try {
            if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                throw new SocialAuthException("유효하지 않은 소셜 인증 결과입니다.", false);
            }
            oauthToken = token;

            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new SocialAuthException("소셜 로그인 시작 정보를 확인할 수 없습니다.");
            }
            PendingSocialAuthIntent pendingIntent = consumePendingIntent(session);
            String actualRegistrationId = normalizeRegistrationId(
                    token.getAuthorizedClientRegistrationId());
            if (pendingIntent.isExpired()
                    || (!LOGIN_INTENT.equals(pendingIntent.intent())
                            && !SIGNUP_INTENT.equals(pendingIntent.intent()))
                    || !actualRegistrationId.equals(
                            normalizeRegistrationId(pendingIntent.registrationId()))) {
                throw new SocialAuthException("유효하지 않은 소셜 로그인 시작 요청입니다.");
            }
            intent = pendingIntent.intent();

            // 새 인증 결과가 확정되기 전에 남아 있던 미완료 흐름은 재사용하지 않는다.
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);
            session.removeAttribute(PENDING_SOCIAL_LINK);

            SocialProfile socialProfile = extractSocialProfile(token);

            if (LOGIN_INTENT.equals(intent)) {
                completedLogin = socialAuthService.findSocialLoginMember(
                        socialProfile.provider(),
                        socialProfile.providerUserId());
                if (completedLogin == null) {
                    throw new SocialAuthException(
                            SocialAuthException.NOT_LINKED,
                            "연결된 회원이 없는 소셜 계정입니다. 회원가입에서 연동해 주세요.");
                }
                redirectPath = "/";
            } else {
                String normalizedEmail = socialAuthService.normalizeVerifiedOAuthEmail(
                        socialProfile.email(), socialProfile.emailVerified());
                PendingSocialLink pendingLink = socialAuthService.prepareSocialLink(
                        socialProfile.provider(),
                        socialProfile.providerUserId(),
                        normalizedEmail,
                        socialProfile.nickname(),
                        socialProfile.profileImageUrl());
                if (pendingLink != null) {
                    session.setAttribute(PENDING_SOCIAL_LINK, pendingLink);
                    redirectPath = "/auth/social/link";
                } else {
                    PendingSocialSignup pendingSignup = new PendingSocialSignup(
                            socialProfile.provider(),
                            socialProfile.providerUserId(),
                            normalizedEmail,
                            socialProfile.nickname(),
                            socialProfile.profileImageUrl(),
                            // 전역 CSRF가 비활성화된 현재 구조에서 소셜 가입 POST는 별도 nonce로 검증한다.
                            UUID.randomUUID().toString(),
                            true,
                            LocalDateTime.now().plusMinutes(SIGNUP_VALID_MINUTES));
                    session.setAttribute(PENDING_SOCIAL_SIGNUP, pendingSignup);
                    redirectPath = "/auth/social/signup";
                }
            }
        } catch (SocialAuthException e) {
            removePendingSocialFlow(request);
            redirectPath = resolveFailureRedirect(intent, e);
            if (!e.isUserVisible()) {
                log.error("소셜 로그인 처리 중 내부 오류가 발생했습니다.", e);
            }
        } catch (Exception e) {
            removePendingSocialFlow(request);
            // OAuth 응답 원문이나 토큰은 기록하지 않고 예외 종류만 서버 로그에서 확인한다.
            log.error("소셜 로그인 처리 중 예기치 않은 오류가 발생했습니다.", e);
        } finally {
            cleanupOAuthAuthentication(oauthToken, request, response);
        }

        if (completedLogin != null) {
            // OAuth 임시 인증 정리가 끝난 뒤 기존 세션을 폐기하여 세션 고정 공격을 막는다.
            completeLogin(request, completedLogin);
        }
        response.sendRedirect(request.getContextPath() + redirectPath);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        removePendingSocialFlow(request);
        clearSecurityContext(request, response);
        response.sendRedirect(request.getContextPath() + "/auth/login?socialError=true");
    }

    private SocialProfile extractSocialProfile(OAuth2AuthenticationToken token) {
        return switch (normalizeRegistrationId(token.getAuthorizedClientRegistrationId())) {
            case KAKAO_REGISTRATION_ID -> extractKakaoProfile(token.getPrincipal());
            case GOOGLE_REGISTRATION_ID -> extractGoogleProfile(token.getPrincipal());
            default -> throw new SocialAuthException("현재 지원하지 않는 소셜 로그인 제공자입니다.");
        };
    }

    private SocialProfile extractKakaoProfile(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerUserId = stringValue(attributes.get("id"));
        if (providerUserId == null || !providerUserId.matches("^[0-9]+$")) {
            throw new SocialAuthException("유효하지 않은 카카오 회원 식별 정보입니다.");
        }

        Map<?, ?> account = mapValue(attributes.get("kakao_account"));
        Map<?, ?> profile = account == null ? null : mapValue(account.get("profile"));
        boolean verifiedEmail = account != null
                && Boolean.TRUE.equals(account.get("is_email_valid"))
                && Boolean.TRUE.equals(account.get("is_email_verified"));
        return new SocialProfile(
                KAKAO_PROVIDER,
                providerUserId,
                account == null ? null : stringValue(account.get("email")),
                verifiedEmail,
                profile == null ? null : stringValue(profile.get("nickname")),
                profile == null ? null : stringValue(profile.get("profile_image_url")));
    }

    private SocialProfile extractGoogleProfile(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerUserId = stringValue(attributes.get("sub"));
        if (providerUserId == null) {
            throw new SocialAuthException("유효하지 않은 Google 회원 식별 정보입니다.");
        }

        return new SocialProfile(
                GOOGLE_PROVIDER,
                providerUserId,
                stringValue(attributes.get("email")),
                Boolean.TRUE.equals(attributes.get("email_verified")),
                stringValue(attributes.get("name")),
                stringValue(attributes.get("picture")));
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

    private void removePendingSocialFlow(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);
            session.removeAttribute(PENDING_SOCIAL_LINK);
            session.removeAttribute(PENDING_SOCIAL_AUTH_INTENT);
        }
    }

    private PendingSocialAuthIntent consumePendingIntent(HttpSession session) {
        Object sessionValue;
        synchronized (session) {
            sessionValue = session.getAttribute(PENDING_SOCIAL_AUTH_INTENT);
            // callback 재전송이 같은 시작 정보를 다시 사용할 수 없도록 조회 즉시 소비한다.
            session.removeAttribute(PENDING_SOCIAL_AUTH_INTENT);
        }
        if (!(sessionValue instanceof PendingSocialAuthIntent pendingIntent)) {
            throw new SocialAuthException("소셜 로그인 시작 정보를 확인할 수 없습니다.");
        }
        return pendingIntent;
    }

    private String normalizeRegistrationId(String registrationId) {
        if (registrationId == null) {
            throw new SocialAuthException("현재 지원하지 않는 소셜 로그인 제공자입니다.");
        }
        String normalized = registrationId.trim().toLowerCase(Locale.ROOT);
        if (!KAKAO_REGISTRATION_ID.equals(normalized)
                && !GOOGLE_REGISTRATION_ID.equals(normalized)) {
            throw new SocialAuthException("현재 지원하지 않는 소셜 로그인 제공자입니다.");
        }
        return normalized;
    }

    private String resolveFailureRedirect(String intent, SocialAuthException exception) {
        if (SIGNUP_INTENT.equals(intent)) {
            if (SocialAuthException.ALREADY_LINKED.equals(exception.getErrorCode())) {
                return "/auth/signup?socialAlreadyLinked=true";
            }
            return "/auth/signup?socialLinkUnavailable=true";
        }
        if (SocialAuthException.NOT_LINKED.equals(exception.getErrorCode())) {
            return "/auth/login?socialNotLinked=true";
        }
        return "/auth/login?socialError=true";
    }

    private void completeLogin(
            HttpServletRequest request,
            LoginMemberDto loginMember) {
        HttpSession previousSession = request.getSession(false);
        if (previousSession != null) {
            previousSession.invalidate();
        }
        request.getSession(true).setAttribute("loginMember", loginMember);
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

    private record SocialProfile(
            String provider,
            String providerUserId,
            String email,
            boolean emailVerified,
            String nickname,
            String profileImageUrl) {

    }
}
