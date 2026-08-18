package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OAuth 인증 후 기존 로컬 회원과의 연동을 완료하기 전까지 서버 세션에만 보관한다.
 * 브라우저가 연동 대상 회원이나 소셜 계정 식별자를 바꾸지 못하도록 원본을 모두 포함한다.
 * mode는 신규 연동, 탈퇴 회원 복구 후 연동, 기존 연동 복구 중 허용할 상태 전이를 제한한다.
 */
public record PendingSocialLink(
        int candidateMemberId,
        String provider,
        String providerUserId,
        String email,
        String providerNickname,
        String profileImageUrl,
        String maskedUsername,
        String linkNonce,
        String mode,
        LocalDateTime expiresAt) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LINK = "LINK";
    public static final String REACTIVATE_AND_LINK = "REACTIVATE_AND_LINK";
    public static final String REACTIVATE_EXISTING_LINK = "REACTIVATE_EXISTING_LINK";

    public boolean isExpired() {
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt);
    }
}
