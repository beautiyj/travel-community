package com.gnagnoohc.travel.auth.service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.dto.VerifiedPasswordReset;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.model.EmailVerification;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	// 이메일 인증 정책
	// 인증 목적을 분리해 추후 비밀번호 찾기 등의 인증 용도와 섞이지 않게 한다.
	private static final String SIGNUP_PURPOSE = "SIGNUP";
	private static final String FIND_PASSWORD_PURPOSE = "FIND_PASSWORD";
	// 인증번호 수명, 재발송 간격, 시도/발송 제한은 정책값으로 한 곳에서 관리한다.
	private static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
	private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
	private static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(30);
	private static final int MAX_SENDS_PER_EMAIL_PER_DAY = 5;
	private static final int MAX_SENDS_PER_DAY = 300;
	private static final int MAX_VERIFICATION_ATTEMPTS = 5;

	// 매퍼는 인증 상태를 DB에 저장하고, 메일 서비스는 SMTP 발송만 담당한다.
	private final AuthMapper mapper;
	private final GoogleMailService googleMailService;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	// 인증번호 발송
	@Transactional
	public void sendSignupVerificationCode(String rawEmail) {
		String email = normalizeAndValidateEmail(rawEmail);
		sendVerificationCode(email, SIGNUP_PURPOSE, null);
	}

	// 인증번호 검증
	@Transactional
	public VerifiedSignupEmail verifySignupCode(String rawEmail, String rawCode) {
		String email = normalizeAndValidateEmail(rawEmail);
		EmailVerification verification = verifyCode(email, rawCode, SIGNUP_PURPOSE, null);
		if (verification == null) {
			return null;
		}
		return findVerifiedSignupEmail(verification.getEmailVerificationId(), email);
	}

	// 비밀번호 찾기 인증번호 발송
	@Transactional
	public void sendPasswordResetVerificationCode(String rawUsername, String rawEmail) {
		String username = validateUsername(rawUsername);
		String email = normalizeAndValidateEmail(rawEmail);
		Integer memberId = mapper.findActiveLocalMemberId(username, email);
		if (memberId == null) {
			throw new EmailVerificationException("입력한 아이디와 이메일을 확인해주세요.");
		}

		sendVerificationCode(email, FIND_PASSWORD_PURPOSE, memberId.longValue());
	}

	// 비밀번호 찾기 인증번호 검증
	@Transactional
	public VerifiedPasswordReset verifyPasswordResetCode(
			String rawUsername, String rawEmail, String rawCode) {
		String username = validateUsername(rawUsername);
		String email = normalizeAndValidateEmail(rawEmail);
		Integer memberId = mapper.findActiveLocalMemberId(username, email);
		if (memberId == null) {
			throw new EmailVerificationException("입력한 아이디와 이메일을 확인해주세요.");
		}

		EmailVerification verification = verifyCode(
				email, rawCode, FIND_PASSWORD_PURPOSE, memberId.longValue());
		if (verification == null) {
			return null;
		}

		VerifiedPasswordReset passwordReset = new VerifiedPasswordReset();
		passwordReset.setEmailVerificationId(verification.getEmailVerificationId());
		passwordReset.setMemberId(memberId);
		passwordReset.setEmail(email);
		return passwordReset;
	}

	// 회원가입 직전 이메일 인증 상태 확인
	@Transactional
	public VerifiedSignupEmail requireVerifiedSignupEmail(
			String rawEmail,
			VerifiedSignupEmail sessionVerification) {
		// 회원가입 직전에 세션에 저장된 인증 정보와 DB 인증 행을 함께 확인한다.
		String email = normalizeAndValidateEmail(rawEmail);

		validateSessionVerification(email, sessionVerification);

		// 가입 확인 중에 새 인증번호가 발송되지 않도록 최신 인증 행을 먼저 잠근다.
		EmailVerification latestVerification = mapper
				.findLatestEmailVerificationForUpdate(email, SIGNUP_PURPOSE);
		validateSignupVerificationAvailability(sessionVerification, latestVerification);

		VerifiedSignupEmail verifiedEmail = createVerifiedSignupEmail(latestVerification);
		validateVerifiedEmailValidity(verifiedEmail, Instant.now());

		return verifiedEmail;
	}

	// 비밀번호 변경 직전에 세션 증표와 최신 DB 인증 결과를 함께 확인한다.
	@Transactional
	public void requireVerifiedPasswordReset(VerifiedPasswordReset sessionVerification) {
		if (sessionVerification == null
				|| sessionVerification.getEmailVerificationId() == null
				|| sessionVerification.getMemberId() == null
				|| sessionVerification.getEmail() == null) {
			throw passwordResetReverificationRequired();
		}

		String email = normalizeAndValidateEmail(sessionVerification.getEmail());
		EmailVerification latestVerification = mapper.findLatestPasswordResetVerificationForUpdate(
				email, sessionVerification.getMemberId());
		if (latestVerification == null
				|| !sessionVerification.getEmailVerificationId()
						.equals(latestVerification.getEmailVerificationId())
				|| latestVerification.getVerifiedAt() == null
				|| latestVerification.getConsumedAt() != null) {
			throw passwordResetReverificationRequired();
		}

		if (latestVerification.getVerifiedAt().toInstant()
				.plus(VERIFIED_VALIDITY).isBefore(Instant.now())) {
			throw passwordResetReverificationRequired();
		}
	}

	// 회원가입과 비밀번호 찾기가 같은 발송 제한과 해시 저장 정책을 사용한다.
	private void sendVerificationCode(String email, String purpose, Long memberId) {
		Instant now = Instant.now();
		EmailVerification latest = findLatestVerificationForUpdate(email, purpose, memberId);
		validateSendLimits(email, purpose, latest, now);

		String verificationCode = createVerificationCode();
		EmailVerification verification = new EmailVerification();
		verification.setEmail(email);
		verification.setPurpose(purpose);
		verification.setCodeHash(passwordEncoder.encode(verificationCode));
		verification.setExpiresAt(Timestamp.from(now.plus(CODE_VALIDITY)));
		verification.setMemberId(memberId);

		if (mapper.insertEmailVerification(verification) != 1) {
			throw new EmailVerificationException("이메일 인증 정보를 저장하지 못했습니다.");
		}

		googleMailService.sendVerificationCode(email, verificationCode);
	}

	// 인증 목적과 회원 귀속 여부에 맞는 최신 행을 잠가 동시 발송·검증을 막는다.
	private EmailVerification findLatestVerificationForUpdate(
			String email, String purpose, Long memberId) {
		if (FIND_PASSWORD_PURPOSE.equals(purpose) && memberId != null) {
			return mapper.findLatestPasswordResetVerificationForUpdate(email, memberId.intValue());
		}
		return mapper.findLatestEmailVerificationForUpdate(email, purpose);
	}

	// 인증번호 확인의 공통 처리다. 오답은 null로 반환해 시도 횟수 증가를 커밋한다.
	private EmailVerification verifyCode(String email, String rawCode, String purpose, Long memberId) {
		String code = validateCodeFormat(rawCode);
		EmailVerification verification = findLatestVerificationForUpdate(email, purpose, memberId);
		validateVerificationState(verification, Instant.now());
		increaseVerificationAttempt(verification);

		if (!passwordEncoder.matches(code, verification.getCodeHash())) {
			return null;
		}
		if (verification.getVerifiedAt() == null
				&& mapper.markEmailVerificationVerified(verification.getEmailVerificationId()) != 1) {
			throw new EmailVerificationException("이메일 인증 상태를 변경하지 못했습니다.");
		}
		return verification;
	}

	// 인증번호 검증 상태 확인
	private void validateVerificationState(EmailVerification verification, Instant now) {
		if (verification == null) {
			throw new EmailVerificationException("먼저 인증번호를 발송해주세요.");
		}
		if (!verification.getExpiresAt().toInstant().isAfter(now)) {
			throw new EmailVerificationException("인증번호가 만료되었습니다. 다시 발송해주세요.");
		}
		if (verification.getAttemptCount() >= MAX_VERIFICATION_ATTEMPTS) {
			throw new EmailVerificationException("인증 시도 횟수를 초과했습니다. 다시 발송해주세요.");
		}
	}

	// 인증 시도 횟수 증가
	private void increaseVerificationAttempt(EmailVerification verification) {
		if (mapper.incrementEmailVerificationAttempt(verification.getEmailVerificationId()) != 1) {
			throw new EmailVerificationException("이메일 인증 시도 횟수를 변경하지 못했습니다.");
		}
	}

	// DB 인증 완료 이메일 조회
	private VerifiedSignupEmail findVerifiedSignupEmail(long emailVerificationId, String email) {
		// 인증 행에서 세션에 필요한 ID, 이메일, 인증 시각만 반환한다.
		VerifiedSignupEmail verifiedEmail = mapper.findVerifiedSignupEmailForUpdate(
				emailVerificationId, email, SIGNUP_PURPOSE);
		if (verifiedEmail == null) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"사용할 수 없는 이메일 인증입니다. 다시 인증해주세요."
			);
		}
		return verifiedEmail;
	}

	// 세션에 저장된 이메일 인증 정보 확인
	private void validateSessionVerification(String email, VerifiedSignupEmail sessionVerification) {
		if (sessionVerification == null
				|| sessionVerification.getEmailVerificationId() == null
				|| !email.equals(sessionVerification.getEmail())) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이메일 인증이 필요합니다."
			);
		}
	}

	// 최신 인증 행의 가입 사용 가능 여부 확인
	private void validateSignupVerificationAvailability(
			VerifiedSignupEmail sessionVerification,
			EmailVerification latestVerification) {
		if (latestVerification == null
				|| !sessionVerification.getEmailVerificationId()
						.equals(latestVerification.getEmailVerificationId())
				|| latestVerification.getVerifiedAt() == null
				|| latestVerification.getMemberId() != null) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"사용할 수 없는 이메일 인증입니다. 다시 인증해주세요."
			);
		}
	}

	// DB 인증 행을 세션 보관용 DTO로 변환
	private VerifiedSignupEmail createVerifiedSignupEmail(EmailVerification verification) {
		VerifiedSignupEmail verifiedEmail = new VerifiedSignupEmail();
		verifiedEmail.setEmailVerificationId(verification.getEmailVerificationId());
		verifiedEmail.setEmail(verification.getEmail());
		verifiedEmail.setVerifiedAt(verification.getVerifiedAt());
		return verifiedEmail;
	}

	// 가입에 사용할 이메일 인증의 유효 시간 확인
	private void validateVerifiedEmailValidity(VerifiedSignupEmail verifiedEmail, Instant now) {
		if (verifiedEmail.getVerifiedAt().toInstant()
				.plus(VERIFIED_VALIDITY).isBefore(now)) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이메일 인증이 만료되었습니다. 다시 인증해주세요."
			);
		}
	}

	// 발송 제한 검증
	private void validateSendLimits(String email, String purpose, EmailVerification latest, Instant now) {
		// 화면의 버튼 비활성화는 우회할 수 있으므로 서버에서 같은 제한을 다시 적용한다.
		if (latest != null
				&& latest.getCreatedAt().toInstant().plus(RESEND_COOLDOWN).isAfter(now)) {
			throw new EmailVerificationException("인증번호는 60초 후 다시 발송할 수 있습니다.");
		}
		if (mapper.countEmailVerificationRequestsInLastDay(email, purpose)
				>= MAX_SENDS_PER_EMAIL_PER_DAY) {
			throw new EmailVerificationException("해당 이메일의 일일 인증번호 발송 횟수를 초과했습니다.");
		}
		if (mapper.countAllEmailVerificationRequestsInLastDay() >= MAX_SENDS_PER_DAY) {
			throw new EmailVerificationException("오늘의 인증번호 발송 한도를 초과했습니다.");
		}
	}

	// 인증번호 생성 및 입력값 검증
	private String createVerificationCode() {
		// SecureRandom을 사용해 예측하기 어려운 6자리 인증번호를 만든다.
		return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1000000));
	}

	private String validateCodeFormat(String rawCode) {
		// DB 조회 전에 형식을 검사해 불필요한 BCrypt 비교를 줄인다.
		if (rawCode == null || !rawCode.matches("\\d{6}")) {
			throw new EmailVerificationException("인증번호 6자리를 입력해주세요.");
		}
		return rawCode;
	}

	private String validateUsername(String rawUsername) {
		if (rawUsername == null || !rawUsername.matches("^[A-Za-z0-9]{5,20}$")) {
			throw new EmailVerificationException("아이디는 영문 또는 숫자 5~20자로 입력해주세요.");
		}
		return rawUsername;
	}

	private EmailVerificationException passwordResetReverificationRequired() {
		return new EmailVerificationException(
				EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
				"이메일 인증이 만료되었거나 이미 사용되었습니다. 다시 인증해주세요.");
	}

	private String normalizeAndValidateEmail(String rawEmail) {
		// 이 검사는 메일 주소 존재 여부가 아니라 입력 형식만 검사한다.
		if (rawEmail == null) {
			throw new EmailVerificationException("이메일을 입력해주세요.");
		}

		String email = rawEmail.trim();
		if (email.length() > 100 || email.indexOf('@') <= 0) {
			throw new EmailVerificationException("올바른 이메일 주소를 입력해주세요.");
		}

		try {
			InternetAddress parsed = new InternetAddress(email, true);
			parsed.validate();
			if (!email.equals(parsed.getAddress())) {
				throw new EmailVerificationException("올바른 이메일 주소를 입력해주세요.");
			}
		} catch (AddressException e) {
			throw new EmailVerificationException("올바른 이메일 주소를 입력해주세요.");
		}

		int atIndex = email.lastIndexOf('@');
		return email.substring(0, atIndex + 1)
				+ email.substring(atIndex + 1).toLowerCase(Locale.ROOT);
	}
}
