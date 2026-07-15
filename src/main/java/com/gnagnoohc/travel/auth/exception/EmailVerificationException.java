package com.gnagnoohc.travel.auth.exception;

public class EmailVerificationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailVerificationException(String message) {
		super(message);
	}

	public EmailVerificationException(String message, Throwable cause) {
		super(message, cause);
	}
}
