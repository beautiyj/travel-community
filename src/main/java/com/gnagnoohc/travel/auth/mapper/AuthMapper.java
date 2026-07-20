package com.gnagnoohc.travel.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.model.EmailVerification;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberLocalAuth;

@Mapper
public interface AuthMapper {

	// 회원가입
	int checkLoginId(@Param("loginId") String loginId);

	int checkNickname(@Param("nickname") String nickname);

	int memberSignUp(Member member);

	int localMemberJoin(MemberLocalAuth memberLocalAuth);

	// 로컬 로그인 및 계정 잠금
	// 동일 계정의 로그인 요청을 직렬화하기 위해 인증 행을 잠가 조회한다.
	MemberLocalAuth findLocalLoginAuthForUpdate(@Param("username") String username);

	int incrementFailedLoginCount(@Param("username") String username);

	int lockLocalLogin(@Param("username") String username);

	int resetLoginFailure(@Param("username") String username);

	// 이메일 인증번호 발송 및 검증
	// 동일 이메일의 발송·검증 요청을 직렬화하기 위해 최신 인증 행을 잠가 조회한다.
	EmailVerification findLatestEmailVerificationForUpdate(
			@Param("email") String email,
			@Param("purpose") String purpose);

	int countEmailVerificationRequestsInLastDay(
			@Param("email") String email,
			@Param("purpose") String purpose);

	int countAllEmailVerificationRequestsInLastDay();

	int insertEmailVerification(EmailVerification emailVerification);

	int incrementEmailVerificationAttempt(@Param("emailVerificationId") long emailVerificationId);

	int markEmailVerificationVerified(@Param("emailVerificationId") long emailVerificationId);

	// 회원가입 이메일 인증 결과 소비
	// 세션이 가리키는 인증 행을 잠가 한 번만 회원가입에 사용되도록 한다.
	VerifiedSignupEmail findVerifiedSignupEmailForUpdate(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("email") String email,
			@Param("purpose") String purpose);

	int consumeSignupEmailVerification(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("memberId") int memberId);

}
