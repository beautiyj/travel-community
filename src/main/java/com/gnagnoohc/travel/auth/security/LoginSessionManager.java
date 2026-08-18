package com.gnagnoohc.travel.auth.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 애플리케이션의 모든 로그인 완료 처리를 한곳에서 수행한다. 기존 세션을 교체하여
 * 자격 증명, 소셜 인증 상태, 로그인 전 세션 식별자가 인증된 세션에 남지 않게 한다.
 */
@Component
public class LoginSessionManager {

    private static final String LOGIN_MEMBER = "loginMember";

    private final SecurityContextRepository securityContextRepository;
    private final RequestCache requestCache;

    public LoginSessionManager(
            SecurityContextRepository securityContextRepository,
            RequestCache requestCache) {
        this.securityContextRepository = securityContextRepository;
        this.requestCache = requestCache;
    }

    /**
     * 현재 세션을 인증된 세션으로 교체하고 안전한 리다이렉트 대상을 반환한다.
     * 호출자는 세션 관리 오류를 노출하지 않고 일반적인 로그인 실패 응답으로 처리한다.
     */
    public String completeLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            LoginMemberDto loginMember) {
        Authentication authentication = createAuthentication(loginMember);
        HttpSession oldSession = request.getSession(false);
        SavedRequest savedRequest = null;

        // 이미 무효화된 세션 ID를 가진 두 번째 요청은 먼저 성공한 요청 뒤에
        // 또 다른 인증 세션을 만들면 안 된다.
        if (oldSession == null && request.getRequestedSessionId() != null) {
            throw new IllegalStateException("The previous login session is no longer valid.");
        }

        if (oldSession != null) {
            // 읽기·무효화·생성 전환 구간만 동기화한다. 전역 잠금 없이 같은 기존 세션의
            // 동시 제출이 모두 인증 세션을 만드는 것을 막는다.
            synchronized (oldSession) {
                assertSessionUsable(oldSession);
                savedRequest = requestCache.getRequest(request, response);
                oldSession.invalidate();
                return storeAuthenticatedSession(
                        request,
                        response,
                        request.getSession(true),
                        loginMember,
                        authentication,
                        safeRedirect(request, savedRequest, loginMember));
            }
        }

        return storeAuthenticatedSession(
                request,
                response,
                request.getSession(true),
                loginMember,
                authentication,
                defaultRedirect(loginMember));
    }

    private String storeAuthenticatedSession(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession loginSession,
            LoginMemberDto loginMember,
            Authentication authentication,
            String redirectPath) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        try {
            loginSession.setAttribute(LOGIN_MEMBER, loginMember);
            SecurityContextHolder.setContext(context);
            // 로컬·소셜 로그인 완료 처리는 스프링 시큐리티의 일반 인증 필터 밖에서
            // 수행되므로 명시적으로 저장해야 한다.
            securityContextRepository.saveContext(context, request, response);
            return redirectPath;
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            invalidateQuietly(loginSession);
            throw new IllegalStateException("Unable to create the login session.", e);
        }
    }

    private Authentication createAuthentication(LoginMemberDto loginMember) {
        if (loginMember == null) {
            throw new IllegalArgumentException("Login member is required.");
        }

        List<SimpleGrantedAuthority> authorities = switch (
                loginMember.getMemberType() + ":" + loginMember.getMemberRole()) {
            case "0:ADMIN" -> List.of(
                    new SimpleGrantedAuthority("TYPE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "1:USER" -> List.of(
                    new SimpleGrantedAuthority("TYPE_GENERAL"),
                    new SimpleGrantedAuthority("ROLE_USER"));
            case "2:USER" -> List.of(
                    new SimpleGrantedAuthority("TYPE_BUSINESS"),
                    new SimpleGrantedAuthority("ROLE_USER"));
            case "2:BUSINESS" -> List.of(
                    new SimpleGrantedAuthority("TYPE_BUSINESS"),
                    new SimpleGrantedAuthority("ROLE_BUSINESS"));
            default -> throw new IllegalArgumentException("Unsupported member type and role.");
        };

        // 자격 증명과 요청 상세 정보는 의도적으로 저장하지 않는다.
        return new UsernamePasswordAuthenticationToken(loginMember, null, authorities);
    }

    private String safeRedirect(
            HttpServletRequest request,
            SavedRequest savedRequest,
            LoginMemberDto loginMember) {
        if (savedRequest == null || !"GET".equalsIgnoreCase(savedRequest.getMethod())) {
            return defaultRedirect(loginMember);
        }

        String target = safeInternalPath(request, savedRequest.getRedirectUrl());
        if (target == null || isUnsafeSavedPath(target)) {
            return defaultRedirect(loginMember);
        }
        return target;
    }

    private String safeInternalPath(HttpServletRequest request, String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(redirectUrl);
            if (uri.isAbsolute()) {
                if (!request.getScheme().equalsIgnoreCase(uri.getScheme())
                        || !request.getServerName().equalsIgnoreCase(uri.getHost())
                        || normalizedPort(request) != normalizedPort(uri)) {
                    return null;
                }
            } else if (uri.getRawAuthority() != null) {
                return null;
            }

            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return null;
            }
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                if (path.equals(contextPath)) {
                    path = "/";
                } else if (path.startsWith(contextPath + "/")) {
                    path = path.substring(contextPath.length());
                } else {
                    return null;
                }
            }
            String query = removeRequestCacheMarker(uri.getRawQuery());
            return query == null || query.isEmpty() ? path : path + "?" + query;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String removeRequestCacheMarker(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return rawQuery;
        }
        return java.util.Arrays.stream(rawQuery.split("&"))
                // 요청 저장소가 저장된 요청과 일반 조회 요청을 구분하려고
                // 붙인 표식이며 사용자 입력이 아니다.
                .filter(parameter -> !"continue".equals(parameter))
                .collect(java.util.stream.Collectors.joining("&"));
    }

    private int normalizedPort(HttpServletRequest request) {
        if (request.getServerPort() > 0) {
            return request.getServerPort();
        }
        return "https".equalsIgnoreCase(request.getScheme()) ? 443 : 80;
    }

    private int normalizedPort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private boolean isUnsafeSavedPath(String pathAndQuery) {
        String path = pathAndQuery.substring(0, pathAndQuery.indexOf('?') < 0
                ? pathAndQuery.length()
                : pathAndQuery.indexOf('?'));
        return path.startsWith("/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/upload/")
                || path.startsWith("/uploads/")
                || path.startsWith("/api/")
                || path.equals("/reservations/availability")
                || path.matches("/reservations/[^/]+/refund-preview")
                || path.startsWith("/payments/kakao/")
                || path.startsWith("/payments/toss/")
                || path.startsWith("/payments/vcard/")
                || path.startsWith("/payments/bank/")
                || path.matches("/admin/business-applications/[^/]+/document");
    }

    private String defaultRedirect(LoginMemberDto loginMember) {
        return switch (loginMember.getMemberType() + ":" + loginMember.getMemberRole()) {
            case "0:ADMIN" -> "/admin";
            case "2:BUSINESS" -> "/business/dashboard";
            case "1:USER", "2:USER" -> "/";
            default -> throw new IllegalArgumentException("Unsupported member type and role.");
        };
    }

    private void assertSessionUsable(HttpSession session) {
        try {
            session.getAttributeNames();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("The previous login session is no longer valid.", e);
        }
    }

    private void invalidateQuietly(HttpSession session) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // 동시 요청이 이미 이 새 세션을 무효화했을 수 있다.
        }
    }
}
