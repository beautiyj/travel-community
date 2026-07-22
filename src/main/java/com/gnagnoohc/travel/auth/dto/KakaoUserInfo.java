package com.gnagnoohc.travel.auth.dto;

/**
 * 카카오 사용자 정보 응답에서 소셜 로그인에 필요한 값만 추린 객체다.
 * 카카오의 원본 JSON 구조는 KakaoApiClient 안에서만 처리한다.
 */
public record KakaoUserInfo(
        String providerUserId,
        String email,
        boolean emailValid,
        boolean emailVerified,
        String nickname,
        String profileImageUrl) {

    /**
     * 현재 소셜 가입 골격은 카카오가 유효성과 소유 확인을 끝낸 이메일만 허용한다.
     */
    public boolean hasVerifiedEmail() {
        return email != null
                && !email.isBlank()
                && emailValid
                && emailVerified;
    }
}
