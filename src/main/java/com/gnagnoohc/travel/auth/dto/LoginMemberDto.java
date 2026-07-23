package com.gnagnoohc.travel.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인한 회원의 정보를 세션과 다른 패키지에서 공통으로 사용하기 위한 DTO다.
 */
@Getter
@AllArgsConstructor
public class LoginMemberDto {

	private int memberId;
	private String nickname;
	// DB에서 조회한 member_role 문자열을 그대로 저장한다.
	private String memberRole;
}
