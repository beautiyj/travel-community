package com.gnagnoohc.travel.auth.controller;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.dto.VerifiedPasswordReset;
import com.gnagnoohc.travel.auth.dto.PendingMemberReactivation;
import com.gnagnoohc.travel.auth.dto.VerifiedMemberReactivation;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.service.AuthService;
import com.gnagnoohc.travel.auth.service.EmailVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 가입·비밀번호 재설정 화면이 호출하는 인증 보조 API다.
 * <p>
 * JSON 응답 조립과 세션 증표 저장만 담당한다. 발송 제한, 인증번호 해시 비교,
 * 만료·재사용 검사는 {@link EmailVerificationService}가 DB 상태를 기준으로 처리한다.
 * 따라서 브라우저가 보내는 "인증 완료" 값은 신뢰하지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/api")
public class AuthApiController {

	private static final String PENDING_MEMBER_REACTIVATION = "pendingMemberReactivation";
	private static final String VERIFIED_MEMBER_REACTIVATION = "verifiedMemberReactivation";
	private static final String REACTIVATION_SEND_MESSAGE =
			"입력한 정보로 재활성화 가능한 계정이 있으면 인증번호를 발송했습니다.";
	private static final Duration REACTIVATION_IP_BURST_WINDOW = Duration.ofSeconds(60);
	private static final Duration REACTIVATION_IP_DAILY_WINDOW = Duration.ofDays(1);
	private static final int MAX_REACTIVATION_SENDS_PER_IP_PER_BURST = 5;
	private static final int MAX_REACTIVATION_SENDS_PER_IP_PER_DAY = 20;
	private static final int MAX_REACTIVATION_IP_THROTTLE_ENTRIES = 1_000;

	// 단일 서버 프로젝트에서 세션 flood와 비대상 아이디 반복 요청을 줄이는 최소 보호 장치다.
	private final Object reactivationSendThrottleLock = new Object();
	private final Map<String, Deque<Long>> reactivationIpSendThrottle =
			new LinkedHashMap<>(16, 0.75f, true);

	private final AuthService service;
	private final EmailVerificationService emailVerificationService;

	/**
	 * 브라우저 뒤로 가기로 복원된 인증 화면이 현재 로그인 상태를 다시 확인할 때 사용한다.
	 * 조회만으로 새 세션을 만들지 않으며 회원 역할이나 개인정보는 응답에 노출하지 않는다.
	 */
	@GetMapping("/session-status")
	public ResponseEntity<Map<String, Object>> sessionStatus(HttpServletRequest request) {
		boolean authenticated = AuthController.resolveAuthenticatedRedirect(
				request.getSession(false)) != null;
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(Map.of("authenticated", authenticated));
	}

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
			if (EmailVerificationException.DUPLICATE_EMAIL.equals(e.getErrorCode())) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(Map.of(
								"success", false,
								"code", EmailVerificationException.DUPLICATE_EMAIL,
								"message", e.getMessage()
						));
			}
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

	/**
	 * 계정 존재·탈퇴 여부를 응답 차이로 노출하지 않는 재활성화 인증번호 발송 API다.
	 * 이전 단계 증표를 먼저 지워 무효 아이디 요청 뒤에 과거 유효 증표를 쓰지 못하게 한다.
	 */
	@PostMapping("/reactivation/send")
	public ResponseEntity<Map<String, Object>> sendMemberReactivationCode(
			@RequestParam(value = "username", required = false) String username,
			HttpServletRequest request) {
		// IP 제한은 세션 생성보다 먼저 적용한다. 제한된 요청은 대상 상태와 무관하므로 쿠키를 만들지 않아도 된다.
		if (!tryAcquireReactivationIpSendPermit(request.getRemoteAddr())) {
			// 기존 세션이 있을 때만 이전 재활성화 증표를 무효화한다. 제한 응답 때문에 새 세션은 만들지 않는다.
			clearMemberReactivationProofs(request.getSession(false));
			return reactivationSendAccepted();
		}
		// IP 제한을 통과한 요청은 대상 여부와 무관하게 같은 세션 생성·증표 제거 과정을 거친다.
		HttpSession session = request.getSession(true);
		clearMemberReactivationProofs(session);

		try {
			PendingMemberReactivation pendingReactivation = emailVerificationService
					.sendMemberReactivationVerificationCode(username, request.getRemoteAddr());
			if (pendingReactivation != null) {
				request.getSession(true)
						.setAttribute(PENDING_MEMBER_REACTIVATION, pendingReactivation);
			}
			return reactivationSendAccepted();
		} catch (EmailVerificationException e) {
			if (EmailVerificationException.NOT_WITHDRAWN_MEMBER.equals(e.getErrorCode())
					|| EmailVerificationException.NOT_FOUND_MEMBER.equals(e.getErrorCode())) {
				return ResponseEntity.badRequest().body(Map.of(
						"success", false,
						"message", e.getMessage()));
			}
			// 발송 제한·대상 상태를 구분하지 않아 계정 열거와 제한 우회 단서를 남기지 않는다.
			return reactivationSendAccepted();
		}
	}

	/**
	 * 브라우저가 보낸 아이디·이메일은 인증 대상 선택에 사용하지 않는다.
	 * send 단계에서 만든 서버 세션 증표와 인증번호만으로 다음 단계로 이동한다.
	 */
	@PostMapping("/reactivation/verify")
	public ResponseEntity<Map<String, Object>> verifyMemberReactivationCode(
			@RequestParam(value = "code", required = false) String code,
			HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		PendingMemberReactivation pendingReactivation = getPendingMemberReactivation(session);
		if (pendingReactivation == null) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", "처음 단계부터 다시 진행해주세요."));
		}

		try {
			VerifiedMemberReactivation verifiedReactivation = emailVerificationService
					.verifyMemberReactivationCode(pendingReactivation, code);
			if (verifiedReactivation == null) {
				return ResponseEntity.badRequest().body(Map.of(
						"success", false,
						"message", "인증번호가 일치하지 않습니다."));
			}

			// 인증 성공 직전에 세션 ID를 바꿔 기존 세션 ID를 아는 공격자의 고정을 막는다.
			request.changeSessionId();
			session.removeAttribute(PENDING_MEMBER_REACTIVATION);
			session.setAttribute(VERIFIED_MEMBER_REACTIVATION, verifiedReactivation);
			return ResponseEntity.ok(Map.of("success", true, "message", "이메일 인증이 완료되었습니다."));
		} catch (EmailVerificationException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	private ResponseEntity<Map<String, Object>> reactivationSendAccepted() {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
				"success", true,
				"message", REACTIVATION_SEND_MESSAGE));
	}

	private PendingMemberReactivation getPendingMemberReactivation(HttpSession session) {
		if (session == null) {
			return null;
		}
		Object sessionValue = session.getAttribute(PENDING_MEMBER_REACTIVATION);
		return sessionValue instanceof PendingMemberReactivation pendingReactivation
				? pendingReactivation : null;
	}

	private void clearMemberReactivationProofs(HttpSession session) {
		if (session == null) {
			return;
		}
		session.removeAttribute(PENDING_MEMBER_REACTIVATION);
		session.removeAttribute(VERIFIED_MEMBER_REACTIVATION);
	}

	/**
	 * 매칭 전 모든 요청에 서버 관찰 IP 기준 5회/60초와 기존 메일 정책의 20회/일 상한을 적용한다.
	 * LRU map이 포화되면 가장 오래 접근하지 않은 IP를 퇴출해 신규 IP를 전역 fail-closed로 막지 않는다.
	 */
	private boolean tryAcquireReactivationIpSendPermit(String rawRequestIp) {
		long now = System.currentTimeMillis();
		String requestIp = normalizeReactivationRequestIp(rawRequestIp);
		synchronized (reactivationSendThrottleLock) {
			Deque<Long> requestTimes = reactivationIpSendThrottle.get(requestIp);
			if (requestTimes == null) {
				if (reactivationIpSendThrottle.size() >= MAX_REACTIVATION_IP_THROTTLE_ENTRIES) {
					Iterator<String> oldestIps = reactivationIpSendThrottle.keySet().iterator();
					if (oldestIps.hasNext()) {
						oldestIps.next();
						oldestIps.remove();
					}
				}
				requestTimes = new ArrayDeque<>();
				reactivationIpSendThrottle.put(requestIp, requestTimes);
			}

			removeExpiredReactivationIpRequestTimes(requestTimes, now);
			long burstStart = now - REACTIVATION_IP_BURST_WINDOW.toMillis();
			long burstCount = requestTimes.stream().filter(requestTime -> requestTime > burstStart).count();
			if (burstCount >= MAX_REACTIVATION_SENDS_PER_IP_PER_BURST
					|| requestTimes.size() >= MAX_REACTIVATION_SENDS_PER_IP_PER_DAY) {
				return false;
			}
			requestTimes.addLast(now);
			return true;
		}
	}

	private void removeExpiredReactivationIpRequestTimes(Deque<Long> requestTimes, long now) {
		long dailyStart = now - REACTIVATION_IP_DAILY_WINDOW.toMillis();
		while (!requestTimes.isEmpty() && requestTimes.peekFirst() <= dailyStart) {
			requestTimes.removeFirst();
		}
	}

	private String normalizeReactivationRequestIp(String rawRequestIp) {
		return rawRequestIp == null || rawRequestIp.isBlank()
				? "unknown"
				: rawRequestIp.trim();
	}
}
