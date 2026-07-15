package com.gnagnoohc.travel.auth.model;

import java.sql.Date;
import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("member")
public class Member {
	private int member_id;
	private String name;
	private String login_id;
	private String email;
	private String nickname;
	private int member_type;
	private String phone;
	private String gender;
	private Date birth;
	private String profile_img_url;
	private String signup_type;
	private String member_status;
	private String email_verified;
	private Timestamp email_verified_at;
	private Timestamp last_login_at;
	private Timestamp created_at;
	private Timestamp updated_at;
	private Timestamp deleted_at;
}
