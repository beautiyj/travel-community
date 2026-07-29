package com.gnagnoohc.travel.auth.exception;

public class EmailVerificationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	// 이메일 인증을 다시 받아야 하는 경우를 하나의 오류 코드로 전달한다.
	public static final String EMAIL_REVERIFICATION_REQUIRED = "EMAIL_REVERIFICATION_REQUIRED";

	private final String errorCode;

	public EmailVerificationException(String message) {
		super(message);
		this.errorCode = null;
	}

	public EmailVerificationException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = null;
	}

	// 화면에서 별도 처리가 필요한 오류에만 오류 코드를 함께 전달한다.
	public EmailVerificationException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
