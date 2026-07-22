/**
 * DOM 조회, API 호출, 비동기 상태 저장은 다루지 않고 전달받은 상태 스냅샷만 판정한다.
 * 전달받은 원본 값을 변경하거나 trim하지 않으며, 최종 가입 허용 여부는 서버 검증이 결정한다.
 */
(function exposeSignupValidation(global) {
	"use strict";

	// 검증 결과 생성
	function result(valid, message) {
		return {
			valid,
			message: valid ? "" : message
		};
	}

	function hasWhitespace(value) {
		return /\s/.test(value);
	}

	// 입력값 형식 검증
	function validateName(value) {
		const valid = typeof value === "string"
			&& value.length >= 2
			&& value.length <= 50
			&& !hasWhitespace(value);
		return result(valid, "이름은 공백 없이 2~50자로 입력해주세요.");
	}

	function validateLoginId(value) {
		const valid = typeof value === "string" && /^[A-Za-z0-9]{5,20}$/.test(value);
		return result(valid, "아이디는 영문 또는 숫자 5~20자로 입력해주세요.");
	}

	function validatePassword(value) {
		const valid = typeof value === "string"
			&& /^(?=.*[A-Za-z])(?=.*\d)[^\s]{8,20}$/.test(value);
		return result(valid, "비밀번호는 공백 없이 영문과 숫자를 포함한 8~20자로 입력해주세요.");
	}

	function validatePasswordConfirm(password, confirmation) {
		const valid = typeof confirmation === "string"
			&& confirmation.length > 0
			&& password === confirmation;
		return result(valid, "비밀번호가 일치하지 않습니다.");
	}

	function validateEmail(value) {
		const valid = typeof value === "string"
			&& value.length <= 100
			&& !hasWhitespace(value)
			&& /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
		return result(valid, "올바른 이메일 주소를 입력해주세요.");
	}

	function validateVerificationCode(value) {
		const valid = typeof value === "string" && /^\d{6}$/.test(value);
		return result(valid, "인증번호 6자리를 입력해주세요.");
	}

	function validateNickname(value) {
		const valid = typeof value === "string"
			&& value.length >= 2
			&& value.length <= 10
			&& !hasWhitespace(value);
		return result(valid, "닉네임은 공백 없이 2~10자로 입력해주세요.");
	}

	function toLocalDateString(date) {
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, "0");
		const day = String(date.getDate()).padStart(2, "0");
		return `${year}-${month}-${day}`;
	}

	function validateBirth(value, today = toLocalDateString(new Date())) {
		const valid = typeof value === "string"
			&& /^\d{4}-\d{2}-\d{2}$/.test(value)
			&& value <= today;
		return result(valid, "생년월일을 확인해주세요.");
	}

	function validatePhone(value) {
		const valid = typeof value === "string"
			&& !hasWhitespace(value)
			&& /^01[016789]-?\d{3,4}-?\d{4}$/.test(value);
		return result(valid, "휴대전화 번호를 확인해주세요.");
	}

	function validatePrivacyAgreement(agreed) {
		return result(agreed === true, "개인정보 수집 및 이용에 동의해주세요.");
	}

	function validateLoginIdForSignup(value, checkedLoginId) {
		const formatValidation = validateLoginId(value);
		if (!formatValidation.valid) {
			return formatValidation;
		}
		return result(checkedLoginId === value, "아이디 중복 확인이 필요합니다.");
	}

	function validateNicknameForSignup(value, checkedNickname) {
		const formatValidation = validateNickname(value);
		if (!formatValidation.valid) {
			return formatValidation;
		}
		return result(checkedNickname === value, "닉네임 중복 확인이 필요합니다.");
	}

	function validateEmailVerification(email, workflowState) {
		// 인증 완료 당시의 원본 이메일과 현재 원본 이메일이 같아야 한다.
		const valid = workflowState.emailVerified === true
			&& workflowState.verifiedEmail === email;
		return result(valid, "이메일 인증을 완료해주세요.");
	}

	// 회원가입 제출 가능 여부 검증
	function validateSignup(values, workflowState, today = toLocalDateString(new Date())) {
		// 회원가입 제출 가능 여부는 이 함수 한 곳에서 결정한다.
		const input = values || {};
		const state = workflowState || {};
		const validations = {
			name: validateName(input.name),
			loginId: validateLoginIdForSignup(input.loginId, state.checkedLoginId),
			password: validatePassword(input.password),
			passwordConfirm: validatePasswordConfirm(input.password, input.passwordConfirm),
			email: validateEmail(input.email),
			emailVerification: validateEmailVerification(input.email, state),
			nickname: validateNicknameForSignup(input.nickname, state.checkedNickname),
			birth: validateBirth(input.birth, today),
			phone: validatePhone(input.phone),
			privacyAgreed: validatePrivacyAgreement(input.privacyAgreed)
		};
		// TODO: 사업자 승인 기능 추가 시 사업자등록증 상태를 values에 추가하고 여기서 함께 검증한다.
		const errors = {};
		let valid = true;

		// 첫 오류에서 멈추지 않고 모든 결과를 모아 기존처럼 전체 필드 오류를 한 번에 표시한다.
		Object.entries(validations).forEach(([field, validation]) => {
			errors[field] = validation.message;
			if (!validation.valid) {
				valid = false;
			}
		});

		return { valid, errors };
	}

	// 전역 함수 충돌을 피하기 위해 검증 기능은 하나의 읽기 전용 객체로만 공개한다.
	global.SignupValidation = Object.freeze({
		validateName,
		validateLoginId,
		validatePassword,
		validatePasswordConfirm,
		validateEmail,
		validateVerificationCode,
		validateNickname,
		toLocalDateString,
		validateBirth,
		validatePhone,
		validatePrivacyAgreement,
		validateSignup
	});
})(window);
