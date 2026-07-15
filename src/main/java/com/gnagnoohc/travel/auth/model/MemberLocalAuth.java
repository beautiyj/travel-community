package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("memberlocalauth")
public class MemberLocalAuth {
	private String username;
	private String password_hash;
	private Timestamp password_updated_at;
	private int failed_login_count;
	private Timestamp locked_until;
	private Timestamp created_at;
	private Timestamp updated_at;
	private int member_id;

}
