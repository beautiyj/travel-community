package com.gnagnoohc.travel.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberSocialAuth;

/**
 * 소셜 회원의 공통 회원 정보와 제공자 인증 정보 저장·조회를 담당한다.
 */
@Mapper
public interface SocialAuthMapper {

    Member findMemberBySocialIdentity(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId);

    // 소셜 회원은 member_local_auth 없이 공통 member 행만 먼저 저장한다.
    int insertSocialMember(Member member);

    int insertSocialAuth(MemberSocialAuth socialAuth);

    int updateSocialLastLogin(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId);

    int updateMemberLastLogin(@Param("memberId") int memberId);
}
