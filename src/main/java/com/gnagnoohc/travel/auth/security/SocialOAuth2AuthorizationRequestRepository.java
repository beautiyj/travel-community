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
 * 애플리케이션이 시작한 소셜 인증 흐름과 Spring Security의 OAuth state를 결합한다.
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
