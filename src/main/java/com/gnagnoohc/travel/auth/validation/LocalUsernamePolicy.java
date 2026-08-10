package com.gnagnoohc.travel.auth.validation;

public final class LocalUsernamePolicy {

	public static final String REGEX = "^[a-z0-9]{5,20}$";
	public static final String MESSAGE = "아이디는 소문자 영문 또는 숫자 5~20자로 입력해주세요.";

	private LocalUsernamePolicy() {
	}

	public static boolean isValid(String username) {
		return username != null && username.matches(REGEX);
	}
}
