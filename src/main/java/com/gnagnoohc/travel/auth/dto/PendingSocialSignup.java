package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 소셜 인증은 끝났지만 우리 서비스의 추가 회원정보 입력이 끝나지 않은 가입 대기 상태다.
 * provider와 providerUserId는 브라우저에서 다시 받지 않고 서버 세션에만 저장한다.
 */
public record PendingSocialSignup(
        String provider,
        String providerUserId,
        String email,
        String providerNickname,
        String profileImageUrl,
        String signupNonce,
        boolean emailVerified,
        LocalDateTime expiresAt) implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean isExpired() {
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt);
    }
}
