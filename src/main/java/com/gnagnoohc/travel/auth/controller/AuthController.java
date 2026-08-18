package com.gnagnoohc.travel.auth.controller;

import com.gnagnoohc.travel.auth.dto.*;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.exception.SignupException;
import com.gnagnoohc.travel.auth.security.LoginSessionManager;
import com.gnagnoohc.travel.auth.service.AuthService;
import com.gnagnoohc.travel.auth.validation.LocalUsernamePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 로컬 로그인·회원가입·계정 찾기 화면의 웹 요청 흐름을 연결한다.
 * <p>
 * 이 클래스는 요청값과 세션을 다루고 반환할 뷰/리다이렉트를 정한다. 비밀번호 검증,
 * 이메일 인증의 유효성 확인, 회원 저장처럼 데이터 상태를 바꾸는 규칙은
 * 서비스 계층에 맡긴다. 스프링 MVC는 매핑, 입력 객체 바인딩과 유효성 검증을
	 * 수행하지만, 로그인 세션 생성은 명시적인 로그인 처리에서만 직접 책임진다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);
	private static final String VERIFIED_MEMBER_REACTIVATION = "verifiedMemberReactivation";

	private final AuthService service;
	private final LoginSessionManager loginSessionManager;

	// 로그인
	@GetMapping("/login")
	public String loginPage(HttpServletRequest request) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		return "auth/login";
	}

	/**
	 * 입력값 형식 확인 → 서비스의 DB 인증 → 기존 세션 폐기 → 새 로그인 세션 생성 순서로 처리한다.
	 * 서비스가 실패 횟수나 잠금 상태를 변경하므로 인증 호출 자체는 트랜잭션 안에서 실행된다.
	 */
	@PostMapping("/login")
	public String login(@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "password", required = false) String password, HttpServletRequest request,
			HttpServletResponse response, Model model) {
		// 입력하지 않은 항목은 로그인 처리 전에 확인해 해당 입력칸에 안내한다.
		boolean usernameMissing = username == null || username.isEmpty();
		boolean passwordBlank = password == null || password.isBlank();
		if (usernameMissing) {
			model.addAttribute("usernameError", "아이디를 입력해주세요.");
			if (passwordBlank) {
				model.addAttribute("passwordError", "비밀번호를 입력해주세요.");
			}
			return "auth/login";
		}
		if (!LocalUsernamePolicy.isValid(username)) {
			model.addAttribute("usernameError", LocalUsernamePolicy.MESSAGE);
			if (passwordBlank) {
				model.addAttribute("passwordError", "비밀번호를 입력해주세요.");
			}
			return "auth/login";
		}
		if (passwordBlank) {
			model.addAttribute("passwordError", "비밀번호를 입력해주세요.");
			return "auth/login";
		}

		LocalLoginResult loginResult;
		try {
			loginResult = service.authenticateLocal(username, password);
		} catch (PessimisticLockingFailureException e) {
			// DB가 이 로그인 요청을 데드락 victim으로 선택하면 자격 증명 오류와 구분해 수동 재시도를 안내한다.
			log.warn("로컬 로그인 중 DB 잠금 획득에 실패했습니다.");
			model.addAttribute("usernameError", "동시 처리 중입니다. 잠시 후 다시 시도해 주세요.");
			return "auth/login";
		}
		switch (loginResult.status()) {
		case LOCKED:
			return "redirect:/auth/login?locked";
		case INVALID_CREDENTIALS:
			return "redirect:/auth/login?error";
		case SUCCESS:
			break;
		}

		// 로그인 성공 시 기존 세션을 폐기하여 세션 고정 공격을 방지한다.
		LoginMemberDto loginMember = loginResult.loginMember();
		try {
			return "redirect:" + loginSessionManager.completeLogin(request, response, loginMember);
		} catch (IllegalArgumentException | IllegalStateException e) {
			// 인증 상태 저장에 실패해도 회원 정보만 남은 세션을 유지하지 않는다.
			log.warn("로그인 세션 생성에 실패했습니다.", e);
			return "redirect:/auth/login?error";
		}
	}

	// 회원가입 화면
	@GetMapping("/signup")
	public String signupPage(HttpServletRequest request) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		return "auth/signup";
	}

	@GetMapping("/signup/user")
	public String userSignupPage(HttpServletRequest request, Model model) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		model.addAttribute("memberType", 1);
		model.addAttribute("businessMember", false);
		return "auth/signup-form";
	}

	@GetMapping("/signup/business")
	public String businessSignupPage(HttpServletRequest request, Model model) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		model.addAttribute("memberType", 2);
		model.addAttribute("businessMember", true);
		return "auth/signup-form";
	}

	/**
	 * 로그인·회원가입 화면에 공통으로 적용할 역할별 이동 경로를 결정한다.
	 * 세션 속성의 타입과 역할을 모두 확인해 잘못된 세션 값은 로그인 상태로 인정하지 않는다.
	 */
	static String resolveAuthenticatedRedirect(HttpSession session) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof LoginMemberDto loginMember)) {
			removeStaleLoginMember(session);
			return null;
		}

		return switch (loginMember.getMemberType() + ":" + loginMember.getMemberRole()) {
			case "0:ADMIN" -> "redirect:/admin";
			case "2:BUSINESS" -> "redirect:/business/dashboard";
			case "1:USER", "2:USER" -> "redirect:/";
			default -> {
				removeStaleLoginMember(session);
				yield null;
			}
		};
	}

	private static void removeStaleLoginMember(HttpSession session) {
		if (session != null) {
			session.removeAttribute("loginMember");
		}
	}

	/**
	 * 화면 입력과 세션의 이메일 인증 증표를 서비스에 함께 전달한다.
	 * 세션 증표만으로 가입을 허용하지 않으며 서비스가 잠긴 DB 인증 행을 다시 확인하고 소비한다.
	 */
	@PostMapping("/membersignup")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> memberSignup(@Valid @ModelAttribute SignUpRequest signUpRequest,
			BindingResult bindingResult, HttpServletRequest request) {

		// 검증 오류가 여러 개면 첫 번째 메시지를 사용자에게 안내한다.
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getAllErrors().get(0).getDefaultMessage();
			if (message == null || message.isBlank()) {
				message = "입력값을 확인해주세요.";
			}
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
		}

		// 새 세션을 만들지 않고 이메일 인증 완료 정보를 조회한다.
		HttpSession session = request.getSession(false);
		VerifiedSignupEmail sessionVerification = getVerifiedSignupEmail(session);

		try {
			service.memberSignUp(signUpRequest, sessionVerification);
			// 회원가입이 완료되면 세션에 남은 이메일 인증 정보를 제거한다.
			if (session != null) {
				session.removeAttribute("verifiedSignupEmail");
			}
			// 성공 응답을 받은 화면에서 회원가입 완료 페이지로 이동한다.
			return ResponseEntity.ok(Map.of("success", true));
		} catch (EmailVerificationException e) {
			// 재인증이 필요한 경우에는 더 이상 사용할 수 없는 인증 정보를 제거한다.
			if (EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED.equals(e.getErrorCode()) && session != null) {
				session.removeAttribute("verifiedSignupEmail");
			}
			// 프론트엔드가 재인증 흐름을 판단할 수 있도록 오류 코드를 함께 반환한다.
			if (e.getErrorCode() != null) {
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", e.getMessage(), "code", e.getErrorCode()));
			}
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		} catch (SignupException e) {
			// 예상 가능한 회원가입 오류만 사용자 메시지로 반환한다.
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		} catch (Exception e) {
			// 예상하지 못한 내부 예외의 상세 내용은 클라이언트에 노출하지 않는다.
			log.error("회원가입 처리 중 예기치 않은 오류가 발생했습니다.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "message", "회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
		}

	}

	// 회원가입에 사용할 세션 인증 정보 조회
	private VerifiedSignupEmail getVerifiedSignupEmail(HttpSession session) {
		if (session == null) {
			return null;
		}

		Object sessionValue = session.getAttribute("verifiedSignupEmail");
		if (sessionValue instanceof VerifiedSignupEmail verifiedSignupEmail) {
			return verifiedSignupEmail;
		}
		return null;
	}

	// 회원가입 완료
	@GetMapping("/signupresult")
	public String signUpResult(HttpServletRequest request) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		return "auth/signupresult";
	}

	// 아이디 찾기
	@GetMapping("/find-id")
	public String findId() {

		return "auth/find-id";
	}

	// 아이디 찾기 결과
	@PostMapping("/find-id")
	public String getFindId(@RequestParam("name") String name, @RequestParam("email") String email, Model model) {

		if (name == null || name.chars().anyMatch(ch -> Character.isWhitespace(ch)) || name.isBlank()
				|| name.length() > 20 || name.length() < 2) {
			model.addAttribute("nameError", "이름을 확인하세요.");
			return "auth/find-id";
		}
		// 회원가입·이메일 인증과 동일하게 이메일 최대 길이를 100자로 검사한다.
		if (email == null || email.chars().anyMatch(ch -> Character.isWhitespace(ch)) || email.isBlank()
				|| !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 100) {
			model.addAttribute("emailError", "이메일을 확인하세요.");
			return "auth/find-id";
		}

		String loginId = service.findId(name, email);
		model.addAttribute("loginId", loginId);

		return "auth/find-id-result";
	}

	// 비밀번호 찾기: 아이디·이메일 입력과 이메일 인증번호 확인 화면
	@GetMapping("/find-password")
	public String findPassword() {
		return "auth/find-password";
	}

	// 아이디·비밀번호 찾기 화면의 정적 링크에서만 진입하는 탈퇴 로컬 회원 재활성화 시작 화면이다.
	@GetMapping("/reactivation")
	public String memberReactivationPage(HttpServletRequest request) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		return "auth/reactivation";
	}

	// 인증 성공 세션 증표가 없는 직접 접근은 아이디 입력 단계로 되돌린다.
	@GetMapping("/reactivation/complete")
	public String memberReactivationCompletePage(HttpServletRequest request) {
		String authenticatedRedirect = resolveAuthenticatedRedirect(request.getSession(false));
		if (authenticatedRedirect != null) {
			return authenticatedRedirect;
		}
		if (getVerifiedMemberReactivation(request.getSession(false)) == null) {
			return "redirect:/auth/reactivation";
		}
		return "auth/reactivation-complete";
	}

	/**
	 * 완료 요청은 로그인 세션을 만들지 않고 상태 전이만 수행한다.
	 * 성공·실패 모두 증표를 제거해 같은 인증 결과의 재시도를 막는다.
	 */
	@PostMapping("/reactivation/complete")
	public String completeMemberReactivation(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		VerifiedMemberReactivation sessionVerification = getVerifiedMemberReactivation(session);
		if (sessionVerification == null) {
			return "redirect:/auth/reactivation";
		}

		try {
			service.reactivateMember(sessionVerification);
			return "redirect:/auth/login?reactivated";
		} catch (EmailVerificationException | IllegalStateException e) {
			return "redirect:/auth/reactivation?error=verification";
		} finally {
			if (session != null) {
				session.removeAttribute(VERIFIED_MEMBER_REACTIVATION);
			}
		}
	}

	// 인증 성공 세션이 없는 직접 접근은 비밀번호 찾기 첫 화면으로 돌려보낸다.
	@GetMapping("/reset-password")
	public String resetPasswordPage(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (getVerifiedPasswordReset(session) == null) {
			return "redirect:/auth/find-password";
		}
		return "auth/reset-password";
	}

	/**
	 * 새 비밀번호 형식을 다시 검사한 뒤 세션 증표와 DB 인증 행을 함께 사용해 비밀번호를 변경한다.
	 * 서비스 트랜잭션에서 인증 결과 소비와 비밀번호 변경 중 하나라도 실패하면 둘 다 롤백된다.
	 */
	@PostMapping("/reset-password")
	public String resetPassword(
			@RequestParam(value = "newPassword", required = false) String newPassword,
			@RequestParam(value = "newPasswordConfirm", required = false) String newPasswordConfirm,
			HttpServletRequest request,
			Model model) {
		HttpSession session = request.getSession(false);
		VerifiedPasswordReset sessionVerification = getVerifiedPasswordReset(session);
		if (sessionVerification == null) {
			return "redirect:/auth/find-password";
		}

		if (newPassword == null
				|| !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$")) {
			model.addAttribute("resetPasswordError", "비밀번호는 공백 없이 영문과 숫자를 포함한 8~20자로 입력해주세요.");
			return "auth/reset-password";
		}
		if (newPasswordConfirm == null || !newPassword.equals(newPasswordConfirm)) {
			model.addAttribute("resetPasswordError", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
			return "auth/reset-password";
		}

		try {
			service.resetPassword(newPassword, sessionVerification);
			// 변경이 끝난 인증 결과는 세션에서도 제거해 뒤로 가기 재시도를 막는다.
			session.removeAttribute("verifiedPasswordReset");
			return "redirect:/auth/login?passwordReset";
		} catch (EmailVerificationException e) {
			if (EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED.equals(e.getErrorCode())) {
				session.removeAttribute("verifiedPasswordReset");
				return "redirect:/auth/find-password?error=verification";
			}
			model.addAttribute("resetPasswordError", e.getMessage());
			return "auth/reset-password";
		}
	}

	// 비밀번호 재설정에만 사용할 수 있는 서버 세션 증표를 타입까지 확인해 꺼낸다.
	private VerifiedPasswordReset getVerifiedPasswordReset(HttpSession session) {
		if (session == null) {
			return null;
		}

		Object sessionValue = session.getAttribute("verifiedPasswordReset");
		if (sessionValue instanceof VerifiedPasswordReset verifiedPasswordReset) {
			return verifiedPasswordReset;
		}
		return null;
	}

	private VerifiedMemberReactivation getVerifiedMemberReactivation(HttpSession session) {
		if (session == null) {
			return null;
		}

		Object sessionValue = session.getAttribute(VERIFIED_MEMBER_REACTIVATION);
		if (sessionValue instanceof VerifiedMemberReactivation verifiedMemberReactivation) {
			return verifiedMemberReactivation;
		}
		return null;
	}
}
