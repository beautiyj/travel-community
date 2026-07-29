package com.gnagnoohc.travel.auth.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
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
import com.gnagnoohc.travel.auth.dto.LocalLoginResult;
import com.gnagnoohc.travel.auth.dto.PendingSocialAuthIntent;
import com.gnagnoohc.travel.auth.dto.PendingSocialLink;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.dto.SocialLinkRequest;
import com.gnagnoohc.travel.auth.dto.SocialSignupRequest;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;
import com.gnagnoohc.travel.auth.service.AuthService;
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
    private static final String PENDING_SOCIAL_LINK = "pendingSocialLink";
    private static final String PENDING_SOCIAL_AUTH_INTENT = "pendingSocialAuthIntent";
    private static final String PENDING_SOCIAL_AUTH_FLOW_ID = "pendingSocialAuthFlowId";
    private static final String PENDING_SOCIAL_AUTH_STATE = "pendingSocialAuthState";
    private static final String LOGIN_INTENT = "LOGIN";
    private static final String SIGNUP_INTENT = "SIGNUP";
    private static final int AUTH_INTENT_VALID_MINUTES = 10;

    private final SocialAuthService socialAuthService;
    private final AuthService authService;

    /**
     * 기존 화면 경로를 유지하면서 실제 OAuth2 Client 표준 시작 경로로 연결한다.
     */
    @GetMapping("/kakao")
    public String startKakaoLogin(HttpSession session) {
        // 새 로그인을 시작할 때 이전 제공자의 미완료 가입 정보를 재사용하지 않는다.
        return startSocialAuth(session, "kakao", LOGIN_INTENT);
    }

    @GetMapping("/google")
    public String startGoogleLogin(HttpSession session) {
        // Google 로그인도 Kakao와 동일하게 이전의 미완료 소셜 가입 정보를 제거하고 시작한다.
        return startSocialAuth(session, "google", LOGIN_INTENT);
    }

    @GetMapping("/naver")
    public String startNaverLogin(HttpSession session) {
        return startSocialAuth(session, "naver", LOGIN_INTENT);
    }

    @GetMapping("/kakao/signup")
    public String startKakaoSignup(HttpSession session) {
        return startSocialAuth(session, "kakao", SIGNUP_INTENT);
    }

    @GetMapping("/google/signup")
    public String startGoogleSignup(HttpSession session) {
        return startSocialAuth(session, "google", SIGNUP_INTENT);
    }

    @GetMapping("/naver/signup")
    public String startNaverSignup(HttpSession session) {
        return startSocialAuth(session, "naver", SIGNUP_INTENT);
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

    /**
     * 기존 계정 확인 안내와 비밀번호 연동을 한 화면에서 처리한다.
     * 원본 아이디와 회원 ID는 모델에 전달하지 않고 서버 세션의 후보 정보만 사용한다.
     */
    @GetMapping("/social/link")
    public String socialLinkPage(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        PendingSocialLink pendingLink = getPendingSocialLink(session);
        if (pendingLink == null) {
            return redirectToSocialLogin(redirectAttributes);
        }

        addPendingLinkToModel(model, pendingLink);
        if (!model.containsAttribute("socialLinkRequest")) {
            model.addAttribute("socialLinkRequest", new SocialLinkRequest());
        }
        return "auth/social-link";
    }

    @PostMapping("/social/link")
    public String linkSocialAccount(
            @Valid @ModelAttribute("socialLinkRequest") SocialLinkRequest socialLinkRequest,
            BindingResult bindingResult,
            @RequestParam(value = "linkNonce", required = false) String linkNonce,
            HttpServletRequest request,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        PendingSocialLink pendingLink = getPendingSocialLink(session);
        if (pendingLink == null) {
            return redirectToSocialLogin(redirectAttributes);
        }

        addPendingLinkToModel(model, pendingLink);
        if (!matchesNonce(pendingLink.linkNonce(), linkNonce)) {
            model.addAttribute(
                    "socialLinkError",
                    "유효하지 않은 소셜 연동 요청입니다. 화면을 새로고침한 뒤 다시 시도해 주세요.");
            return "auth/social-link";
        }
        if (bindingResult.hasErrors()) {
            addFieldErrorsToModel(bindingResult, model);
            return "auth/social-link";
        }

        try {
            // 비밀번호 실패 횟수는 연동 저장과 분리된 트랜잭션에서 먼저 확정한다.
            LocalLoginResult loginResult = authService.authenticateSocialLinkCandidate(
                    pendingLink.candidateMemberId(),
                    socialLinkRequest.getUsername(),
                    socialLinkRequest.getPassword());
            if (loginResult.status() == LocalLoginResult.LoginStatus.LOCKED) {
                model.addAttribute(
                        "socialLinkError",
                        "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.");
                return "auth/social-link";
            }
            if (loginResult.status() == LocalLoginResult.LoginStatus.INVALID_CREDENTIALS) {
                model.addAttribute("socialLinkError", "아이디 또는 비밀번호를 확인해 주세요.");
                return "auth/social-link";
            }
            if (loginResult.loginMember() == null
                    || loginResult.loginMember().getMemberId() != pendingLink.candidateMemberId()) {
                throw new SocialAuthException("연동할 회원 정보가 일치하지 않습니다.");
            }

            LoginMemberDto loginMember = socialAuthService.linkSocialAccount(
                    pendingLink,
                    loginResult.loginMember().getMemberId());
            return completeLogin(request, loginMember);
        } catch (PessimisticLockingFailureException e) {
            // DB 트랜잭션은 이미 롤백됐으므로 pending을 유지하고 사용자가 수동으로 다시 시도하게 한다.
            log.warn("소셜 계정 연동 중 DB 잠금 획득에 실패했습니다.");
            model.addAttribute(
                    "socialLinkError",
                    "동시 처리 중입니다. 잠시 후 다시 시도해 주세요.");
            return "auth/social-link";
        } catch (SocialAuthException e) {
            if (!e.isUserVisible()) {
                log.error("소셜 계정 연동 중 내부 오류가 발생했습니다.", e);
            }
            model.addAttribute(
                    "socialLinkError",
                    e.isUserVisible()
                            ? e.getMessage()
                            : "소셜 계정 연동 중 오류가 발생했습니다.");
            return "auth/social-link";
        } catch (Exception e) {
            log.error("소셜 계정 연동 중 예기치 않은 오류가 발생했습니다.", e);
            model.addAttribute("socialLinkError", "소셜 계정 연동 중 오류가 발생했습니다.");
            return "auth/social-link";
        }
    }

    @PostMapping("/social/link/cancel")
    public String cancelSocialLink(
            @RequestParam(value = "linkNonce", required = false) String linkNonce,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        PendingSocialLink pendingLink = getPendingSocialLink(session);
        if (pendingLink == null) {
            return redirectToSocialLogin(redirectAttributes);
        }
        if (!matchesNonce(pendingLink.linkNonce(), linkNonce)) {
            redirectAttributes.addFlashAttribute(
                    "socialLinkError",
                    "유효하지 않은 소셜 연동 취소 요청입니다.");
            return "redirect:/auth/social/link";
        }

        session.removeAttribute(PENDING_SOCIAL_LINK);
        return "redirect:/auth/login";
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

    private PendingSocialLink getPendingSocialLink(HttpSession session) {
        Object sessionValue = session.getAttribute(PENDING_SOCIAL_LINK);
        if (sessionValue instanceof PendingSocialLink pendingLink) {
            if (!pendingLink.isExpired()) {
                return pendingLink;
            }
            session.removeAttribute(PENDING_SOCIAL_LINK);
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

    private void addPendingLinkToModel(
            Model model,
            PendingSocialLink pendingLink) {
        model.addAttribute("socialProviderName", getSocialProviderName(pendingLink.provider()));
        model.addAttribute("maskedUsername", pendingLink.maskedUsername());
        model.addAttribute("socialEmail", pendingLink.email());
        model.addAttribute("socialLinkNonce", pendingLink.linkNonce());
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
                "socialError",
                "소셜 인증 정보가 없거나 만료됐습니다. 다시 로그인해 주세요.");
        return "redirect:/auth/login";
    }

    private String getSocialProviderName(String provider) {
        // 화면에는 DB provider 코드 대신 사용자용 이름을 전달한다.
        return switch (provider) {
            case "KAKAO" -> "카카오";
            case "GOOGLE" -> "구글";
            case "NAVER" -> "네이버";
            default -> "소셜";
        };
    }

    private String startSocialAuth(
            HttpSession session,
            String registrationId,
            String intent) {
        String normalizedRegistrationId = normalizeRegistrationId(registrationId);
        String normalizedIntent = normalizeIntent(intent);
        String flowId = UUID.randomUUID().toString();

        // 새 OAuth 시작은 이전에 완료되지 않은 흐름을 폐기하고 현재 요청으로 교체한다.
        // Spring Security가 새 authorization request의 state를 저장하므로 이전 callback은 검증에 실패한다.
        synchronized (session) {
            session.removeAttribute(PENDING_SOCIAL_AUTH_INTENT);
            session.removeAttribute(PENDING_SOCIAL_SIGNUP);
            session.removeAttribute(PENDING_SOCIAL_LINK);
            session.removeAttribute(PENDING_SOCIAL_AUTH_STATE);
            session.setAttribute(PENDING_SOCIAL_AUTH_FLOW_ID, flowId);
            session.setAttribute(
                    PENDING_SOCIAL_AUTH_INTENT,
                    new PendingSocialAuthIntent(
                            normalizedIntent,
                            normalizedRegistrationId,
                            LocalDateTime.now().plusMinutes(AUTH_INTENT_VALID_MINUTES)));
        }
        return "redirect:/oauth2/authorization/" + normalizedRegistrationId + "?flow=" + flowId;
    }

    private String normalizeRegistrationId(String registrationId) {
        if (registrationId == null) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
        String normalized = registrationId.trim().toLowerCase(Locale.ROOT);
        if (!"kakao".equals(normalized)
                && !"google".equals(normalized)
                && !"naver".equals(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
        return normalized;
    }

    private String normalizeIntent(String intent) {
        if (LOGIN_INTENT.equals(intent) || SIGNUP_INTENT.equals(intent)) {
            return intent;
        }
        throw new IllegalArgumentException("지원하지 않는 소셜 인증 흐름입니다.");
    }

    private boolean matchesSignupNonce(String savedNonce, String requestNonce) {
        return matchesNonce(savedNonce, requestNonce);
    }

    private boolean matchesNonce(String savedNonce, String requestNonce) {
        if (savedNonce == null || requestNonce == null) {
            return false;
        }
        // 문자열 길이나 일치 위치에 따른 비교 시간 차이를 줄여 세션 nonce를 검증한다.
        return MessageDigest.isEqual(
                savedNonce.getBytes(StandardCharsets.UTF_8),
                requestNonce.getBytes(StandardCharsets.UTF_8));
    }
}
