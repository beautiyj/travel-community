package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 비밀번호 재설정 이메일 인증이 끝난 뒤 서버 세션에만 보관하는 증표다.
 */
@Getter
@Setter
public class VerifiedPasswordReset implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long emailVerificationId;
	private Integer memberId;
	private String email;
}
