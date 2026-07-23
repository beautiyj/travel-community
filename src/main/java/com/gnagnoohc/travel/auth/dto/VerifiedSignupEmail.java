package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;
import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Alias("verifiedsignupemail")
public class VerifiedSignupEmail implements Serializable {

	private static final long serialVersionUID = 1L;

	// 인증 행의 ID를 세션에 보관해 회원가입 시 같은 인증 결과를 다시 확인한다.
	private Long emailVerificationId;
	private String email;
	private Timestamp verifiedAt;

}
