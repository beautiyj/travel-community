package com.gnagnoohc.travel.auth.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인한 회원의 정보를 세션과 다른 패키지에서 공통으로 사용하기 위한 DTO다.
 */
@Getter
@AllArgsConstructor
public class LoginMemberDto implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final int memberId;
	private final String nickname;
	// 마이페이지에서 회원 유형을 구분할 수 있도록 DB의 member_type 값을 보관한다.
	private final int memberType;
	// DB에서 조회한 member_role 문자열을 그대로 저장한다.
	private final String memberRole;
}
