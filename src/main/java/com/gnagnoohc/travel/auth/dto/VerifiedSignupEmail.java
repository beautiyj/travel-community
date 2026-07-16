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

	// 세션의 인증 결과가 정확한 DB 인증 행을 가리키도록 PK를 함께 보관한다.
	private Long emailVerificationId;
	private String email;
	private Timestamp verifiedAt;

}
