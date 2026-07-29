package com.gnagnoohc.travel.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 기존 로컬 계정에 소셜 로그인을 연동할 때 사용자가 직접 입력하는 값만 받는다.
 * 제공자 식별자와 연동 대상 회원 ID는 브라우저가 아니라 서버 세션에서 확인한다.
 */
@Getter
@Setter
public class SocialLinkRequest {

    @NotBlank(message = "기존 아이디를 입력해주세요.")
    @Pattern(regexp = "^[A-Za-z0-9]{5,20}$", message = "아이디는 영문 또는 숫자 5~20자로 입력해주세요.")
    private String username;

    @NotBlank(message = "기존 비밀번호를 입력해주세요.")
    private String password;

    @AssertTrue(message = "소셜 로그인 연동에 동의해주세요.")
    private boolean linkAgreed;
}
