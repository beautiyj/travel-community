package com.gnagnoohc.travel.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.LocalLoginResult;
import com.gnagnoohc.travel.auth.dto.SignUpRequest;
import com.gnagnoohc.travel.auth.dto.VerifiedPasswordReset;
import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.exception.SignupException;
import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberLocalAuth;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private static final int MAX_FAILED_LOGIN_COUNT = 5;

	private final AuthMapper mapper;
	private final PasswordEncoder passEncoder;
	private final EmailVerificationService emailVerificationService;

	// 로그인
	@Transactional
	public LocalLoginResult authenticateLocal(String username, String rawPassword) {
		if (username == null || username.isBlank()
				|| rawPassword == null || rawPassword.isBlank()) {
			return LocalLoginResult.invalidCredentials();
		}

		// 같은 아이디의 요청이 실패 횟수를 동시에 바꾸지 않도록 로그인 인증 정보를 잠가 조회한다.
		MemberLocalAuth localAuth = mapper.findLocalLoginAuthForUpdate(username.trim());
		if (localAuth == null) {
			return LocalLoginResult.invalidCredentials();
		}

		// 잠금 중에는 비밀번호를 검사하지 않고 실패 횟수와 잠금 시간도 변경하지 않는다.
		if (localAuth.isCurrentlyLocked()) {
			return LocalLoginResult.locked();
		}

		// 만료된 잠금 정보는 다음 로그인 요청에서 초기화한다.
		if (localAuth.getLockedUntil() != null) {
			updateOneRow(mapper.resetLoginFailure(localAuth.getUsername()));
			localAuth.setFailedLoginCount(0);
		}

		if (passEncoder.matches(rawPassword, localAuth.getPasswordHash())) {
			// 이전에 비밀번호를 틀린 기록이 있으면 로그인 성공 시 초기화한다.
			if (localAuth.getFailedLoginCount() > 0) {
				updateOneRow(mapper.resetLoginFailure(localAuth.getUsername()));
			}
			// 비밀번호 검증까지 끝난 회원 정보만 로그인 세션 생성 대상으로 전달한다.
			return LocalLoginResult.success(new LoginMemberDto(
					localAuth.getMemberId(),
					localAuth.getNickname(),
					localAuth.getMemberRole()));
		}

		int nextFailedLoginCount = localAuth.getFailedLoginCount() + 1;
		if (nextFailedLoginCount >= MAX_FAILED_LOGIN_COUNT) {
			// 다섯 번째 실패에서 계정을 잠그고, 잠금 중인 요청으로 잠금 시간을 연장하지 않는다.
			updateOneRow(mapper.lockLocalLogin(localAuth.getUsername()));
			return LocalLoginResult.locked();
		}

		updateOneRow(mapper.incrementFailedLoginCount(localAuth.getUsername()));
		return LocalLoginResult.invalidCredentials();
	}

	// 인증 정보가 정확히 한 건 변경되지 않으면 트랜잭션을 롤백한다.
	private void updateOneRow(int updatedRowCount) {
		if (updatedRowCount != 1) {
			throw new IllegalStateException("로그인 인증 정보 갱신에 실패했습니다.");
		}
	}

	// 회원가입
	@Transactional
	public int memberSignUp(
			SignUpRequest signUpRequest,
			VerifiedSignupEmail sessionVerification) {

		// TODO 사업자 승인 기능을 추가할 때 사업자등록증 업로드도 함께 구현한다.
		// 입력값 검증부터 이메일 인증 결과를 회원과 연결하는 작업까지 하나의 트랜잭션으로 진행한다.
		validateSignupRequest(signUpRequest);
		VerifiedSignupEmail verifiedEmail = emailVerificationService
				.requireVerifiedSignupEmail(signUpRequest.getEmail(), sessionVerification);

		Member member = createMember(signUpRequest, verifiedEmail);
		saveMember(member);
		saveLocalAuth(createLocalAuth(member, signUpRequest.getPassword()));
		consumeSignupEmailVerification(verifiedEmail, member);

		return member.getMemberId();
	}

	// 회원가입 입력값 중복 확인
	public int checkLoginId(String loginId) {
		return mapper.checkLoginId(loginId);
	}

	public int checkNickname(String nickname) {
		return mapper.checkNickname(nickname);
	}

	// 회원가입 입력값 검증
	private void validateSignupRequest(SignUpRequest signUpRequest) {
		// 사용자에게 안내 가능한 회원가입 오류를 전용 예외로 전달한다.
		validateNoWhitespace("아이디", signUpRequest.getLoginId());
		validateNoWhitespace("닉네임", signUpRequest.getNickname());
		validateNoWhitespace("이름", signUpRequest.getName());
		validateNoWhitespace("비밀번호", signUpRequest.getPassword());
		validateNoWhitespace("이메일", signUpRequest.getEmail());
		validateNoWhitespace("전화번호", signUpRequest.getPhone());

		if (!signUpRequest.getPassword()
				.equals(signUpRequest.getPasswordConfirm())) {
			throw new SignupException("비밀번호가 비밀번호 확인란과 일치하지 않습니다.");
		}

		if (!signUpRequest.isPrivacyAgreed()) {
			throw new SignupException("개인정보 동의가 필요합니다.");
		}

		if (signUpRequest.getMemberType() != 1
				&& signUpRequest.getMemberType() != 2) {
			throw new SignupException("잘못된 회원 유형");
		}
	}

	// 회원 공통 정보 생성
	private Member createMember(SignUpRequest signUpRequest, VerifiedSignupEmail verifiedEmail) {
		// 이메일은 요청값이 아니라 DB에서 인증 완료를 확인한 값을 사용한다.
		Member member = new Member();
		member.setName(signUpRequest.getName());
		member.setLoginId(signUpRequest.getLoginId());
		member.setEmail(verifiedEmail.getEmail());
		member.setNickname(signUpRequest.getNickname());
		member.setMemberType(signUpRequest.getMemberType());
		member.setPhone(signUpRequest.getPhone());
		member.setGender(signUpRequest.getGender());
		member.setBirth(signUpRequest.getBirth());
		member.setEmailVerified("Y");
		member.setEmailVerifiedAt(verifiedEmail.getVerifiedAt());
		return member;
	}

	// 회원 공통 정보 저장
	private void saveMember(Member member) {
		if (mapper.memberSignUp(member) != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
	}

	// 로컬 로그인 인증 정보 생성
	private MemberLocalAuth createLocalAuth(Member member, String rawPassword) {
		MemberLocalAuth memberLocalAuth = new MemberLocalAuth();
		memberLocalAuth.setMemberId(member.getMemberId());
		memberLocalAuth.setUsername(member.getLoginId());
		memberLocalAuth.setPasswordHash(passEncoder.encode(rawPassword));
		return memberLocalAuth;
	}

	// 로컬 로그인 인증 정보 저장
	private void saveLocalAuth(MemberLocalAuth memberLocalAuth) {
		if (mapper.localMemberJoin(memberLocalAuth) != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
	}

	// 이메일 인증 결과를 회원과 연결
	private void consumeSignupEmailVerification(VerifiedSignupEmail verifiedEmail, Member member) {
		// 인증 행을 새 회원과 연결해 다른 회원가입에서 다시 사용할 수 없게 한다.
		if (mapper.consumeSignupEmailVerification(
				verifiedEmail.getEmailVerificationId(), member.getMemberId()) != 1) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이미 사용되었거나 유효하지 않은 이메일 인증입니다. 다시 인증해주세요."
			);
		}
	}

	// 입력값의 공백 포함 여부 확인
	private void validateNoWhitespace(String fieldName, String value) {
		if (value != null && value.chars().anyMatch(ch -> Character.isWhitespace(ch))) {
			throw new SignupException(fieldName + "에는 공백을 입력할 수 없습니다.");
		}
	}

	public String findId(String name, String email) {
		
		return mapper.findId(name, email);
	}

	// 비밀번호 변경과 인증 결과 소비는 반드시 함께 성공하거나 함께 롤백되어야 한다.
	@Transactional
	public void resetPassword(
			String newPassword,
			VerifiedPasswordReset sessionVerification) {
		// 세션 값만 신뢰하지 않고 최신 DB 인증 행을 잠가 만료·재발송·재사용 여부를 확인한다.
		emailVerificationService.requireVerifiedPasswordReset(sessionVerification);

		String passwordHash = passEncoder.encode(newPassword);
		// 인증 결과를 먼저 소비해 같은 이메일 인증으로 동시 변경 요청이 성공하지 못하게 한다.
		if (mapper.consumePasswordResetVerification(
				sessionVerification.getEmailVerificationId(), sessionVerification.getMemberId()) != 1) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이메일 인증이 만료되었거나 이미 사용되었습니다. 다시 인증해주세요.");
		}

		// 비밀번호 변경 후에는 잠금과 실패 횟수를 초기화해 새 비밀번호로 로그인할 수 있게 한다.
		if (mapper.updatePasswordByMemberId(sessionVerification.getMemberId(), passwordHash) != 1) {
			throw new IllegalStateException("비밀번호 변경에 실패했습니다.");
		}
	}

}
