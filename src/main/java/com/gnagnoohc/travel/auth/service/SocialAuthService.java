package com.gnagnoohc.travel.auth.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.PendingSocialLink;
import com.gnagnoohc.travel.auth.dto.PendingSocialSignup;
import com.gnagnoohc.travel.auth.dto.SocialSignupRequest;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.exception.SocialAuthException;
import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.mapper.SocialAuthMapper;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberSocialAuth;

import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인, 신규 가입, 기존 로컬 계정 연동 규칙을 담당한다.
 * 외부 OAuth 통신은 Spring Security가 담당하고 이 서비스에는 검증된 값만 전달한다.
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private static final String KAKAO = "KAKAO";
    private static final String GOOGLE = "GOOGLE";
    private static final String NAVER = "NAVER";
    private static final int LINK_VALID_MINUTES = 10;

    private final AuthMapper authMapper;
    private final SocialAuthMapper socialAuthMapper;
    private final EmailVerificationService emailVerificationService;

    /**
     * LOGIN 의도에서만 사용한다. 연결 회원 조회와 마지막 로그인 갱신을 한 트랜잭션으로 처리한다.
     */
    @Transactional
    public LoginMemberDto findSocialLoginMember(String provider, String providerUserId) {
        validateProviderUserId(provider, providerUserId);

        Member member = socialAuthMapper.findMemberBySocialIdentity(provider, providerUserId);
        if (member == null) {
            return null;
        }
        validateActiveMember(member);

        // 두 마지막 로그인 시각은 같은 DB 트랜잭션에서 함께 반영하거나 함께 롤백한다.
        if (socialAuthMapper.updateMemberLastLogin(member.getMemberId()) != 1
                || socialAuthMapper.updateSocialLastLogin(provider, providerUserId) != 1) {
            throw new SocialAuthException("소셜 로그인 시각 갱신에 실패했습니다.", false);
        }

        return toLoginMember(member);
    }

    /**
     * SIGNUP 의도에서 신규 가입과 기존 계정 연동을 구분한다.
     * 반환값이 null이면 같은 이메일 회원이 없으므로 신규 소셜 가입을 진행할 수 있다.
     */
    @Transactional(readOnly = true)
    public PendingSocialLink prepareSocialLink(
            String provider,
            String providerUserId,
            String verifiedEmail,
            String providerNickname,
            String profileImageUrl) {
        validateProviderUserId(provider, providerUserId);

        Member identityMember = socialAuthMapper.findMemberBySocialIdentity(provider, providerUserId);
        if (identityMember != null) {
            if ("ACTIVE".equals(identityMember.getMemberStatus())
                    && identityMember.getDeletedAt() == null) {
                throw new SocialAuthException(
                        SocialAuthException.ALREADY_LINKED,
                        "이미 다른 회원 정보와 연동된 소셜 계정입니다.");
            }
            if (!"WITHDRAWN".equals(identityMember.getMemberStatus())
                    || identityMember.getDeletedAt() == null
                    || !sameNormalizedEmail(verifiedEmail, identityMember.getEmail())) {
                throw linkUnavailable();
            }

            String username = socialAuthMapper.findLocalUsernameByMemberId(
                    identityMember.getMemberId());
            MemberSocialAuth existingSocial = socialAuthMapper.findSocialAuthByMemberId(
                    identityMember.getMemberId());
            if (username == null
                    || existingSocial == null
                    || !sameIdentity(existingSocial, provider, providerUserId)) {
                // 소셜 전용 탈퇴 회원은 로컬 비밀번호로 소유권을 확인할 수 없으므로 자동 복구하지 않는다.
                throw linkUnavailable();
            }
            return createPendingLink(
                    identityMember.getMemberId(),
                    provider,
                    providerUserId,
                    verifiedEmail,
                    providerNickname,
                    profileImageUrl,
                    username,
                    PendingSocialLink.REACTIVATE_EXISTING_LINK);
        }

        Member emailMember = socialAuthMapper.findMemberByEmail(verifiedEmail);
        if (emailMember == null) {
            return null;
        }

        boolean active = "ACTIVE".equals(emailMember.getMemberStatus())
                && emailMember.getDeletedAt() == null;
        boolean withdrawn = "WITHDRAWN".equals(emailMember.getMemberStatus())
                && emailMember.getDeletedAt() != null;
        if (!active && !withdrawn) {
            // 정지·비활성 회원이 같은 이메일로 새 계정을 만들어 상태 제한을 우회하지 못하게 한다.
            throw linkUnavailable();
        }

        String username = socialAuthMapper.findLocalUsernameByMemberId(emailMember.getMemberId());
        if (username == null) {
            // 같은 이메일의 소셜 전용 회원은 확인할 로컬 비밀번호가 없으므로 연동 대상으로 삼지 않는다.
            throw linkUnavailable();
        }
        if (socialAuthMapper.findSocialAuthByMemberId(emailMember.getMemberId()) != null) {
            throw new SocialAuthException(
                    SocialAuthException.ALREADY_LINKED,
                    "해당 회원에는 이미 소셜 계정이 연동되어 있습니다.");
        }

        return createPendingLink(
                emailMember.getMemberId(),
                provider,
                providerUserId,
                verifiedEmail,
                providerNickname,
                profileImageUrl,
                username,
                active ? PendingSocialLink.LINK : PendingSocialLink.REACTIVATE_AND_LINK);
    }

    /**
     * 로컬 가입과 같은 규칙으로 OAuth 이메일을 정규화하고 형식을 검사한다.
     */
    public String normalizeVerifiedOAuthEmail(String rawEmail, boolean emailVerified) {
        if (!emailVerified) {
            throw new SocialAuthException("소셜 계정에서 검증된 이메일 제공 동의가 필요합니다.");
        }
        try {
            return emailVerificationService.normalizeAndValidateEmail(rawEmail);
        } catch (EmailVerificationException e) {
            throw new SocialAuthException("소셜 로그인에서 확인된 이메일 정보가 유효하지 않습니다.");
        }
    }

    /**
     * member와 member_social_auth INSERT를 하나의 트랜잭션으로 처리한다.
     * provider 식별자와 이메일은 요청 DTO가 아니라 pendingSignup에서만 가져온다.
     */
    @Transactional
    public LoginMemberDto registerSocialMember(
            PendingSocialSignup pendingSignup,
            SocialSignupRequest signupRequest) {
        validatePendingSignup(pendingSignup);
        validateSignupBusinessRules(signupRequest);

        // 사전 검사는 안내용이며, 동시 요청의 최종 방어는 이메일·소셜 식별자 UNIQUE 제약이다.
        if (socialAuthMapper.findMemberBySocialIdentity(
                pendingSignup.provider(), pendingSignup.providerUserId()) != null) {
            throw new SocialAuthException("이미 가입된 소셜 계정입니다. 로그인해 주세요.");
        }
        if (socialAuthMapper.findMemberByEmail(pendingSignup.email()) != null) {
            throw new SocialAuthException("이미 가입된 이메일입니다. 기존 계정과 연동해 주세요.");
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
            // 이메일·닉네임·소셜 식별자 중복 경합을 SQL 정보 없이 안전한 메시지로 변환한다.
            throw new SocialAuthException(
                    "이미 가입된 이메일 또는 소셜 계정이거나 사용 중인 닉네임입니다.", e);
        }

        return toLoginMember(member);
    }

    /**
     * 로컬 비밀번호 인증이 끝난 후보 회원을 다시 잠가 연동 또는 복구를 완료한다.
     */
    @Transactional
    public LoginMemberDto linkSocialAccount(
            PendingSocialLink pendingLink,
            int authenticatedMemberId) {
        validatePendingLink(pendingLink);
        if (authenticatedMemberId != pendingLink.candidateMemberId()) {
            throw new SocialAuthException("연동할 회원 정보가 일치하지 않습니다.");
        }

        try {
            Member member = socialAuthMapper.findMemberByIdForUpdate(authenticatedMemberId);
            validateLinkCandidate(member, pendingLink);

            MemberSocialAuth memberSocial = socialAuthMapper.findSocialAuthByMemberIdForUpdate(
                    authenticatedMemberId);
            Member identityMember = socialAuthMapper.findMemberBySocialIdentity(
                    pendingLink.provider(), pendingLink.providerUserId());

            switch (pendingLink.mode()) {
                case PendingSocialLink.LINK -> {
                    requireActiveLinkCandidate(member);
                    requireNoExistingSocial(memberSocial, identityMember);
                    insertLinkedSocial(pendingLink, authenticatedMemberId);
                    requireOneRow(socialAuthMapper.updateMemberLastLogin(authenticatedMemberId));
                }
                case PendingSocialLink.REACTIVATE_AND_LINK -> {
                    requireWithdrawnLinkCandidate(member);
                    requireNoExistingSocial(memberSocial, identityMember);
                    requireOneRow(socialAuthMapper.reactivateWithdrawnMember(authenticatedMemberId));
                    insertLinkedSocial(pendingLink, authenticatedMemberId);
                }
                case PendingSocialLink.REACTIVATE_EXISTING_LINK -> {
                    requireWithdrawnLinkCandidate(member);
                    if (memberSocial == null
                            || !sameIdentity(
                                    memberSocial,
                                    pendingLink.provider(),
                                    pendingLink.providerUserId())
                            || identityMember == null
                            || identityMember.getMemberId() != authenticatedMemberId) {
                        throw new SocialAuthException(
                                SocialAuthException.ALREADY_LINKED,
                                "소셜 계정 연동 상태가 변경되었습니다. 다시 확인해 주세요.");
                    }
                    requireOneRow(socialAuthMapper.reactivateWithdrawnMember(authenticatedMemberId));
                    requireOneRow(socialAuthMapper.updateSocialLastLogin(
                            pendingLink.provider(), pendingLink.providerUserId()));
                }
                default -> throw new SocialAuthException("유효하지 않은 소셜 연동 방식입니다.");
            }

            return toLoginMember(member);
        } catch (DuplicateKeyException e) {
            // 회원당 하나, 소셜 식별자당 하나라는 DB 제약 위반 시 전체 트랜잭션을 롤백한다.
            throw new SocialAuthException(
                    SocialAuthException.ALREADY_LINKED,
                    "이미 연동된 소셜 계정이 있습니다.");
        }
    }

    private PendingSocialLink createPendingLink(
            int memberId,
            String provider,
            String providerUserId,
            String email,
            String providerNickname,
            String profileImageUrl,
            String username,
            String mode) {
        return new PendingSocialLink(
                memberId,
                provider,
                providerUserId,
                email,
                limitOptional(providerNickname, 100),
                limitOptional(profileImageUrl, 500),
                maskUsername(username),
                UUID.randomUUID().toString(),
                mode,
                LocalDateTime.now().plusMinutes(LINK_VALID_MINUTES));
    }

    private String maskUsername(String username) {
        int visibleLength = username.length() <= 2 ? 1 : 2;
        return username.substring(0, visibleLength)
                + "*".repeat(username.length() - visibleLength);
    }

    private void validateLinkCandidate(Member member, PendingSocialLink pendingLink) {
        if (member == null
                || !sameNormalizedEmail(pendingLink.email(), member.getEmail())
                || socialAuthMapper.findLocalUsernameByMemberIdForUpdate(member.getMemberId()) == null) {
            throw linkUnavailable();
        }
        if (member.getMemberRole() == null || member.getMemberRole().isBlank()) {
            throw new SocialAuthException("회원 권한 정보를 확인할 수 없습니다.", false);
        }
    }

    private void requireActiveLinkCandidate(Member member) {
        if (!"ACTIVE".equals(member.getMemberStatus()) || member.getDeletedAt() != null) {
            throw linkUnavailable();
        }
    }

    private void requireWithdrawnLinkCandidate(Member member) {
        if (!"WITHDRAWN".equals(member.getMemberStatus()) || member.getDeletedAt() == null) {
            throw linkUnavailable();
        }
    }

    private void requireNoExistingSocial(
            MemberSocialAuth memberSocial,
            Member identityMember) {
        if (memberSocial != null || identityMember != null) {
            throw new SocialAuthException(
                    SocialAuthException.ALREADY_LINKED,
                    "이미 연동된 소셜 계정이 있습니다.");
        }
    }

    private void insertLinkedSocial(PendingSocialLink pendingLink, int memberId) {
        MemberSocialAuth socialAuth = createSocialAuth(pendingLink);
        socialAuth.setMemberId(memberId);
        requireOneRow(socialAuthMapper.insertSocialAuth(socialAuth));
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
        // 입력 형식과 관계없이 같은 전화번호를 하나의 숫자 형식으로 저장한다.
        member.setPhone(signupRequest.getPhone().replace("-", ""));
        member.setGender(toStoredGender(signupRequest.getGender()));
        member.setBirth(signupRequest.getBirth());
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

    private MemberSocialAuth createSocialAuth(PendingSocialLink pendingLink) {
        MemberSocialAuth socialAuth = new MemberSocialAuth();
        socialAuth.setProvider(pendingLink.provider());
        socialAuth.setProviderUserId(pendingLink.providerUserId());
        socialAuth.setProviderEmail(pendingLink.email());
        socialAuth.setProviderEmailVerifiedYn("Y");
        socialAuth.setProviderNickname(pendingLink.providerNickname());
        socialAuth.setProviderProfileImageUrl(pendingLink.profileImageUrl());
        return socialAuth;
    }

    private void validateActiveMember(Member member) {
        if (!"ACTIVE".equals(member.getMemberStatus()) || member.getDeletedAt() != null) {
            throw new SocialAuthException("현재 로그인할 수 없는 회원 계정입니다.");
        }
        if (member.getMemberRole() == null || member.getMemberRole().isBlank()) {
            throw new SocialAuthException("회원 권한 정보를 확인할 수 없습니다.", false);
        }
    }

    private LoginMemberDto toLoginMember(Member member) {
        return new LoginMemberDto(
                member.getMemberId(),
                member.getNickname(),
                member.getMemberType(),
                member.getMemberRole());
    }

    private boolean sameIdentity(
            MemberSocialAuth socialAuth,
            String provider,
            String providerUserId) {
        return provider.equals(socialAuth.getProvider())
                && providerUserId.equals(socialAuth.getProviderUserId());
    }

    private boolean sameNormalizedEmail(String normalizedEmail, String storedEmail) {
        if (normalizedEmail == null || storedEmail == null) {
            return false;
        }
        try {
            // 신규 OAuth 이메일뿐 아니라 정규화 규칙 도입 전 저장된 대소문자·양끝 공백도
            // 같은 규칙으로 비교해 기존 회원 연동을 잘못 거부하지 않는다.
            return normalizedEmail.equals(
                    emailVerificationService.normalizeAndValidateEmail(storedEmail));
        } catch (EmailVerificationException e) {
            // DB의 기존 이메일 형식 자체가 유효하지 않으면 소유권 연동 대상으로 인정하지 않는다.
            return false;
        }
    }

    private void requireOneRow(int rowCount) {
        if (rowCount != 1) {
            throw new SocialAuthException("소셜 연동 정보 갱신에 실패했습니다.", false);
        }
    }

    private SocialAuthException linkUnavailable() {
        return new SocialAuthException(
                SocialAuthException.LINK_UNAVAILABLE,
                "현재 해당 회원과 소셜 계정을 연동할 수 없습니다.");
    }

    private void validateProviderUserId(String provider, String providerUserId) {
        if ((!KAKAO.equals(provider) && !GOOGLE.equals(provider) && !NAVER.equals(provider))
                || providerUserId == null
                || providerUserId.isBlank()
                || providerUserId.length() > 255
                || (KAKAO.equals(provider) && !providerUserId.matches("^[0-9]+$"))
                || (NAVER.equals(provider) && providerUserId.length() > 64)
                || (provider + "_" + providerUserId).length() > 100) {
            throw new SocialAuthException("유효하지 않은 소셜 회원 식별 정보입니다.");
        }
    }

    private void validatePendingSignup(PendingSocialSignup pendingSignup) {
        if (pendingSignup == null || pendingSignup.isExpired()) {
            throw new SocialAuthException("소셜 인증 정보가 없거나 만료됐습니다. 다시 로그인해 주세요.");
        }
        validateProviderUserId(pendingSignup.provider(), pendingSignup.providerUserId());
        String normalizedEmail = normalizeVerifiedOAuthEmail(
                pendingSignup.email(), pendingSignup.emailVerified());
        if (!normalizedEmail.equals(pendingSignup.email())) {
            throw new SocialAuthException("소셜 로그인 이메일 정규화 정보가 일치하지 않습니다.");
        }
    }

    private void validatePendingLink(PendingSocialLink pendingLink) {
        if (pendingLink == null || pendingLink.isExpired()) {
            throw new SocialAuthException("소셜 연동 정보가 없거나 만료됐습니다. 다시 시도해 주세요.");
        }
        validateProviderUserId(pendingLink.provider(), pendingLink.providerUserId());
        String normalizedEmail = normalizeVerifiedOAuthEmail(pendingLink.email(), true);
        if (!normalizedEmail.equals(pendingLink.email())
                || pendingLink.candidateMemberId() <= 0
                || pendingLink.linkNonce() == null
                || pendingLink.linkNonce().isBlank()) {
            throw new SocialAuthException("유효하지 않은 소셜 연동 정보입니다.");
        }
    }

    // DTO 형식 검증과 별개로 가입 유스케이스의 개인정보 동의만 다시 보장한다.
    private void validateSignupBusinessRules(SocialSignupRequest signupRequest) {
        if (signupRequest == null || !signupRequest.isPrivacyAgreed()) {
            throw new SocialAuthException("소셜 회원가입 입력값을 다시 확인해 주세요.");
        }
    }

    private String toStoredGender(String gender) {
        return "NONE".equals(gender) ? null : gender;
    }

    private String limitOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
