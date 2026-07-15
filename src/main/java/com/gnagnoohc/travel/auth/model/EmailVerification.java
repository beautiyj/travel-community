package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("emailverification")
public class EmailVerification {
	private Long email_verification_id;
	private String email;
	private String purpose;
	private String code_hash;
	private Timestamp expires_at;
	private Timestamp verified_at;
	private int attempt_count;
	private int resend_count;
	private Timestamp created_at;
	private Long member_id;
}
