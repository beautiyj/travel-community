package com.gnagnoohc.travel.auth.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OAuth 시작 의도와 제공자를 callback까지 서버 세션에 보관한다.
 * 한 번 소비한 객체는 재사용하지 않으며 10분이 지나면 새 흐름으로 교체할 수 있다.
 */
public record PendingSocialAuthIntent(
        String intent,
        String registrationId,
        LocalDateTime expiresAt) implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean isExpired() {
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt);
    }
}
