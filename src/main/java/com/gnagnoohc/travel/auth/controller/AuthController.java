package com.gnagnoohc.travel.auth.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gnagnoohc.travel.auth.dto.SignUpRequest;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.exception.SignupException;
import com.gnagnoohc.travel.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService service;

	@GetMapping("/login")
	public String loginPage() {
		return "auth/login";
	}

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

	@GetMapping("/test")
	public String authTest() {
		return "auth/test";
	}

	@PostMapping("/membersignup")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> memberSignup(
			@ModelAttribute SignUpRequest signUpRequest) {
		
		
		
		try{
			service.memberSignUp(signUpRequest);
			// [수정] 성공 응답은 처리 결과만 반환하고 화면 이동 경로는 프론트에서 관리한다.
			return ResponseEntity.ok(Map.of(
					"success",true
					));
		// [수정] 예상 가능한 회원가입 오류만 사용자 메시지로 반환한다.
		} catch (EmailVerificationException | SignupException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		// [수정] 예상하지 못한 내부 예외의 상세 내용을 클라이언트에 노출하지 않는다.
		} catch (Exception e) {
			log.error("회원가입 처리 중 예기치 않은 오류가 발생했습니다.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
					"success", false,
					"message", "회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
			));
		}
		
	}
	@GetMapping("/signupresult")
	public String signUpResult() {
		return "auth/signupresult";
	}
}
