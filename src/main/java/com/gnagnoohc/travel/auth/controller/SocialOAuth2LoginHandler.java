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
import com.gnagnoohc.travel.auth.security.LoginSessionManager;
import com.gnagnoohc.travel.auth.service.SocialAuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 인증 제공자가 검증한 사용자를 서비스의 세션 로그인 또는 추가 가입으로 연결한다.
 * 외부 제공자별 응답 차이는 이 클래스에서 공통 프로필로 변환하고 회원 처리는 서비스에 맡긴다.
 * <p>
 * 시큐리티가 콜백의 소셜 인증 요청과 상태값을 검증한 다음 이 처리기를 호출한다.
 * 처리기는 서비스 고유의 로그인·가입 의도를 한 번만 소비하고, 성공 후 시큐리티의
 * 임시 인증·승인된 클라이언트를 제거한다. 애플리케이션의 최종 로그인 기준은
 * 회원 정보 세션 하나이며 소셜 인증 액세스 토큰을 로그인 세션으로 유지하지 않는다.
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
    private static final String NAVER_REGISTRATION_ID = "naver";
    private static final String NAVER_PROVIDER = "NAVER";
    private static final int NAVER_PROVIDER_USER_ID_MAX_LENGTH = 64;
    private static final String PENDING_SOCIAL_SIGNUP = "pendingSocialSignup";
    private static final String PENDING_SOCIAL_LINK = "pendingSocialLink";
    private static final String PENDING_SOCIAL_AUTH_INTENT = "pendingSocialAuthIntent";
    private static final String PENDING_SOCIAL_AUTH_FLOW_ID = "pendingSocialAuthFlowId";
    private static final String PENDING_SOCIAL_AUTH_STATE = "pendingSocialAuthState";
    private static final String LOGIN_INTENT = "LOGIN";
    private static final String SIGNUP_INTENT = "SIGNUP";
    private static final int SIGNUP_VALID_MINUTES = 10;

    private final SocialAuthService socialAuthService;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final SecurityContextRepository securityContextRepository;
    private final LoginSessionManager loginSessionManager;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        // 예외가 발생해도 안전한 기본 목적지로 이동하며 토큰이나 제공자 응답 원문은 노출하지 않는다.
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
            PendingSocialAuthIntent pendingIntent = consumePendingIntent(request, session);
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
            removePendingSocialFlowIfMatching(request);
            redirectPath = resolveFailureRedirect(intent, e);
            if (!e.isUserVisible()) {
                log.error("소셜 로그인 처리 중 내부 오류가 발생했습니다.", e);
            }
        } catch (Exception e) {
            removePendingSocialFlowIfMatching(request);
            // 소셜 인증 응답 원문이나 토큰은 기록하지 않고 예외 종류만 서버 로그에서 확인한다.
            log.error("소셜 로그인 처리 중 예기치 않은 오류가 발생했습니다.", e);
        } finally {
            if (!cleanupOAuthAuthentication(oauthToken, request, response)) {
                // 임시 소셜 인증을 제거하지 못하면 애플리케이션 로그인이나
                // 대기 중인 소셜 흐름으로 진행하지 않는다.
                completedLogin = null;
                redirectPath = "/auth/login?socialError=true";
            }
        }

        if (completedLogin != null) {
            // 임시 소셜 인증 정리가 끝난 뒤 기존 세션을 폐기하여 세션 고정 공격을 막는다.
            try {
                // 항상 실행되는 정리 구간에서 임시 소셜 인증 상태를 먼저 지운 뒤
                // 애플리케이션 인증을 명시적으로 저장한다.
                redirectPath = loginSessionManager.completeLogin(request, response, completedLogin);
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.warn("소셜 로그인 세션 생성에 실패했습니다.", e);
                redirectPath = "/auth/login?socialError=true";
            }
        }
        response.sendRedirect(request.getContextPath() + redirectPath);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        removePendingSocialFlowIfMatching(request);
        clearSecurityContext(request, response);
        response.sendRedirect(request.getContextPath() + "/auth/login?socialError=true");
    }

    private SocialProfile extractSocialProfile(OAuth2AuthenticationToken token) {
        return switch (normalizeRegistrationId(token.getAuthorizedClientRegistrationId())) {
            case KAKAO_REGISTRATION_ID -> extractKakaoProfile(token.getPrincipal());
            case GOOGLE_REGISTRATION_ID -> extractGoogleProfile(token.getPrincipal());
            case NAVER_REGISTRATION_ID -> extractNaverProfile(token.getPrincipal());
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

    private SocialProfile extractNaverProfile(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        if (!"00".equals(stringValue(attributes.get("resultcode")))) {
            throw new SocialAuthException("네이버 회원 정보를 확인할 수 없습니다.");
        }

        Map<?, ?> response = mapValue(attributes.get("response"));
        String providerUserId = response == null ? null : stringValue(response.get("id"));
        if (providerUserId == null
                || providerUserId.length() > NAVER_PROVIDER_USER_ID_MAX_LENGTH) {
            throw new SocialAuthException("유효하지 않은 네이버 회원 식별 정보입니다.");
        }

        /*
         * 네이버는 email_verified 필드를 반환하지 않는다.
         * 이 프로젝트에서는 인증 성공 응답의 이메일을 검증된 이메일로 신뢰한다.
         * 이메일은 신규 가입·연동에서만 필수이며 기존 연결 회원 로그인은 식별자만 사용한다.
         */
        return new SocialProfile(
                NAVER_PROVIDER,
                providerUserId,
                stringValue(response.get("email")),
                true,
                null,
                stringValue(response.get("profile_image")));
    }

    private boolean cleanupOAuthAuthentication(
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean cleanupFailed = false;
        try {
            if (authentication != null) {
                authorizedClientRepository.removeAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication,
                        request,
                        response);
            }
        } catch (RuntimeException e) {
            cleanupFailed = true;
            log.error("소셜 로그인 임시 토큰 정리에 실패했습니다.", e);
        }
        if (cleanupFailed) {
            invalidateSession(request);
            SecurityContextHolder.clearContext();
            return false;
        }
        return clearSecurityContext(request, response);
    }

    private boolean clearSecurityContext(
            HttpServletRequest request,
            HttpServletResponse response) {
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        try {
            SecurityContextHolder.setContext(emptyContext);
            // 애플리케이션 인증은 회원 정보 세션만 사용하므로 임시 소셜 인증은 즉시 제거한다.
            securityContextRepository.saveContext(emptyContext, request, response);
            return true;
        } catch (RuntimeException e) {
            log.error("OAuth temporary authentication removal failed.", e);
            invalidateSession(request);
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // 세션은 이미 사용할 수 없으므로 남은 인증 상태가 없다.
        }
    }

    private void removePendingSocialFlowIfMatching(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        String callbackState = validState(request.getParameter("state"));
        synchronized (session) {
            String pendingState = session.getAttribute(PENDING_SOCIAL_AUTH_STATE)
                    instanceof String state ? validState(state) : null;
            if (callbackState == null || !callbackState.equals(pendingState)) {
                return;
            }
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);
            session.removeAttribute(PENDING_SOCIAL_LINK);
            session.removeAttribute(PENDING_SOCIAL_AUTH_INTENT);
            session.removeAttribute(PENDING_SOCIAL_AUTH_FLOW_ID);
            session.removeAttribute(PENDING_SOCIAL_AUTH_STATE);
        }
    }

    private PendingSocialAuthIntent consumePendingIntent(
            HttpServletRequest request,
            HttpSession session) {
        Object sessionValue;
        synchronized (session) {
            String callbackState = validState(request.getParameter("state"));
            String pendingState = session.getAttribute(PENDING_SOCIAL_AUTH_STATE)
                    instanceof String state ? validState(state) : null;
            if (callbackState == null || !callbackState.equals(pendingState)) {
                throw new SocialAuthException("소셜 로그인 시작 정보를 확인할 수 없습니다.");
            }

            sessionValue = session.getAttribute(PENDING_SOCIAL_AUTH_INTENT);
            if (sessionValue instanceof PendingSocialAuthIntent) {
                // 콜백 재전송이 같은 시작 정보를 다시 사용할 수 없도록 상태값과 함께 소비한다.
                session.removeAttribute(PENDING_SOCIAL_AUTH_INTENT);
                session.removeAttribute(PENDING_SOCIAL_AUTH_FLOW_ID);
                session.removeAttribute(PENDING_SOCIAL_AUTH_STATE);
            }
        }
        if (!(sessionValue instanceof PendingSocialAuthIntent pendingIntent)) {
            throw new SocialAuthException("소셜 로그인 시작 정보를 확인할 수 없습니다.");
        }
        return pendingIntent;
    }

    private String validState(String state) {
        return state == null || state.isBlank() ? null : state;
    }

    private String normalizeRegistrationId(String registrationId) {
        if (registrationId == null) {
            throw new SocialAuthException("현재 지원하지 않는 소셜 로그인 제공자입니다.");
        }
        String normalized = registrationId.trim().toLowerCase(Locale.ROOT);
        if (!KAKAO_REGISTRATION_ID.equals(normalized)
                && !GOOGLE_REGISTRATION_ID.equals(normalized)
                && !NAVER_REGISTRATION_ID.equals(normalized)) {
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
