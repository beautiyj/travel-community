package com.gnagnoohc.travel.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.auth.dto.LocalLoginResult;
import com.gnagnoohc.travel.auth.dto.SignUpRequest;
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
		// 같은 계정의 동시 요청을 직렬화해 실패 횟수와 잠금 시각을 일관되게 변경한다.
		if (username == null || username.isBlank()
				|| rawPassword == null || rawPassword.isBlank()) {
			return LocalLoginResult.invalidCredentials();
		}

		// member_local_auth의 기본키인 username으로 로컬 인증 정보를 조회한다.
		MemberLocalAuth localAuth = mapper.findLocalLoginAuthForUpdate(username.trim());
		if (localAuth == null) {
			return LocalLoginResult.invalidCredentials();
		}

		// 잠금 중에는 비밀번호 검사, 실패 횟수 증가, 잠금시간 연장을 모두 하지 않는다.
		if (localAuth.isCurrentlyLocked()) {
			return LocalLoginResult.locked();
		}

		// 만료된 잠금은 다음 로그인 요청에서 초기화하므로 별도 스케줄러가 필요 없다.
		if (localAuth.getLockedUntil() != null) {
			updateOneRow(mapper.resetLoginFailure(localAuth.getUsername()));
			localAuth.setFailedLoginCount(0);
		}

		if (passEncoder.matches(rawPassword, localAuth.getPasswordHash())) {
			// 잠금 이력이 아닌 일반 실패 이력은 로그인 성공 시 초기화한다.
			if (localAuth.getFailedLoginCount() > 0) {
				updateOneRow(mapper.resetLoginFailure(localAuth.getUsername()));
			}
			return LocalLoginResult.success(localAuth.getMemberId());
		}

		int nextFailedLoginCount = localAuth.getFailedLoginCount() + 1;
		if (nextFailedLoginCount >= MAX_FAILED_LOGIN_COUNT) {
			// 다섯 번째 실패가 잠금을 만들며, 잠금시간은 이후 요청으로 연장되지 않는다.
			updateOneRow(mapper.lockLocalLogin(localAuth.getUsername()));
			return LocalLoginResult.locked();
		}

		updateOneRow(mapper.incrementFailedLoginCount(localAuth.getUsername()));
		return LocalLoginResult.invalidCredentials();
	}

	// 행 잠금 후의 갱신이 누락되면 인증 상태가 불일치하므로 트랜잭션을 롤백한다.
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

		// 사업자 등록증 파일 업로드는 관리자 승인 기능 추가 후 구현한다.
		//		String filename = mf.getOriginalFilename();
		//		int size = (int)mf.getSize();
		//
		//		String path = session.getServletContext().getRealPath("businessRegister");
		//		int businessUpload = 0;
		//		String newfilename = "";
		//
		//		String extension = filename.substring(filename.lastIndexOf("."), filename.length());
		//
		//		UUID uuid = UUID.randomUUID();
		//
		//		newfilename = path(저장소) + uuid.toString() + extension;
		//		System.out.println("newfilename:" + newfilename);
		//
		//		member.setProfileImgUrl(newfilename);

		// 회원가입 흐름은 검증, 회원 저장, 로컬 인증 저장, 이메일 인증 사용 처리 순으로 진행한다.
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

	// 회원 공통 정보 저장
	private Member createMember(SignUpRequest signUpRequest, VerifiedSignupEmail verifiedEmail) {
		// 이메일은 클라이언트 입력값이 아닌 DB 인증 완료 상태의 값을 사용한다.
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

	// 회원 공통 정보 DB 저장
	private void saveMember(Member member) {
		if (mapper.memberSignUp(member) != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
	}

	// 로컬 로그인 인증 정보 저장
	private MemberLocalAuth createLocalAuth(Member member, String rawPassword) {
		MemberLocalAuth memberLocalAuth = new MemberLocalAuth();
		memberLocalAuth.setMemberId(member.getMemberId());
		memberLocalAuth.setUsername(member.getLoginId());
		memberLocalAuth.setPasswordHash(passEncoder.encode(rawPassword));
		return memberLocalAuth;
	}

	// 로컬 로그인 인증 정보 DB 저장
	private void saveLocalAuth(MemberLocalAuth memberLocalAuth) {
		if (mapper.localMemberJoin(memberLocalAuth) != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
	}

	// 이메일 인증 이력 사용 처리
	private void consumeSignupEmailVerification(VerifiedSignupEmail verifiedEmail, Member member) {
		// 회원 정보 저장과 같은 트랜잭션에서 인증 행을 회원과 연결해 재사용을 막는다.
		if (mapper.consumeSignupEmailVerification(
				verifiedEmail.getEmailVerificationId(), member.getMemberId()) != 1) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이미 사용되었거나 유효하지 않은 이메일 인증입니다. 다시 인증해주세요."
			);
		}
	}

	// 공통 입력값 검증
	private void validateNoWhitespace(String fieldName, String value) {
		if (value != null && value.chars().anyMatch(ch -> Character.isWhitespace(ch))) {
			throw new SignupException(fieldName + "에는 공백을 입력할 수 없습니다.");
		}
	}

}
