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
	// 로그인 조회 시 DB 현재 시각을 기준으로 계산한 잠금 상태다.
	private boolean currentlyLocked;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private int memberId;

}
