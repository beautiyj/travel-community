package com.gnagnoohc.travel.auth.model;

import java.sql.Date;
import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

/**
 * 회원 공통 정보를 담아 member 테이블과 주고받는다.
 */
@Data
@Alias("member")
public class Member {
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
