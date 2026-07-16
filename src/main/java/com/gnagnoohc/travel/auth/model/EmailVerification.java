package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("emailverification")
public class EmailVerification {
	// Java 프로퍼티는 camelCase로 두고 DB snake_case 컬럼은 AuthMapper에서 연결한다.
	private Long emailVerificationId;
	private String email;
	private String purpose;
	private String codeHash;
	private Timestamp expiresAt;
	private Timestamp verifiedAt;
	private int attemptCount;
	private int resendCount;
	private Timestamp createdAt;
	private Long memberId;
}
