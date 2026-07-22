package com.gnagnoohc.travel.auth.exception;

public class EmailVerificationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	// 만료·세션 없음·이미 사용됨을 프론트엔드가 같은 재인증 흐름으로 처리하도록 통합한다.
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

	// 프론트엔드 동작을 제어해야 하는 이메일 인증 오류에만 처리 코드를 함께 전달한다.
	public EmailVerificationException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
