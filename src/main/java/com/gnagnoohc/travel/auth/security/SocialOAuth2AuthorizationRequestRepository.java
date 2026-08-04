package com.gnagnoohc.travel.auth.security;

import java.util.Locale;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import com.gnagnoohc.travel.auth.dto.PendingSocialAuthIntent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 소셜 인증 시작 의도와 Spring Security가 생성한 OAuth 요청/state를 같은 세션 흐름으로 묶는다.
 * <p>
 * Spring Security의 기본 저장소는 OAuth 요청과 state 저장·복원을 담당한다. 이 래퍼는 그 전에
 * 우리 서비스가 만든 flow ID, LOGIN/SIGNUP 의도, 제공자가 모두 일치하는지 추가로 검사한다.
 * 따라서 다른 탭의 오래된 시작 요청이나 공격자가 만든 callback이 현재 가입/로그인 의도를
 * 소비하지 못한다. 실제 callback state의 최종 소비 검사는 로그인 성공 핸들러와 함께 이뤄진다.
 */
public class SocialOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String PENDING_SOCIAL_AUTH_FLOW_ID = "pendingSocialAuthFlowId";
    public static final String PENDING_SOCIAL_AUTH_STATE = "pendingSocialAuthState";
    public static final String PENDING_SOCIAL_AUTH_INTENT = "pendingSocialAuthIntent";

    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate;

    public SocialOAuth2AuthorizationRequestRepository() {
        this(new HttpSessionOAuth2AuthorizationRequestRepository());
    }

    SocialOAuth2AuthorizationRequestRepository(
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        // /oauth2/authorization/{provider} 진입 시 Spring Security가 만든 state를
        // 컨트롤러가 먼저 세션에 기록한 동일 flow에만 연결한다.
        String requestFlowId = nonBlank(request.getParameter("flow"));
        String requestRegistrationId = registrationIdFrom(request);
        synchronized (session) {
            String pendingFlowId = stringValue(
                    session.getAttribute(PENDING_SOCIAL_AUTH_FLOW_ID));
            Object pendingValue = session.getAttribute(PENDING_SOCIAL_AUTH_INTENT);
            if (requestFlowId == null
                    || !requestFlowId.equals(pendingFlowId)
                    || !(pendingValue instanceof PendingSocialAuthIntent pendingIntent)
                    || pendingIntent.isExpired()
                    || requestRegistrationId == null
                    || !requestRegistrationId.equals(
                            normalizedRegistrationId(pendingIntent.registrationId()))
                    || nonBlank(authorizationRequest.getState()) == null) {
                return;
            }

            session.setAttribute(PENDING_SOCIAL_AUTH_STATE, authorizationRequest.getState());
            delegate.saveAuthorizationRequest(authorizationRequest, request, response);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        // 기본 저장소에 삭제를 위임하기 전에 callback state와 저장 state를 비교한다.
        // 불일치하면 null을 반환하여 Spring Security 인증 자체가 성공하지 못하게 한다.
        String callbackState = exactNonBlank(request.getParameter("state"));
        synchronized (session) {
            OAuth2AuthorizationRequest storedRequest =
                    delegate.loadAuthorizationRequest(request);
            String storedState = storedRequest == null
                    ? null
                    : exactNonBlank(storedRequest.getState());
            if (callbackState == null || !callbackState.equals(storedState)) {
                return null;
            }
            return delegate.removeAuthorizationRequest(request, response);
        }
    }

    private String registrationIdFrom(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return null;
        }
        int lastSlash = requestUri.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == requestUri.length() - 1) {
            return null;
        }
        return normalizedRegistrationId(requestUri.substring(lastSlash + 1));
    }

    private String normalizedRegistrationId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String nonBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String exactNonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String stringValue(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
