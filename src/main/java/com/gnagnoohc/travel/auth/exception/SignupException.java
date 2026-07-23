package com.gnagnoohc.travel.auth.exception;

/**
 * 회원가입 과정에서 사용자에게 안내할 수 있는 입력값 또는 처리 오류를 나타낸다.
 */
public class SignupException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SignupException(String message) {
		super(message);
	}
}
