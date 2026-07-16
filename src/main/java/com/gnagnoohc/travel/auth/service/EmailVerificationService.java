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
	// 인증번호 수명, 재발송 간격, 시도/발송 제한은 정책값으로 한 곳에서 관리한다.
	private static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
	private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
	private static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(30);
	private static final int MAX_SENDS_PER_EMAIL_PER_DAY = 5;
	private static final int MAX_SENDS_PER_DAY = 300;
	private static final int MAX_VERIFICATION_ATTEMPTS = 5;

	// 의존성
	// 매퍼는 인증 상태를 DB에 저장하고, 메일 서비스는 SMTP 발송만 담당한다.
	private final AuthMapper mapper;
	private final GoogleMailService googleMailService;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	// 인증번호 발송
	@Transactional
	public void sendSignupVerificationCode(String rawEmail) {
		// 이메일 형식을 확인하고 도메인 부분을 소문자로 통일한다.
		String email = normalizeAndValidateEmail(rawEmail);
		Instant now = Instant.now();

		// 최신 발송 기록을 잠근 뒤 재발송 대기시간과 일일 발송 한도를 검사한다.
		EmailVerification latest = mapper.findLatestEmailVerificationForUpdate(email, SIGNUP_PURPOSE);
		validateSendLimits(email, latest, now);

		// 인증번호 원문은 메일로만 전달하고 DB에는 BCrypt 해시만 저장한다.
		String verificationCode = createVerificationCode();
		EmailVerification verification = new EmailVerification();
		verification.setEmail(email);
		verification.setPurpose(SIGNUP_PURPOSE);
		verification.setCodeHash(passwordEncoder.encode(verificationCode));
		verification.setExpiresAt(Timestamp.from(now.plus(CODE_VALIDITY)));

		// 발송 실패 시 DB INSERT도 롤백되도록 같은 트랜잭션에서 처리한다.
		if (mapper.insertEmailVerification(verification) != 1) {
			throw new EmailVerificationException("이메일 인증 정보를 저장하지 못했습니다.");
		}

		googleMailService.sendVerificationCode(email, verificationCode);
	}

	// 인증번호 검증
	@Transactional
	public VerifiedSignupEmail verifySignupCode(String rawEmail, String rawCode) {
		// 검증 요청도 서버에서 이메일과 6자리 코드 형식을 다시 검사한다.
		String email = normalizeAndValidateEmail(rawEmail);
		String code = validateCodeFormat(rawCode);
		// 동시에 여러 번 검증하는 경우를 막기 위해 최신 인증 행을 잠근다.
		EmailVerification verification = mapper.findLatestEmailVerificationForUpdate(email, SIGNUP_PURPOSE);
		validateVerificationState(verification, Instant.now());

		// 인증번호 일치 여부와 관계없이 먼저 시도 횟수를 증가시킨다.
		// 오답에서는 예외를 던지지 않고 false를 반환해야 증가 UPDATE가 커밋된다.
		increaseVerificationAttempt(verification);

		if (!passwordEncoder.matches(code, verification.getCodeHash())) {
			return null;
		}
		// 최초 성공일 때만 verified_at을 기록하고 재요청에서는 기존 인증 시각을 유지한다.
		if (verification.getVerifiedAt() == null
				&& mapper.markEmailVerificationVerified(verification.getEmailVerificationId()) != 1) {
			throw new EmailVerificationException("이메일 인증 상태를 변경하지 못했습니다.");
		}

		return findVerifiedSignupEmail(verification.getEmailVerificationId(), email);
	}

	// 회원가입 직전 이메일 인증 상태 확인
	@Transactional
	public VerifiedSignupEmail requireVerifiedSignupEmail(
			String rawEmail,
			VerifiedSignupEmail sessionVerification) {
		// 회원가입 직전에 세션 증표와 정확한 DB 인증 행을 함께 검증한다.
		String email = normalizeAndValidateEmail(rawEmail);

		validateSessionVerification(email, sessionVerification);

		// 발송과 같은 최신 행 잠금을 먼저 잡아 재발송과 가입의 실행 순서를 고정한다.
		EmailVerification latestVerification = mapper
				.findLatestEmailVerificationForUpdate(email, SIGNUP_PURPOSE);
		validateSignupVerificationAvailability(sessionVerification, latestVerification);

		VerifiedSignupEmail verifiedEmail = createVerifiedSignupEmail(latestVerification);
		validateVerifiedEmailValidity(verifiedEmail, Instant.now());

		return verifiedEmail;
	}

	// 인증번호 검증 상태 확인
	private void validateVerificationState(EmailVerification verification, Instant now) {
		if (verification == null) {
			throw new EmailVerificationException("먼저 인증번호를 발송해주세요.");
		}
		// 이미 인증된 행도 새 세션에 저장할 때는 실제 인증번호를 다시 확인한다.
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
		// DB의 인증 시각과 행 ID를 세션에 저장할 안전한 결과로 반환한다.
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

	// 회원가입 직전 세션 및 DB 인증 상태 확인
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
	private void validateSendLimits(String email, EmailVerification latest, Instant now) {
		// 화면의 버튼 비활성화는 우회할 수 있으므로 서버에서 같은 제한을 다시 적용한다.
		if (latest != null
				&& latest.getCreatedAt().toInstant().plus(RESEND_COOLDOWN).isAfter(now)) {
			throw new EmailVerificationException("인증번호는 60초 후 다시 발송할 수 있습니다.");
		}
		if (mapper.countEmailVerificationRequestsInLastDay(email, SIGNUP_PURPOSE)
				>= MAX_SENDS_PER_EMAIL_PER_DAY) {
			throw new EmailVerificationException("해당 이메일의 일일 인증번호 발송 횟수를 초과했습니다.");
		}
		if (mapper.countAllEmailVerificationRequestsInLastDay() >= MAX_SENDS_PER_DAY) {
			throw new EmailVerificationException("오늘의 인증번호 발송 한도를 초과했습니다.");
		}
	}

	// 인증번호 생성 및 입력값 검증
	private String createVerificationCode() {
		// 예측 가능한 Random이 아닌 SecureRandom으로 000000~999999 범위의 코드를 만든다.
		return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1000000));
	}

	private String validateCodeFormat(String rawCode) {
		// DB 조회 전에 형식을 검사해 불필요한 BCrypt 비교를 줄인다.
		if (rawCode == null || !rawCode.matches("\\d{6}")) {
			throw new EmailVerificationException("인증번호 6자리를 입력해주세요.");
		}
		return rawCode;
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
