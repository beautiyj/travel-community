package com.gnagnoohc.travel.auth.dto;

import java.util.Objects;

/**
 * 로컬 로그인 처리 결과를 서비스에서 컨트롤러로 전달한다.
 * 로그인에 성공한 경우에만 세션에 저장할 회원 정보를 포함한다.
 */
public record LocalLoginResult(LoginStatus status, LoginMemberDto loginMember) {

	public enum LoginStatus {
		SUCCESS,
		INVALID_CREDENTIALS,
		LOCKED
	}

	public static LocalLoginResult success(LoginMemberDto loginMember) {
		return new LocalLoginResult(LoginStatus.SUCCESS,
				Objects.requireNonNull(loginMember, "loginMember must not be null"));
	}

	public static LocalLoginResult invalidCredentials() {
		return new LocalLoginResult(LoginStatus.INVALID_CREDENTIALS, null);
	}

	public static LocalLoginResult locked() {
		return new LocalLoginResult(LoginStatus.LOCKED, null);
	}
}
