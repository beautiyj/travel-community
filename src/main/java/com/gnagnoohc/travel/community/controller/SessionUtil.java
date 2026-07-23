package com.gnagnoohc.travel.community.controller;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

// CommunityController / CommentController 양쪽에 똑같이 있던
// 세션(loginMember) 추출 코드가 중복이라 뺀 유틸 (컨트롤러 안에서만 겹치는 코드라 controller 패키지에 둠)
// session.setAttribute("loginMember", loginResult.loginMember())로
// LoginMemberDto(memberId: int, nickname, memberRole)가 세션에 그대로 들어옴
public class SessionUtil {

	private SessionUtil() {
		// 유틸 클래스, 인스턴스 생성 방지
	}

	// 세션 로그인 정보에서 memberId 꺼내기
	public static int getMemberId(Object login) {
		return ((LoginMemberDto) login).getMemberId();
	}

	// 세션 로그인 정보에서 memberRole 꺼내기 ('USER' / 'BUSINESS' / 'ADMIN')
	public static String getMemberRole(Object login) {
		return ((LoginMemberDto) login).getMemberRole();
	}

	// 세션 로그인 정보에서 nickname 꺼내기 (필요한 화면에서 바로 쓰고 싶을 때)
	public static String getNickname(Object login) {
		return ((LoginMemberDto) login).getNickname();
	}
}