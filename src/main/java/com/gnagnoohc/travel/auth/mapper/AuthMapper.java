package com.gnagnoohc.travel.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.model.EmailVerification;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberLocalAuth;

@Mapper
public interface AuthMapper {

	// 회원가입 정보 저장
	int memberSignUp(Member member);

	int localMemberJoin(MemberLocalAuth memberLocalAuth);

	// 로그인 검증
	// 활성 로컬 계정의 로그인 검증에 필요한 member_id와 비밀번호 해시만 조회한다.
	MemberLocalAuth findLocalLoginAuth(@Param("loginId") String loginId);

	// 회원가입 입력값 중복 확인
	int checkLoginId(@Param("loginId") String loginId);

	int checkNickname(@Param("nickname") String nickname);

	// 이메일 인증
	// 세션이 가리키는 정확한 인증 행을 잠가 가입 중 재사용을 막는다.
	VerifiedSignupEmail findVerifiedSignupEmailForUpdate(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("email") String email,
			@Param("purpose") String purpose);

	// 발송/검증 시 최신 인증 행을 잠가 동시 요청을 조정한다.
	EmailVerification findLatestEmailVerificationForUpdate(
			@Param("email") String email,
			@Param("purpose") String purpose);

	// 특정 이메일의 최근 24시간 발송 횟수를 조회한다.
	int countEmailVerificationRequestsInLastDay(
			@Param("email") String email,
			@Param("purpose") String purpose);

	// 전체 최근 24시간 발송량을 조회해 SMTP 자원을 보호한다.
	int countAllEmailVerificationRequestsInLastDay();

	// 발송할 때마다 인증번호 해시와 만료 시각을 새 이력으로 저장한다.
	int insertEmailVerification(EmailVerification emailVerification);

	// 오답을 포함한 인증 시도 횟수를 1 증가시킨다.
	int incrementEmailVerificationAttempt(@Param("emailVerificationId") long emailVerificationId);

	// 인증 성공 시각을 기록한다.
	int markEmailVerificationVerified(@Param("emailVerificationId") long emailVerificationId);

	// 회원가입에 사용한 인증 행을 생성된 회원과 연결해 한 번만 소비한다.
	int consumeSignupEmailVerification(
			@Param("emailVerificationId") long emailVerificationId,
			@Param("memberId") int memberId);

}
