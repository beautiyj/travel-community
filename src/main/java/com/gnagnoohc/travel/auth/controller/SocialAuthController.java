package com.gnagnoohc.travel.auth.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
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
 * 소셜 로그인 시작과 제공자 공통 신규 가입 화면의 세션 흐름을 담당한다.
 * OAuth callback은 Spring Security 필터와 SocialOAuth2LoginHandler가 처리한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class SocialAuthController {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthController.class);
    private static final String PENDING_SOCIAL_SIGNUP = "pendingSocialSignup";

    private final SocialAuthService socialAuthService;

    /**
     * 기존 화면 경로를 유지하면서 실제 OAuth2 Client 표준 시작 경로로 연결한다.
     */
    @GetMapping("/kakao")
    public String startKakaoLogin(HttpSession session) {
        // 새 로그인을 시작할 때 이전 제공자의 미완료 가입 정보를 재사용하지 않는다.
        session.removeAttribute(PENDING_SOCIAL_SIGNUP);
        return "redirect:/oauth2/authorization/kakao";
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

    private boolean matchesSignupNonce(String savedNonce, String requestNonce) {
        if (savedNonce == null || requestNonce == null) {
            return false;
        }
        // 문자열 길이나 일치 위치에 따른 비교 시간 차이를 줄여 세션 nonce를 검증한다.
        return MessageDigest.isEqual(
                savedNonce.getBytes(StandardCharsets.UTF_8),
                requestNonce.getBytes(StandardCharsets.UTF_8));
    }
}
