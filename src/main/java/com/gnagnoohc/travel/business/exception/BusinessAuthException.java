package com.gnagnoohc.travel.business.exception;

// business 화면/API 접근 시 인증·권한이 없을 때 던지는 예외.
// 실패 사유(reason)만 담고, 그 사유를 화면용 리다이렉트로 볼지 API용 상태코드로 볼지는
// 각 컨트롤러의 @ExceptionHandler가 정한다 (화면: /auth/login·/ 로 리다이렉트, API: 401·403).
public class BusinessAuthException extends RuntimeException {

    public enum Reason {
        // 미로그인 -> 화면은 로그인 페이지로, API는 401
        NOT_LOGGED_IN("/auth/login"),
        // 로그인했지만 사업자 회원이 아님 -> 화면은 홈으로, API는 403
        NOT_BUSINESS("/");

        private final String redirectTarget;

        Reason(String redirectTarget) {
            this.redirectTarget = redirectTarget;
        }

        public String getRedirectTarget() {
            return redirectTarget;
        }
    }

    private final Reason reason;

    public BusinessAuthException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public String getRedirectTarget() {
        return reason.getRedirectTarget();
    }
}
