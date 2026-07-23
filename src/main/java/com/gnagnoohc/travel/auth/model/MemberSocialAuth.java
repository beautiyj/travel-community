package com.gnagnoohc.travel.auth.model;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

/**
 * member_social_auth 테이블과 값을 주고받는 모델이다.
 * 실제 소셜 회원 식별의 원본은 provider와 providerUserId의 조합이다.
 */
@Data
@Alias("membersocialauth")
public class MemberSocialAuth {

    private int socialAuthId;
    private String provider;
    private String providerUserId;
    private String providerEmail;
    private String providerEmailVerifiedYn;
    private String providerName;
    private String providerNickname;
    private String providerProfileImageUrl;
    private Timestamp lastLoginAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int memberId;
}
