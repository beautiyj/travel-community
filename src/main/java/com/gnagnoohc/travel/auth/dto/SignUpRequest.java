package com.gnagnoohc.travel.auth.dto;

import java.sql.Date;

import org.apache.ibatis.type.Alias;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 화면에서 전달되는 입력만 표현한다.
 */
@Getter
@Setter
@Alias("signuprequest")
public class SignUpRequest {

	// 회원 유형
	// 화면 값을 조작해도 일반 회원(1)과 사업자 회원(2)만 가입할 수 있다.
	@Min(value = 1, message = "회원 유형이 올바르지 않습니다.")
	@Max(value = 2, message = "회원 유형이 올바르지 않습니다.")
	private int memberType;

	// 기본 회원 정보
	// 회원가입 입력 규칙을 서버에서도 동일하게 검증한다.
	@NotBlank(message = "이름을 입력해주세요.")
	// DB member.name VARCHAR(20)과 동일한 최대 길이를 적용한다.
	@Size(min = 2, max = 20, message = "이름은 2~20자로 입력해주세요.")
	@Pattern(regexp = "^[^\\s]+$", message = "이름에는 공백을 입력할 수 없습니다.")
	private String name;

	// 로그인 정보
	@NotBlank(message = "아이디를 입력해주세요.")
	@Pattern(regexp = "^[A-Za-z0-9]{5,20}$", message = "아이디는 영문 또는 숫자 5~20자로 입력해주세요.")
	private String loginId;

	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$", message = "비밀번호는 공백 없이 영문과 숫자를 포함한 8~20자로 입력해주세요.")
	private String password;

	@NotBlank(message = "비밀번호 확인을 입력해주세요.")
	@Size(min = 8, max = 20, message = "비밀번호 확인은 8~20자로 입력해주세요.")
	private String passwordConfirm;

	// 이메일 및 연락처 정보
	@NotBlank(message = "이메일을 입력해주세요.")
	@Email(message = "올바른 이메일 주소를 입력해주세요.")
	@Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
	private String email;

	@NotBlank(message = "닉네임을 입력해주세요.")
	@Pattern(regexp = "^[^\\s]{2,10}$", message = "닉네임은 공백 없이 2~10자로 입력해주세요.")
	private String nickname;

	@NotNull(message = "생년월일을 입력해주세요.")
	@PastOrPresent(message = "생년월일은 오늘 또는 과거 날짜여야 합니다.")
	private Date birth;

	@NotBlank(message = "전화번호를 입력해주세요.")
	@Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대전화 번호를 확인해주세요.")
	private String phone;

	@Pattern(regexp = "^(MALE|FEMALE)$", message = "성별 값이 올바르지 않습니다.")
	private String gender;

	// 약관 동의 및 항목 간 검증
	@AssertTrue(message = "개인정보 수집 및 이용에 동의해주세요.")
	private boolean privacyAgreed;

	@AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
	public boolean isPasswordMatched() {
		return password != null && password.equals(passwordConfirm);
	}
}
