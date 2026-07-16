package com.gnagnoohc.travel.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
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
			emailVerificationService.sendSignupVerificationCode(email);
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
	// 인증 결과는 서버 DB의 verified_at을 기준으로 판단한다.
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

			// DB에서 검증한 인증 결과만 세션에 저장하고 세션 ID를 교체한다.
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
}
