package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.business.exception.BusinessAuthException;
import jakarta.servlet.http.HttpSession;

// 로그인 세션(loginMember) 해석 + 사업자 권한 판별을 business 컨트롤러들이 공유하는 유틸.
// 세션 키/타입은 로그인 파트(AuthController) 컨벤션: "loginMember" -> LoginMemberDto
public final class BusinessSessionSupport {

    public static final String LOGIN_SESSION_KEY = "loginMember";

    private BusinessSessionSupport() {
        // 유틸 클래스, 인스턴스 생성 방지
    }

    /**
     * 로그인 세션에서 사업자 회원 ID를 얻는다. 미로그인·권한부족이면 BusinessAuthException을 던지고,
     * 각 컨트롤러의 @ExceptionHandler가 화면은 리다이렉트로 / API는 401·403으로 처리한다.
     * 세션의 memberId는 int라 서비스 시그니처(Long)에 맞춰 변환한다.
     */
    public static Long requireBusinessMemberId(HttpSession session) {
        LoginMemberDto login = getLogin(session);
        if (login == null) {
            throw new BusinessAuthException(BusinessAuthException.Reason.NOT_LOGGED_IN);
        }
        if (!isBusiness(login)) {
            throw new BusinessAuthException(BusinessAuthException.Reason.NOT_BUSINESS);
        }
        return (long) login.getMemberId();
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
