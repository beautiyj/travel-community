package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("memberlocalauth")
public class MemberLocalAuth {
	// Java 프로퍼티는 camelCase로 두고 DB snake_case 컬럼은 AuthMapper에서 연결한다.
	private String username;
	private String passwordHash;
	private Timestamp passwordUpdatedAt;
	private int failedLoginCount;
	private Timestamp lockedUntil;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private int memberId;

}
