package com.gnagnoohc.travel.auth.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gnagnoohc.travel.auth.client.KakaoApiClient;
import com.gnagnoohc.travel.auth.dto.KakaoUserInfo;
import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.dto.SocialSignupRequest;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;
import com.gnagnoohc.travel.auth.service.SocialAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 카카오 로그인 시작·콜백과 제공자 공통 신규 가입 화면의 세션 흐름을 담당한다.
 * 실제 외부 HTTP 호출과 DB 저장은 각각 Client와 Service로 위임한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class SocialAuthController {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthController.class);
    private static final String PENDING_SOCIAL_SIGNUP = "pendingSocialSignup";
    private static final String KAKAO_OAUTH_STATE = "kakaoOAuthState";
    private static final int STATE_VALID_MINUTES = 5;
    private static final int SIGNUP_VALID_MINUTES = 10;

    private final KakaoApiClient kakaoApiClient;
    private final SocialAuthService socialAuthService;

    /**
     * 카카오 인가 요청과 콜백이 같은 세션에서 시작됐는지 확인할 state를 저장한다.
     */
    @GetMapping("/kakao")
    public String startKakaoLogin(
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        // 새 로그인을 시작할 때 이전 제공자의 미완료 가입 정보를 재사용하지 않는다.
        session.removeAttribute(PENDING_SOCIAL_SIGNUP);

        KakaoOAuthState oauthState = new KakaoOAuthState(
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusMinutes(STATE_VALID_MINUTES));
        session.setAttribute(KAKAO_OAUTH_STATE, oauthState);

        try {
            return "redirect:" + kakaoApiClient.createAuthorizationUri(oauthState.value());
        } catch (IllegalStateException e) {
            session.removeAttribute(KAKAO_OAUTH_STATE);
            redirectAttributes.addFlashAttribute("kakaoError", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    /**
     * state 검증 뒤 Client와 Service를 순서대로 호출해 로그인 또는 신규가입으로 분기한다.
     */
    @GetMapping("/kakao/callback")
    public String kakaoCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        KakaoOAuthState savedState = getAndRemoveOAuthState(session);

        if (savedState == null || !savedState.matches(state)) {
            redirectAttributes.addFlashAttribute(
                    "kakaoError",
                    "유효하지 않거나 만료된 카카오 로그인 요청입니다. 다시 시도해 주세요.");
            return "redirect:/auth/login";
        }
        if (error != null) {
            redirectAttributes.addFlashAttribute(
                    "kakaoError",
                    "카카오 로그인이 취소됐거나 인증에 실패했습니다.");
            return "redirect:/auth/login";
        }
        if (code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "kakaoError",
                    "카카오 인가 코드를 받지 못했습니다. 다시 시도해 주세요.");
            return "redirect:/auth/login";
        }

        try {
            // 외부 카카오 호출은 Service의 DB 트랜잭션이 시작되기 전에 끝낸다.
            KakaoUserInfo kakaoUserInfo = kakaoApiClient.requestUserInfo(code);
            LoginMemberDto loginMember = socialAuthService
                    .findKakaoLoginMember(kakaoUserInfo.providerUserId());

            if (loginMember != null) {
                return completeLogin(request, loginMember);
            }

            if (!kakaoUserInfo.hasVerifiedEmail()) {
                redirectAttributes.addFlashAttribute(
                        "kakaoError",
                        "카카오 계정의 검증된 이메일 제공 동의가 필요합니다.");
                return "redirect:/auth/login";
            }

            // OAuth 인증 성공으로 가입 권한이 생기는 시점에 세션 ID를 바꿔 세션 고정을 차단한다.
            request.changeSessionId();
            PendingSocialSignup pendingSignup = new PendingSocialSignup(
                    "KAKAO",
                    kakaoUserInfo.providerUserId(),
                    kakaoUserInfo.email(),
                    kakaoUserInfo.nickname(),
                    kakaoUserInfo.profileImageUrl(),
                    // 전역 CSRF가 비활성화된 현재 구조에서 카카오 가입 POST만 별도로 검증한다.
                    UUID.randomUUID().toString(),
                    true,
                    LocalDateTime.now().plusMinutes(SIGNUP_VALID_MINUTES));
            session.setAttribute(PENDING_SOCIAL_SIGNUP, pendingSignup);
            return "redirect:/auth/social/signup";
        } catch (SocialAuthException e) {
            addKakaoError(redirectAttributes, e, "카카오 로그인 처리 중 오류가 발생했습니다.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            // 예상하지 못한 오류의 상세 내용은 사용자에게 노출하지 않는다.
            log.error("카카오 로그인 처리 중 예기치 않은 오류가 발생했습니다.", e);
            redirectAttributes.addFlashAttribute(
                    "kakaoError", "카카오 로그인 처리 중 오류가 발생했습니다.");
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/social/signup")
    public String socialSignupPage(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        PendingSocialSignup pendingSignup = getPendingSocialSignup(session);
        if (pendingSignup == null) {
            return redirectToSocialLogin(redirectAttributes);
        }

        addPendingSignupToModel(model, pendingSignup);
        if (!model.containsAttribute("socialSignupRequest")) {
            model.addAttribute("socialSignupRequest", new SocialSignupRequest());
        }
        return "auth/social-signup";
    }

    @PostMapping("/social/signup")
    public String socialSignup(
            @Valid @ModelAttribute("socialSignupRequest") SocialSignupRequest socialSignupRequest,
            BindingResult bindingResult,
            @RequestParam(value = "signupNonce", required = false) String signupNonce,
            HttpServletRequest request,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        PendingSocialSignup pendingSignup = getPendingSocialSignup(session);
        if (pendingSignup == null) {
            return redirectToSocialLogin(redirectAttributes);
        }

        addPendingSignupToModel(model, pendingSignup);
        if (!matchesSignupNonce(pendingSignup.signupNonce(), signupNonce)) {
            // 누락·위조된 POST는 DB 서비스에 도달시키지 않고 같은 화면에서 다시 제출하게 한다.
            model.addAttribute(
                    "socialSignupError",
                    "유효하지 않은 소셜 회원가입 요청입니다. 화면을 새로고침한 뒤 다시 시도해 주세요.");
            return "auth/social-signup";
        }
        if (bindingResult.hasErrors()) {
            addFieldErrorsToModel(bindingResult, model);
            return "auth/social-signup";
        }

        try {
            LoginMemberDto loginMember = socialAuthService.registerSocialMember(
                    pendingSignup,
                    socialSignupRequest);
            return completeLogin(request, loginMember);
        } catch (SocialAuthException e) {
            if (!e.isUserVisible()) {
                log.error("소셜 회원가입 처리 중 내부 오류가 발생했습니다.", e);
            }
            model.addAttribute(
                    "socialSignupError",
                    e.isUserVisible()
                            ? e.getMessage()
                            : "소셜 회원가입 처리 중 오류가 발생했습니다.");
            return "auth/social-signup";
        } catch (Exception e) {
            log.error("소셜 회원가입 처리 중 예기치 않은 오류가 발생했습니다.", e);
            model.addAttribute(
                    "socialSignupError",
                    "소셜 회원가입 처리 중 오류가 발생했습니다.");
            return "auth/social-signup";
        }
    }

    private KakaoOAuthState getAndRemoveOAuthState(HttpSession session) {
        Object sessionValue = session.getAttribute(KAKAO_OAUTH_STATE);
        session.removeAttribute(KAKAO_OAUTH_STATE);
        if (sessionValue instanceof KakaoOAuthState oauthState) {
            return oauthState;
        }
        return null;
    }

    private PendingSocialSignup getPendingSocialSignup(HttpSession session) {
        Object sessionValue = session.getAttribute(PENDING_SOCIAL_SIGNUP);
        if (sessionValue instanceof PendingSocialSignup pendingSignup) {
            if (!pendingSignup.isExpired()) {
                return pendingSignup;
            }
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);
        }
        return null;
    }

    private void addPendingSignupToModel(
            Model model,
            PendingSocialSignup pendingSignup) {
        model.addAttribute("socialProviderName", getSocialProviderName(pendingSignup.provider()));
        model.addAttribute("socialEmail", pendingSignup.email());
        model.addAttribute("socialProfileImageUrl", pendingSignup.profileImageUrl());
        model.addAttribute("socialSignupNonce", pendingSignup.signupNonce());
    }

    private void addFieldErrorsToModel(
            BindingResult bindingResult,
            Model model) {
        Map<String, String> fieldErrors = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (firstMessage, ignoredMessage) -> firstMessage));
        model.addAttribute("errors", fieldErrors);
    }

    /**
     * 기존 로컬 로그인과 같은 세션 키를 사용하고 이전 세션은 폐기한다.
     */
    private String completeLogin(
            HttpServletRequest request,
            LoginMemberDto loginMember) {
        HttpSession previousSession = request.getSession(false);
        if (previousSession != null) {
            previousSession.invalidate();
        }

        HttpSession loginSession = request.getSession(true);
        loginSession.setAttribute("loginMember", loginMember);
        return "redirect:/";
    }

    private String redirectToSocialLogin(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "kakaoError",
                "소셜 인증 정보가 없거나 만료됐습니다. 다시 로그인해 주세요.");
        return "redirect:/auth/login";
    }

    private String getSocialProviderName(String provider) {
        // 현재 실제 연동 제공자는 카카오뿐이며, 화면에는 내부 코드 대신 사용자용 이름을 전달한다.
        return "KAKAO".equals(provider) ? "카카오" : "소셜";
    }

    private void addKakaoError(
            RedirectAttributes redirectAttributes,
            SocialAuthException exception,
            String internalErrorMessage) {
        if (!exception.isUserVisible()) {
            log.error("카카오 인증 처리 중 내부 오류가 발생했습니다.", exception);
        }
        redirectAttributes.addFlashAttribute(
                "kakaoError",
                exception.isUserVisible() ? exception.getMessage() : internalErrorMessage);
    }

    private boolean matchesSignupNonce(String savedNonce, String requestNonce) {
        if (savedNonce == null || requestNonce == null) {
            return false;
        }
        // 문자열 길이나 일치 위치에 따른 비교 시간 차이를 줄여 세션 nonce를 검증한다.
        return MessageDigest.isEqual(
                savedNonce.getBytes(StandardCharsets.UTF_8),
                requestNonce.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * state는 한 번만 사용하고 짧은 시간 안에 돌아온 콜백만 허용한다.
     */
    private record KakaoOAuthState(
            String value,
            LocalDateTime expiresAt) {

        private boolean matches(String callbackState) {
            return callbackState != null
                    && value.equals(callbackState)
                    && expiresAt != null
                    && LocalDateTime.now().isBefore(expiresAt);
        }
    }
}
