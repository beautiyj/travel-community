package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;

import org.apache.ibatis.type.Alias;

import lombok.Getter;
import lombok.Setter;

/**
 * 탈퇴 로컬 회원에게 인증번호를 보낸 뒤에만 서버 세션에 보관하는 재활성화 대상 증표다.
 * 클라이언트가 memberId나 가입 이메일을 선택해서 전달하지 못하도록 서버가 DB 조회 결과만 담는다.
 */
@Getter
@Setter
@Alias("pendingmemberreactivation")
public class PendingMemberReactivation implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer memberId;
	private String email;
}
