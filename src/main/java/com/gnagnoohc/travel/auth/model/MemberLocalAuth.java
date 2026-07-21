package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

/**
 * 로컬 로그인 비밀번호와 로그인 실패 횟수, 임시 잠금 상태를 관리한다.
 */
@Data
@Alias("memberlocalauth")
public class MemberLocalAuth {
	private String username;
	private String passwordHash;
	private Timestamp passwordUpdatedAt;
	private int failedLoginCount;
	private Timestamp lockedUntil;
	// DB 현재 시각을 기준으로 계정이 잠겨 있는지 나타낸다.
	private boolean currentlyLocked;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private int memberId;

}
