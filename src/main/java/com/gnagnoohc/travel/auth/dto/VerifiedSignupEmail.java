package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;
import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

/**
 * 가입용 이메일 인증에 성공한 뒤 세션에 보관하는 최소 증표다.
 * 이 객체가 존재해도 가입 권한이 확정되는 것은 아니며, 서비스가 ID와 이메일로 DB 인증 행을
 * 다시 잠가 최신 발송·만료·소비 상태를 확인해야 한다.
 */
@Getter
@Setter
@Alias("verifiedsignupemail")
public class VerifiedSignupEmail implements Serializable {

	private static final long serialVersionUID = 1L;

	// 인증 행 ID를 키로 사용하므로 인증번호 원문이나 해시는 세션에 저장하지 않는다.
	private Long emailVerificationId;
	private String email;
	private Timestamp verifiedAt;

}
