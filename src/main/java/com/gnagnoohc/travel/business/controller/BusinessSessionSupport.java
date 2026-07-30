package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import jakarta.servlet.http.HttpSession;

// 로그인 세션(loginMember) 해석 + 사업자 권한 판별을 business 컨트롤러들이 공유하는 유틸.
// 세션 키/타입은 로그인 파트(AuthController) 컨벤션: "loginMember" -> LoginMemberDto
public final class BusinessSessionSupport {

    public static final String LOGIN_SESSION_KEY = "loginMember";

    private BusinessSessionSupport() {
        // 유틸 클래스, 인스턴스 생성 방지
    }

    // 세션에서 로그인 정보를 꺼낸다. 미로그인/타입 불일치면 null
    public static LoginMemberDto getLogin(HttpSession session) {
        Object login = session.getAttribute(LOGIN_SESSION_KEY);
        return (login instanceof LoginMemberDto dto) ? dto : null;
    }

    // 사업자 화면 접근 권한 (사업자 회원만)
    public static boolean isBusiness(LoginMemberDto login) {
        return "BUSINESS".equals(login.getMemberRole());
    }
}
