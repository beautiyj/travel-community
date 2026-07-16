package com.gnagnoohc.travel.auth.model;

import java.sql.Date;
import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("member")
public class Member {
	// Java 프로퍼티는 camelCase로 두고 DB snake_case 컬럼은 AuthMapper에서 연결한다.
	private int memberId;
	private String name;
	private String loginId;
	private String email;
	private String nickname;
	private int memberType;
	private String phone;
	private String gender;
	private Date birth;
	private String profileImgUrl;
	private String signupType;
	private String memberStatus;
	private String emailVerified;
	private Timestamp emailVerifiedAt;
	private Timestamp lastLoginAt;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private Timestamp deletedAt;
}
