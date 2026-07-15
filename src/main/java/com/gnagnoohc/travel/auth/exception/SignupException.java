package com.gnagnoohc.travel.auth.exception;

/**
	* [수정] 회원가입 요청에서 사용자에게 안내할 수 있는 검증/업무 오류를 표현한다.
	*/
public class SignupException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SignupException(String message) {
		super(message);
	}
}
