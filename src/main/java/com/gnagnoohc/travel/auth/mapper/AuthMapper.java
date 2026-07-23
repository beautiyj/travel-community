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
	// 같은 계정의 로그인 요청이 동시에 처리되지 않도록 인증 행을 잠가 조회한다.
	MemberLocalAuth findLocalLoginAuthForUpdate(@Param("username") String username);

	int incrementFailedLoginCount(@Param("username") String username);

	int lockLocalLogin(@Param("username") String username);

	int resetLoginFailure(@Param("username") String username);

	// 이메일 인증번호 발송 및 검증
	// 같은 이메일의 발송과 검증이 동시에 처리되지 않도록 최신 인증 행을 잠가 조회한다.
	EmailVerification findLatestEmailVerificationForUpdate(
			@Param("email") String email,
			@Param("purpose") String purpose);

	EmailVerification findLatestPasswordResetVerificationForUpdate(
			@Param("email") String email,
			@Param("memberId") int memberId);

	int countEmailVerificationRequestsInLastDay(
			@Param("email") String email,
			@Param("purpose") String purpose);

	int countAllEmailVerificationRequestsInLastDay();

	int insertEmailVerification(EmailVerification emailVerification);

	int incrementEmailVerificationAttempt(@Param("emailVerificationId") long emailVerificationId);

	int markEmailVerificationVerified(@Param("emailVerificationId") long emailVerificationId);

	// 회원가입에 사용할 이메일 인증 확인
	// 세션에 저장된 인증 행을 잠가 다른 회원가입에서 다시 사용하지 못하게 한다.
	VerifiedSignupEmail findVerifiedSignupEmailForUpdate(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("email") String email,
			@Param("purpose") String purpose);

	int consumeSignupEmailVerification(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("memberId") int memberId);

	Integer findActiveLocalMemberId(
			@Param("username") String username,
			@Param("email") String email);

	int consumePasswordResetVerification(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("memberId") int memberId);

	int updatePasswordByMemberId(
			@Param("memberId") int memberId,
			@Param("passwordHash") String passwordHash);

	String findId(@Param("name") String name, @Param("email") String email);

}
