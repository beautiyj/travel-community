package com.gnagnoohc.travel.auth.dto;

import java.sql.Date;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 화면에서 전달되는 입력만 표현한다.
 */
@Getter
@Setter
@Alias("signuprequest")
public class SignUpRequest {

	private int memberType;
	private String name;
	private String loginId;
	private String password;
	private String passwordConfirm;
	private String email;
	private String nickname;
	private Date birth;
	private String phone;
	private String gender;
	private boolean privacyAgreed;
}
