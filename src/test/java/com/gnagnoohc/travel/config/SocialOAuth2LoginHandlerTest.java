package com.gnagnoohc.travel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.SecurityContextRepository;

import com.gnagnoohc.travel.auth.controller.SocialOAuth2LoginHandler;
import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.service.SocialAuthService;

@ExtendWith(MockitoExtension.class)
class SocialOAuth2LoginHandlerTest {

    @Mock
    private SocialAuthService socialAuthService;

    @Mock
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Mock
    private SecurityContextRepository securityContextRepository;

    private SocialOAuth2LoginHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SocialOAuth2LoginHandler(
                socialAuthService,
                authorizedClientRepository,
                securityContextRepository);
    }

    @Test
    void 기존_활성회원은_우리_세션으로_로그인하고_OAuth_상태를_정리한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = kakaoAuthentication(verifiedAttributes());
        when(socialAuthService.findSocialLoginMember("KAKAO", "12345"))
                .thenReturn(new LoginMemberDto(7, "여행자", "USER"));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/", response.getRedirectedUrl());
        LoginMemberDto loginMember = assertInstanceOf(
                LoginMemberDto.class,
                request.getSession().getAttribute("loginMember"));
        assertEquals(7, loginMember.getMemberId());
        assertNull(request.getSession().getAttribute("pendingSocialSignup"));
        verify(authorizedClientRepository).removeAuthorizedClient(
                "kakao", authentication, request, response);
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    void 신규회원은_검증된_카카오값만_가입대기_세션에_저장한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = kakaoAuthentication(verifiedAttributes());
        when(socialAuthService.findSocialLoginMember("KAKAO", "12345"))
                .thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/auth/social/signup", response.getRedirectedUrl());
        PendingSocialSignup pendingSignup = assertInstanceOf(
                PendingSocialSignup.class,
                request.getSession().getAttribute("pendingSocialSignup"));
        assertEquals("KAKAO", pendingSignup.provider());
        assertEquals("12345", pendingSignup.providerUserId());
        assertEquals("member@example.com", pendingSignup.email());
        assertEquals("카카오닉네임", pendingSignup.providerNickname());
        assertNotNull(pendingSignup.signupNonce());
        assertNull(request.getSession().getAttribute("loginMember"));
    }

    @Test
    void 이메일이_검증되지_않으면_가입대기_정보를_남기지_않는다() throws Exception {
        Map<String, Object> attributes = verifiedAttributes();
        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        account.put("is_email_verified", false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = kakaoAuthentication(attributes);
        when(socialAuthService.findSocialLoginMember("KAKAO", "12345"))
                .thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/auth/login?socialError=true", response.getRedirectedUrl());
        assertNull(request.getSession().getAttribute("pendingSocialSignup"));
    }

    @Test
    void 미지원_provider는_서비스를_호출하지_않고_거부한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        verifiedAttributes(),
                        "id"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                "google");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/auth/login?socialError=true", response.getRedirectedUrl());
        verify(socialAuthService, never()).findSocialLoginMember(any(), any());
        verify(authorizedClientRepository).removeAuthorizedClient(
                "google", authentication, request, response);
    }

    @Test
    void OAuth_실패는_이전_가입정보와_Spring_인증을_정리한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("pendingSocialSignup", "stale-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error("invalid_state")));

        assertEquals("/auth/login?socialError=true", response.getRedirectedUrl());
        assertNull(request.getSession().getAttribute("pendingSocialSignup"));
        verify(securityContextRepository).saveContext(any(), any(), any());
        verify(authorizedClientRepository, never()).removeAuthorizedClient(
                any(), any(), any(), any());
    }

    private OAuth2AuthenticationToken kakaoAuthentication(Map<String, Object> attributes) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new OAuth2AuthenticationToken(
                new DefaultOAuth2User(authorities, attributes, "id"),
                authorities,
                "kakao");
    }

    private Map<String, Object> verifiedAttributes() {
        return new java.util.HashMap<>(Map.of(
                "id", 12345L,
                "kakao_account", new java.util.HashMap<>(Map.of(
                        "email", "member@example.com",
                        "is_email_valid", true,
                        "is_email_verified", true,
                        "profile", Map.of(
                                "nickname", "카카오닉네임",
                                "profile_image_url", "https://example.com/profile.jpg")))));
    }
}
