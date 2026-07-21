package com.gnagnoohc.travel.auth.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gnagnoohc.travel.auth.dto.LocalLoginResult;
import com.gnagnoohc.travel.auth.dto.SignUpRequest;
import com.gnagnoohc.travel.auth.dto.VerifiedPasswordReset;
import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.exception.SignupException;
import com.gnagnoohc.travel.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService service;

	// 로그인
	@GetMapping("/login")
	public String loginPage() {
		return "auth/login";
	}

	@PostMapping("/login")
	public String login(@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "password", required = false) String password, HttpServletRequest request,
			Model model) {
		// 입력하지 않은 항목은 로그인 처리 전에 확인해 해당 입력칸에 안내한다.
		boolean usernameBlank = username == null || username.isBlank();
		boolean passwordBlank = password == null || password.isBlank();
		if (usernameBlank || passwordBlank) {
			if (usernameBlank) {
				model.addAttribute("usernameError", "아이디를 입력해주세요.");
			}
			if (passwordBlank) {
				model.addAttribute("passwordError", "비밀번호를 입력해주세요.");
			}
			return "auth/login";
		}

		LocalLoginResult loginResult = service.authenticateLocal(username, password);
		switch (loginResult.status()) {
		case LOCKED:
			return "redirect:/auth/login?locked";
		case INVALID_CREDENTIALS:
			return "redirect:/auth/login?error";
		case SUCCESS:
			break;
		}

		// 로그인 성공 시 기존 세션을 폐기하여 세션 고정 공격을 방지한다.
		HttpSession previousSession = request.getSession(false);
		if (previousSession != null) {
			previousSession.invalidate();
		}

		HttpSession loginSession = request.getSession(true);
		loginSession.setAttribute("id", loginResult.memberId());
		return "redirect:/";
	}

	// 로그아웃
	@GetMapping("/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/";
	}

	// 회원가입 화면
	@GetMapping("/signup")
	public String signupPage() {
		return "auth/signup";
	}

	@GetMapping("/signup/user")
	public String userSignupPage(Model model) {
		model.addAttribute("memberType", 1);
		model.addAttribute("businessMember", false);
		return "auth/signup-form";
	}

	@GetMapping("/signup/business")
	public String businessSignupPage(Model model) {
		model.addAttribute("memberType", 2);
		model.addAttribute("businessMember", true);
		return "auth/signup-form";
	}

	// 회원가입 처리
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
	public String signUpResult() {
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
		if (email == null || email.chars().anyMatch(ch -> Character.isWhitespace(ch)) || email.isBlank()
				|| !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 50) {
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

	// 인증 성공 세션이 없는 직접 접근은 비밀번호 찾기 첫 화면으로 돌려보낸다.
	@GetMapping("/reset-password")
	public String resetPasswordPage(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (getVerifiedPasswordReset(session) == null) {
			return "redirect:/auth/find-password";
		}
		return "auth/reset-password";
	}

	// 새 비밀번호는 서버에서 다시 검증하고 세션·DB 인증 결과를 함께 확인해 변경한다.
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
}
