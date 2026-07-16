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

import com.gnagnoohc.travel.auth.dto.SignUpRequest;
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
	public String login(@RequestParam("username") String username, @RequestParam("password") String password,
			HttpServletRequest request) {
		// 아이디와 비밀번호를 검증한 뒤, 성공한 회원의 ID만 세션에 저장한다.
		Integer memberId = service.authenticateLocal(username, password);
		if (memberId == null) {
			return "redirect:/auth/login?error";
		}

		// 로그인 성공 시 기존 세션을 폐기하여 세션 고정 공격을 방지한다.
		HttpSession previousSession = request.getSession(false);
		if (previousSession != null) {
			previousSession.invalidate();
		}

		HttpSession loginSession = request.getSession(true);
		loginSession.setAttribute("id", memberId);
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

		// Bean Validation 오류는 첫 번째 메시지로 응답한다.
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getAllErrors().get(0).getDefaultMessage();
			if (message == null || message.isBlank()) {
				message = "입력값을 확인해주세요.";
			}
			return ResponseEntity.badRequest()
					.body(Map.of(
							"success", false,
							"message", message
					));
		}

		// 새 세션을 만들지 않고 이메일 인증 완료 정보를 조회한다.
		HttpSession session = request.getSession(false);
		VerifiedSignupEmail sessionVerification = getVerifiedSignupEmail(session);

		try {
			service.memberSignUp(signUpRequest, sessionVerification);
			// 가입 트랜잭션이 성공하면 사용 완료한 인증 정보를 세션에서 제거한다.
			if (session != null) {
				session.removeAttribute("verifiedSignupEmail");
			}
			// 화면 이동은 성공 응답을 받은 프론트엔드에서 처리한다.
			return ResponseEntity.ok(Map.of("success", true));
		} catch (EmailVerificationException e) {
			// 재인증이 필요한 경우에는 더 이상 사용할 수 없는 인증 정보를 제거한다.
			if (EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED.equals(e.getErrorCode()) && session != null) {
				session.removeAttribute("verifiedSignupEmail");
			}
			// 프론트엔드가 재인증 흐름을 판단할 수 있도록 오류 코드를 함께 반환한다.
			if (e.getErrorCode() != null) {
				return ResponseEntity.badRequest()
						.body(Map.of(
								"success", false,
								"message", e.getMessage(),
								"code", e.getErrorCode()
						));
			}
			return ResponseEntity.badRequest()
					.body(Map.of(
							"success", false,
							"message", e.getMessage()
					));
		} catch (SignupException e) {
			// 예상 가능한 회원가입 오류만 사용자 메시지로 반환한다.
			return ResponseEntity.badRequest()
					.body(Map.of(
							"success", false,
							"message", e.getMessage()
					));
		} catch (Exception e) {
			// 예상하지 못한 내부 예외의 상세 내용은 클라이언트에 노출하지 않는다.
			log.error("회원가입 처리 중 예기치 않은 오류가 발생했습니다.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of(
							"success", false,
							"message", "회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
					));
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

	// 개발용 테스트 화면
	@GetMapping("/test")
	public String authTest() {
		return "auth/test";
	}
}
