package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

/**
 * 이메일 인증번호의 해시, 만료 시각, 인증 상태와 시도 횟수를 관리한다.
 */
@Data
@Alias("emailverification")
public class EmailVerification {
	private Long emailVerificationId;
	private String email;
	private String purpose;
	private String codeHash;
	private Timestamp expiresAt;
	private Timestamp verifiedAt;
	private int attemptCount;
	private Timestamp createdAt;
	private Long memberId;
	// 이메일 발송을 요청한 클라이언트 IP를 저장해 IP별 발송 제한에 사용한다.
	private String requestIp;
	// 비밀번호 재설정 등에서 같은 인증 결과를 한 번만 사용하도록 기록한다.
	private Timestamp consumedAt;
}
