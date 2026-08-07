package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 재활성화 전용 인증번호를 검증한 뒤 최종 상태 변경에만 쓰는 서버 세션 증표다.
 * 인증번호 원문이나 해시는 세션에 저장하지 않고 DB 행 식별자만 보관한다.
 */
@Getter
@Setter
public class VerifiedMemberReactivation implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long emailVerificationId;
	private Integer memberId;
	private String email;
}
