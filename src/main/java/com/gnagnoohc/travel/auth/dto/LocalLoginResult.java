package com.gnagnoohc.travel.auth.dto;

/**
 * 로그인 처리의 정상적인 결과를 컨트롤러에 전달한다.
 * 실패 사유는 내부에서 구분하되 화면에서는 동일한 오류 메시지를 사용한다.
 */
public record LocalLoginResult(LoginStatus status, Integer memberId) {

	public enum LoginStatus {
		SUCCESS,
		INVALID_CREDENTIALS,
		LOCKED
	}

	public static LocalLoginResult success(int memberId) {
		return new LocalLoginResult(LoginStatus.SUCCESS, memberId);
	}

	public static LocalLoginResult invalidCredentials() {
		return new LocalLoginResult(LoginStatus.INVALID_CREDENTIALS, null);
	}

	public static LocalLoginResult locked() {
		return new LocalLoginResult(LoginStatus.LOCKED, null);
	}

	public boolean isSuccess() {
		return status == LoginStatus.SUCCESS;
	}
}
