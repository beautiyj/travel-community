package com.gnagnoohc.travel.auth.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.dto.SocialSignupRequest;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;
import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.mapper.SocialAuthMapper;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberSocialAuth;

import lombok.RequiredArgsConstructor;

/**
 * 우리 서비스의 소셜 회원 조회와 가입 규칙을 담당한다.
 * 외부 OAuth 통신은 Spring Security가 담당하고 이 서비스에는 검증된 값만 전달한다.
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private static final String KAKAO = "KAKAO";

    private final AuthMapper authMapper;
    private final SocialAuthMapper socialAuthMapper;

    /**
     * (provider, providerUserId)로 회원을 찾고 활성 회원의 마지막 로그인 시간을 갱신한다.
     * 미가입 회원이면 null을 반환해 OAuth 성공 핸들러가 신규가입 흐름을 시작하게 한다.
     */
    @Transactional
    public LoginMemberDto findSocialLoginMember(String provider, String providerUserId) {
        validateProviderUserId(provider, providerUserId);

        Member member = socialAuthMapper.findMemberBySocialIdentity(provider, providerUserId);
        if (member == null) {
            return null;
        }
        if (!"ACTIVE".equals(member.getMemberStatus()) || member.getDeletedAt() != null) {
            throw new SocialAuthException("현재 로그인할 수 없는 회원 계정입니다.");
        }
        if (member.getMemberRole() == null || member.getMemberRole().isBlank()) {
            throw new SocialAuthException("회원 권한 정보를 확인할 수 없습니다.", false);
        }

        // 두 마지막 로그인 시각은 같은 DB 트랜잭션에서 함께 반영하거나 함께 롤백한다.
        if (socialAuthMapper.updateMemberLastLogin(member.getMemberId()) != 1
                || socialAuthMapper.updateSocialLastLogin(provider, providerUserId) != 1) {
            throw new SocialAuthException("소셜 로그인 시각 갱신에 실패했습니다.", false);
        }

        return new LoginMemberDto(
                member.getMemberId(),
                member.getNickname(),
                member.getMemberRole());
    }

    /**
     * member와 member_social_auth INSERT를 하나의 트랜잭션으로 처리한다.
     * providerUserId와 검증된 이메일은 요청 DTO가 아니라 pendingSignup에서만 가져온다.
     */
    @Transactional
    public LoginMemberDto registerSocialMember(
            PendingSocialSignup pendingSignup,
            SocialSignupRequest signupRequest) {
        validatePendingSignup(pendingSignup);
        validateSignupRequest(signupRequest);

        // 사전 검사는 사용자 안내용이고, 동시 요청의 최종 방어는 DB UNIQUE 제약이다.
        if (socialAuthMapper.findMemberBySocialIdentity(
                pendingSignup.provider(), pendingSignup.providerUserId()) != null) {
            throw new SocialAuthException("이미 가입된 소셜 계정입니다. 로그인해 주세요.");
        }
        if (authMapper.checkNickname(signupRequest.getNickname()) > 0) {
            throw new SocialAuthException("이미 사용 중인 닉네임입니다.");
        }

        Member member = createMember(pendingSignup, signupRequest);
        MemberSocialAuth socialAuth = createSocialAuth(pendingSignup);

        try {
            if (socialAuthMapper.insertSocialMember(member) != 1 || member.getMemberId() <= 0) {
                throw new SocialAuthException("소셜 회원 공통 정보 저장에 실패했습니다.", false);
            }

            socialAuth.setMemberId(member.getMemberId());
            if (socialAuthMapper.insertSocialAuth(socialAuth) != 1) {
                throw new SocialAuthException("소셜 인증 정보 저장에 실패했습니다.", false);
            }
        } catch (DuplicateKeyException e) {
            // 중복 클릭이나 동시 가입 요청도 내부 SQL 내용을 노출하지 않고 안전한 메시지로 변환한다.
            throw new SocialAuthException(
                    "이미 가입된 소셜 계정이거나 사용 중인 닉네임입니다.", e);
        }

        return new LoginMemberDto(
                member.getMemberId(),
                member.getNickname(),
                member.getMemberRole());
    }

    private Member createMember(
            PendingSocialSignup pendingSignup,
            SocialSignupRequest signupRequest) {
        String provider = pendingSignup.provider();
        Member member = new Member();
        member.setName(signupRequest.getName());
        member.setLoginId(provider + "_" + pendingSignup.providerUserId());
        member.setEmail(pendingSignup.email());
        member.setNickname(signupRequest.getNickname());
        member.setMemberType(1);
        member.setProfileImgUrl(limitOptional(pendingSignup.profileImageUrl(), 500));
        member.setSignupType(provider);
        member.setMemberStatus("ACTIVE");
        member.setMemberRole("USER");
        member.setEmailVerified("Y");
        member.setEmailVerifiedAt(Timestamp.valueOf(LocalDateTime.now()));
        return member;
    }

    private MemberSocialAuth createSocialAuth(PendingSocialSignup pendingSignup) {
        MemberSocialAuth socialAuth = new MemberSocialAuth();
        socialAuth.setProvider(pendingSignup.provider());
        socialAuth.setProviderUserId(pendingSignup.providerUserId());
        socialAuth.setProviderEmail(pendingSignup.email());
        socialAuth.setProviderEmailVerifiedYn("Y");
        socialAuth.setProviderNickname(limitOptional(pendingSignup.providerNickname(), 100));
        socialAuth.setProviderProfileImageUrl(limitOptional(pendingSignup.profileImageUrl(), 500));
        return socialAuth;
    }

    private void validateProviderUserId(String provider, String providerUserId) {
        if (!KAKAO.equals(provider)
                || providerUserId == null
                || !providerUserId.matches("^[0-9]+$")
                || (provider + "_" + providerUserId).length() > 100) {
            throw new SocialAuthException("유효하지 않은 카카오 회원 식별 정보입니다.");
        }
    }

    private void validatePendingSignup(PendingSocialSignup pendingSignup) {
        if (pendingSignup == null || pendingSignup.isExpired()) {
            throw new SocialAuthException("소셜 인증 정보가 없거나 만료됐습니다. 다시 로그인해 주세요.");
        }
        // 현재 실제 연동이 끝난 제공자는 카카오뿐이므로 임의 제공자 가입은 허용하지 않는다.
        if (!KAKAO.equals(pendingSignup.provider())) {
            throw new SocialAuthException("현재 지원하지 않는 소셜 로그인 제공자입니다.");
        }
        validateProviderUserId(pendingSignup.provider(), pendingSignup.providerUserId());
        if (!pendingSignup.emailVerified()
                || pendingSignup.email() == null
                || pendingSignup.email().isBlank()
                || pendingSignup.email().length() > 100) {
            throw new SocialAuthException("소셜 로그인에서 확인된 이메일 정보가 유효하지 않습니다.");
        }
    }

    private void validateSignupRequest(SocialSignupRequest signupRequest) {
        if (signupRequest == null
                || signupRequest.getName() == null
                || signupRequest.getName().length() < 2
                || signupRequest.getName().length() > 20
                || signupRequest.getName().matches(".*\\s.*")
                || signupRequest.getNickname() == null
                || !signupRequest.getNickname().matches("^[^\\s]{2,10}$")
                || !signupRequest.isPrivacyAgreed()) {
            throw new SocialAuthException("소셜 회원가입 입력값을 다시 확인해 주세요.");
        }
    }

    private String limitOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // 표시용 외부 정보가 DB 컬럼보다 길면 인증 원본과 무관하므로 안전하게 제한한다.
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
