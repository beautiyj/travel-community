package com.gnagnoohc.travel.auth.exception;

/**
 * 소셜 인증 과정에서 사용자에게 안전하게 안내할 수 있는 실패를 나타낸다.
 */
public class SocialAuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean userVisible;

    public SocialAuthException(String message) {
        super(message);
        this.userVisible = true;
    }

    public SocialAuthException(String message, Throwable cause) {
        super(message, cause);
        this.userVisible = true;
    }

    /**
     * DB 영향 행 수 불일치처럼 사용자에게 원인을 노출하면 안 되는 내부 실패를 구분한다.
     */
    public SocialAuthException(String message, boolean userVisible) {
        super(message);
        this.userVisible = userVisible;
    }

    public boolean isUserVisible() {
        return userVisible;
    }
}
