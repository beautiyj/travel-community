package com.gnagnoohc.travel.auth.dto;

import java.sql.Date;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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

    // 소셜 회원도 로컬 회원과 같은 생년월일 정책을 적용한다.
    @NotNull(message = "생년월일을 입력해주세요.")
    @PastOrPresent(message = "생년월일은 오늘 또는 과거 날짜여야 합니다.")
    private Date birth;

    // 로컬 회원가입과 같은 휴대전화 형식으로 소셜 회원의 연락처를 받는다.
    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대전화 번호를 확인해주세요.")
    private String phone;

    // 브라우저에서는 남성, 여성, 선택 안 함 중 하나를 반드시 전송한다.
    @NotBlank(message = "성별을 선택해주세요.")
    @Pattern(regexp = "^(MALE|FEMALE|NONE)$", message = "성별 값이 올바르지 않습니다.")
    private String gender;

    @AssertTrue(message = "개인정보 수집 및 이용에 동의해 주세요.")
    private boolean privacyAgreed;
}
