package com.gnagnoohc.travel.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.dto.VerifiedPasswordReset;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.service.AuthService;
import com.gnagnoohc.travel.auth.service.EmailVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/api")
public class AuthApiController {

	private final AuthService service;
	private final EmailVerificationService emailVerificationService;

	// 회원가입 입력값 중복 확인
	@GetMapping("/check-login-id")
	public ResponseEntity<Map<String, Object>> checkLoginId(
			@RequestParam("loginId") String loginId) {

		boolean available = service.checkLoginId(loginId) == 0;

		return ResponseEntity.ok(Map.of(
				"success", true,
				"available", available,
				"message", available ? "사용 가능한 아이디입니다." : "중복된 아이디입니다."
		));
	}

	@GetMapping("/check-nickname")
	public ResponseEntity<Map<String, Object>> checkNickname(
			@RequestParam("nickname") String nickname) {

		boolean available = service.checkNickname(nickname) == 0;

		return ResponseEntity.ok(Map.of(
				"success", true,
				"available", available,
				"message", available ? "사용 가능한 닉네임 입니다." : "중복된 닉네임 입니다."
		));
	}

	// 이메일 인증번호 발송
	// 이메일 형식과 발송 제한은 서비스에서 다시 검사한 뒤 인증번호를 발송한다.
	@PostMapping("/email-verification/send")
	public ResponseEntity<Map<String, Object>> sendEmailVerificationCode(
			@RequestParam("email") String email,
			HttpServletRequest request) {
		try {
			// 전달 헤더가 아닌 서버가 확인한 원격 주소만 발송 제한에 사용한다.
			emailVerificationService.sendSignupVerificationCode(email, request.getRemoteAddr());
			// 새 인증번호 발송이 성공하면 이전 인증 결과는 더 이상 사용하지 않는다.
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.removeAttribute("verifiedSignupEmail");
			}
			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "인증번호를 발송했습니다."
			));
		} catch (EmailVerificationException e) {
			return ResponseEntity.badRequest()
					.body(Map.of(
							"success", false,
							"message", e.getMessage()
					));
		}
	}

	// 이메일 인증번호 검증
	// 클라이언트가 보낸 값이 아니라 DB에 기록된 인증 완료 상태를 기준으로 판단한다.
	@PostMapping("/email-verification/verify")
	public ResponseEntity<Map<String, Object>> verifyEmailCode(
			@RequestParam("email") String email,
			@RequestParam("code") String code,
			HttpServletRequest request) {
		try {
			VerifiedSignupEmail verifiedEmail = emailVerificationService
					.verifySignupCode(email, code);
			if (verifiedEmail == null) {
				return ResponseEntity.badRequest()
						.body(Map.of(
								"success", false,
								"message", "인증번호가 일치하지 않습니다."
						));
			}

			// 확인된 인증 정보만 세션에 저장하고, 세션 고정 공격을 막기 위해 세션 ID를 변경한다.
			HttpSession session = request.getSession(true);
			if (!session.isNew()) {
				request.changeSessionId();
			}
			session.setAttribute("verifiedSignupEmail", verifiedEmail);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "이메일 인증이 완료되었습니다."
			));
		} catch (EmailVerificationException e) {
			return ResponseEntity.badRequest()
					.body(Map.of(
							"success", false,
							"message", e.getMessage()
					));
		}
	}

	// 비밀번호 찾기 인증번호 발송: 활성 로컬 회원의 아이디와 이메일이 일치할 때만 발송한다.
	@PostMapping("/password-reset/send")
	public ResponseEntity<Map<String, Object>> sendPasswordResetCode(
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "email", required = false) String email,
			HttpServletRequest request) {
		try {
			// X-Forwarded-For는 위조될 수 있으므로 현재 단계에서는 직접 원격 주소만 사용한다.
			emailVerificationService.sendPasswordResetVerificationCode(
					username, email, request.getRemoteAddr());
			// 새 인증번호를 발송하면 이전 비밀번호 재설정 증표는 즉시 무효화한다.
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.removeAttribute("verifiedPasswordReset");
			}
			return ResponseEntity.ok(Map.of("success", true, "message", "인증번호를 발송했습니다."));
		} catch (EmailVerificationException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// 비밀번호 찾기 인증번호 검증: 성공한 DB 인증 결과만 새 비밀번호 화면 접근 세션에 저장한다.
	@PostMapping("/password-reset/verify")
	public ResponseEntity<Map<String, Object>> verifyPasswordResetCode(
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "code", required = false) String code,
			HttpServletRequest request) {
		try {
			VerifiedPasswordReset verifiedPasswordReset = emailVerificationService
					.verifyPasswordResetCode(username, email, code);
			if (verifiedPasswordReset == null) {
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "인증번호가 일치하지 않습니다."));
			}

			// 인증 성공 직전에 세션 ID를 교체해 세션 고정 공격을 방지한다.
			HttpSession session = request.getSession(true);
			if (!session.isNew()) {
				request.changeSessionId();
			}
			session.setAttribute("verifiedPasswordReset", verifiedPasswordReset);
			return ResponseEntity.ok(Map.of("success", true, "message", "이메일 인증이 완료되었습니다."));
		} catch (EmailVerificationException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}
}
