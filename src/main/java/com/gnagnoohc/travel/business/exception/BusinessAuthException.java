package com.gnagnoohc.travel.business.exception;

// business 화면 접근 시 인증/권한이 없을 때, 이동시킬 목적지를 담아 던지는 예외.
// (미로그인 -> /auth/login, 권한 부족 -> / 등)
public class BusinessAuthException extends RuntimeException {

    private final String redirectTarget;

    public BusinessAuthException(String redirectTarget) {
        this.redirectTarget = redirectTarget;
    }

    public String getRedirectTarget() {
        return redirectTarget;
    }
}
