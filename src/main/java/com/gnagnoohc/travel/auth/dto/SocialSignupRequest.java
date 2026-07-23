package com.gnagnoohc.travel.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 신규 소셜 회원이 우리 서비스 가입 화면에서 직접 입력하는 값만 받는다.
 * 제공자 식별자와 이메일 인증 여부는 브라우저 요청을 믿지 않고 서버 세션 값을 사용한다.
 */
@Getter
@Setter
public class SocialSignupRequest {

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(min = 2, max = 20, message = "이름은 2~20자로 입력해 주세요.")
    @Pattern(regexp = "^[^\\s]+$", message = "이름에는 공백을 입력할 수 없습니다.")
    private String name;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Pattern(regexp = "^[^\\s]{2,10}$", message = "닉네임은 공백 없이 2~10자로 입력해 주세요.")
    private String nickname;

    @AssertTrue(message = "개인정보 수집 및 이용에 동의해 주세요.")
    private boolean privacyAgreed;
}
