// 중복 확인을 통과한 당시의 값을 저장한다. 입력값이 바뀌면 다시 확인해야 한다.
const duplicateCheckState = {
	loginId: "",
	nickname: ""
};

// 이메일 인증은 입력값과 인증 완료 여부를 함께 저장해야 다른 이메일로 바꾼 뒤 재사용할 수 없다.
const emailVerificationState = {
	email: "",
	verified: false
};

// 화면의 재발송 카운트다운을 관리한다. 서버의 발송 제한을 대신하지 않는다.
let emailCooldownTimer = null;
// [수정] 배포 경로가 루트가 아니어도 회원가입 API URL을 현재 앱 경로로 만든다.
let appBasePath = "";

document.addEventListener("DOMContentLoaded", () => {
	// 회원가입 화면의 요소를 준비하고 각 입력 이벤트를 연결한다.
	const form = document.querySelector("#signupForm");
	const birth = document.querySelector("#birth");
	const loginId = document.querySelector("#login_id");
	const password = document.querySelector("#password");
	const passwordConfirm = document.querySelector("#passwordConfirm");
	const nickname = document.querySelector("#nickname");
	const email = document.querySelector("#email");
	const verificationCode = document.querySelector("#verificationCode");
	const verificationField = document.querySelector("#emailVerificationField");
	const sendEmailButton = document.querySelector("#sendEmailCodeButton");
	const verifyEmailButton = document.querySelector("#verifyEmailCodeButton");
	// [수정] form action에서 애플리케이션 컨텍스트 경로를 한 번만 계산한다.
	appBasePath = resolveAppBasePath(form.action);

	// [수정] 비밀번호 두 값이 변경될 때 회원가입 유효성 메시지를 즉시 갱신한다.
	const validatePasswordMatchOnChange = () => {
		// [수정] 비밀번호 관련 값을 수정하면 이전 submit 요약 메시지도 제거한다.
		clearMessage();
		clearFieldMessage("passwordError");
		clearFieldMessage("passwordConfirmError");
		if (passwordConfirm.value.length === 0) {
			return;
		}

		if (password.value === passwordConfirm.value) {
			showFieldMessage("passwordConfirmError", "비밀번호가 일치합니다.", false);
			return;
		}

		setError("passwordConfirmError", "비밀번호가 일치하지 않습니다.");
	};
	password.addEventListener("change", validatePasswordMatchOnChange);
	passwordConfirm.addEventListener("change", validatePasswordMatchOnChange);
	// [수정] 비밀번호와 확인값을 한 글자씩 입력할 때마다 일치 여부를 갱신한다.
	password.addEventListener("input", validatePasswordMatchOnChange);
	passwordConfirm.addEventListener("input", validatePasswordMatchOnChange);

	// [수정] submit 유효성 메시지는 사용자가 해당 입력값을 change할 때 즉시 제거한다.
	[
		["name", "nameError"],
		["login_id", "usernameError"],
		["email", "emailError"],
		// [수정] 인증번호를 수정하면 인증번호 입력 오류도 함께 숨긴다.
		["verificationCode", "verificationCodeError"],
		["nickname", "nicknameError"],
		["birth", "birthError"],
		["phone", "phoneError"],
		["privacyAgreed", "privacyAgreedError"]
	].forEach(([fieldId, messageId]) => {
		const field = document.querySelector(`#${fieldId}`);
		field.addEventListener("change", () => {
			// [수정] 변경한 입력의 필드 메시지와 약관 아래 submit 요약을 함께 숨긴다.
			clearFieldMessage(messageId);
			clearMessage();
		});
	});

	// 생년월일은 미래 날짜를 선택하지 못하게 한다.
	birth.max = new Date().toISOString().slice(0, 10);

	// 아이디가 바뀌면 이전 중복 확인 결과를 무효화한다.
	loginId.addEventListener("input", () => {
		// [수정] 이전 submit 알림이 입력 변경 후 남지 않도록 초기화한다.
		clearMessage();
		if (duplicateCheckState.loginId !== "") {
			duplicateCheckState.loginId = "";
			setError("usernameError", "아이디가 변경되어 중복 확인이 필요합니다.");
		}
	});

	// 닉네임이 바뀌면 이전 중복 확인 결과를 무효화한다.
	nickname.addEventListener("input", () => {
		// [수정] 이전 submit 알림이 입력 변경 후 남지 않도록 초기화한다.
		clearMessage();
		if (duplicateCheckState.nickname !== "") {
			duplicateCheckState.nickname = "";
			setError("nicknameError", "닉네임이 변경되어 중복 확인이 필요합니다.");
		}
	});

	// 이메일이 변경되면 이전 인증 결과와 인증번호 입력값을 폐기한다.
	email.addEventListener("input", () => {
		// [수정] 이메일 입력을 다시 시작하면 이전 submit 알림을 숨긴다.
		clearMessage();
		emailVerificationState.email = "";
		emailVerificationState.verified = false;
		verificationCode.value = "";
		verificationField.hidden = true;
		verifyEmailButton.disabled = false;
		clearEmailCooldown(sendEmailButton);
	});

	// 이메일 형식을 검사한 뒤 서버에 인증번호 발송을 요청한다.
	sendEmailButton.addEventListener("click", async () => {
		// [수정] 이메일 인증 결과는 약관 아래가 아닌 이메일 오류 태그에 표시한다.
		clearMessage();
		clearFieldMessage("emailError");
		if (!validateEmail() || sendEmailButton.disabled) {
			return;
		}

		sendEmailButton.disabled = true;
		try {
			const response = await fetch(appUrl("/auth/api/email-verification/send"), {
				method: "POST",
				headers: {
					"Content-Type": "application/x-www-form-urlencoded",
					Accept: "application/json"
				},
				body: new URLSearchParams({ email: email.value })
			});
			const result = await parseJsonResponse(response);

			if (!response.ok || !result.success) {
				showFieldMessage("emailError", result.message, true);
				clearEmailCooldown(sendEmailButton);
				return;
			}

			verificationField.hidden = false;
			// [수정] 인증번호 발송 성공 결과는 이메일 아래 초록색 안내로 표시한다.
			showFieldMessage("emailError", result.message, false);
			startEmailCooldown(sendEmailButton, 60);
			verificationCode.focus();
		} catch (error) {
			showRequestError(error, "emailError");
			clearEmailCooldown(sendEmailButton);
		}
	});

	// 6자리 형식을 확인한 뒤 서버에 인증번호 검증을 요청한다.
	verifyEmailButton.addEventListener("click", async () => {
		// [수정] 이메일 인증 결과는 이메일 입력 아래 emailError 태그에 표시한다.
		clearMessage();
		clearFieldMessage("emailError");
		clearFieldMessage("verificationCodeError");
		if (!validateEmail()) {
			return;
		}

		const code = verificationCode.value.trim();
		if (!/^\d{6}$/.test(code)) {
			setError("verificationCodeError", "인증번호 6자리를 입력해주세요.");
			return;
		}

		verifyEmailButton.disabled = true;
		try {
			const response = await fetch(appUrl("/auth/api/email-verification/verify"), {
				method: "POST",
				headers: {
					"Content-Type": "application/x-www-form-urlencoded",
					Accept: "application/json"
				},
				body: new URLSearchParams({ email: email.value, code })
			});
			const result = await parseJsonResponse(response);

			if (!response.ok || !result.success) {
				showFieldMessage("emailError", result.message, true);
				verifyEmailButton.disabled = false;
				return;
			}

			emailVerificationState.email = email.value;
			emailVerificationState.verified = true;
			verifyEmailButton.disabled = true;
			clearEmailCooldown(sendEmailButton);
			sendEmailButton.disabled = true;
			sendEmailButton.textContent = "인증 완료";
			// [수정] 이메일 인증 완료 결과는 이메일 아래 초록색 안내로 표시한다.
			showFieldMessage("emailError", result.message, false);
		} catch (error) {
			showRequestError(error, "emailError");
			verifyEmailButton.disabled = false;
		}
	});

	// 아이디 형식을 검사한 뒤 서버 중복 확인 API를 비동기로 호출한다.
	document.querySelector("#checkUsernameButton").addEventListener("click", async () => {
		// [수정] 아이디 중복 확인 결과는 usernameError 태그에 표시한다.
		clearMessage();
		clearFieldMessage("usernameError");
		if (!validateUsername()) {
			return;
		}

		const checkedValue = loginId.value;
		const available = await checkAvailability(
			appUrl(`/auth/api/check-login-id?loginId=${encodeURIComponent(checkedValue)}`),
			loginId,
			checkedValue,
			"usernameError"
		);
		duplicateCheckState.loginId = available ? checkedValue : "";
	});

	// 닉네임 형식을 검사한 뒤 서버 중복 확인 API를 비동기로 호출한다.
	document.querySelector("#checkNicknameButton").addEventListener("click", async () => {
		// [수정] 닉네임 중복 확인 결과는 nicknameError 태그에 표시한다.
		clearMessage();
		clearFieldMessage("nicknameError");
		if (!validateNickname()) {
			return;
		}

		const checkedValue = nickname.value;
		const available = await checkAvailability(
			appUrl(`/auth/api/check-nickname?nickname=${encodeURIComponent(checkedValue)}`),
			nickname,
			checkedValue,
			"nicknameError"
		);
		duplicateCheckState.nickname = available ? checkedValue : "";
	});

	// 브라우저 기본 제출을 막고, 전체 검증 후 회원가입 API를 호출한다.
	form.addEventListener("submit", async (event) => {
		// [수정] submit 결과와 submit 요약 오류만 약관 아래 공통 알림에 표시한다.
		clearMessage();
		const valid = validateSignupForm(true);
		event.preventDefault();

		if (!valid) {
			// [수정] 브라우저 alert 대신 약관 아래 submit 알림 영역에 유효성 요약을 표시한다.
			showMessage("입력항목을 확인해주세요.", true);
			return;
		}
		try {
			const response = await fetch(form.action, {
				method: "POST",
				body: new FormData(form),
				headers: {
					Accept: "application/json"
				}
			});

			const data = await parseJsonResponse(response);

			if (!response.ok || !data.success) {
				showMessage(data.message, true);
				return;
			}

			// [수정] 회원가입 성공 시 결과 페이지 컨트롤러를 직접 요청한다.
			window.location.assign(appUrl("/auth/signupresult"));

		} catch (error) {
			showRequestError(error);
		}
	});
});

async function checkAvailability(url, input, checkedValue, messageElementId) {
	// 중복 확인 공통 함수: 응답 도착 전에 입력값이 바뀌었는지도 확인한다.
	try {
		const response = await fetch(url, {
			method: "GET",
			headers: {
				Accept: "application/json"
			}
		});
		const result = await parseJsonResponse(response);

		if (!response.ok || !result.success) {
			// [수정] 중복 확인 실패도 호출한 입력 필드 아래에 표시한다.
			showFieldMessage(messageElementId, result.message || "중복 확인에 실패했습니다.", true);
			return false;
		}

		if (input.value !== checkedValue) {
			return false;
		}

		// [수정] 중복 여부에 따른 안내/오류 색상과 위치를 필드 메시지로 처리한다.
		// [수정] available=true(중복 없음)일 때 별도 field-success 태그를 초록색으로 사용한다.
		showFieldMessage(messageElementId, result.message, !result.available);

		if (!result.available) {
			input.value = "";
			input.focus();
		}

		return result.available;
	} catch (error) {
		showRequestError(error, messageElementId);
		return false;
	}
}

// [수정] form action 경로에서 현재 애플리케이션의 컨텍스트 경로를 추출한다.
function resolveAppBasePath(action) {
	const pathname = new URL(action, window.location.href).pathname;
	return pathname.replace(/\/auth\/membersignup\/?$/, "");
}

// [수정] 추출한 컨텍스트 경로를 API 상대 경로 앞에 붙인다.
function appUrl(path) {
	return `${appBasePath}${path}`;
}

// [수정] HTML/빈 응답을 JSON 응답으로 오인하지 않도록 응답 형식을 검증한다.
async function parseJsonResponse(response) {
	const contentType = response.headers.get("content-type") || "";
	if (!contentType.toLowerCase().includes("json")) {
		throw new ResponseFormatError(response.status);
	}

	try {
		const result = await response.json();
		if (!result || typeof result !== "object") {
			throw new ResponseFormatError(response.status);
		}
		return result;
	} catch (error) {
		if (error instanceof ResponseFormatError) {
			throw error;
		}
		throw new ResponseFormatError(response.status);
	}
}

// [수정] 네트워크/응답 형식 오류를 지정된 필드 또는 submit 공통 영역에 표시한다.
function showRequestError(error, fieldMessageId = null) {
	const message = error instanceof ResponseFormatError
		? error.message
		: "서버와 통신하지 못했습니다.";

	if (fieldMessageId !== null) {
		showFieldMessage(fieldMessageId, message, true);
		return;
	}

	showMessage(message, true);
}

// [수정] 중복 확인/이메일 인증처럼 필드에 종속된 결과를 해당 태그에 표시한다.
function showFieldMessage(elementId, message, isError) {
	const messageElement = document.querySelector(`#${elementId}`);
	const hasMessage = typeof message === "string" && message.trim() !== "";
	// [수정] field-error는 오류 전용으로 유지하고 성공 결과는 별도 태그에 표시한다.
	const successElementId = getSuccessElementId(elementId);
	const successElement = successElementId === ""
		? null
		: document.querySelector(`#${successElementId}`);
	const displayMessage = hasMessage ? message : "요청 처리 중 오류가 발생했습니다.";
	messageElement.textContent = !isError && hasMessage ? "" : displayMessage;
	if (successElement) {
		successElement.textContent = !isError && hasMessage ? message : "";
		// [수정] 성공 안내 태그 자체에도 초록색을 지정해 이전 CSS 캐시의 영향을 받지 않게 한다.
		successElement.style.color = !isError && hasMessage ? "#087f5b" : "";
	}
}

// [수정] 새 요청 전에 필드별 이전 안내 메시지를 초기화한다.
function clearFieldMessage(elementId) {
	const messageElement = document.querySelector(`#${elementId}`);
	messageElement.textContent = "";
	const successElementId = getSuccessElementId(elementId);
	const successElement = successElementId === ""
		? null
		: document.querySelector(`#${successElementId}`);
	if (successElement) {
		successElement.textContent = "";
		successElement.style.color = "";
	}
}

// [수정] 오류 태그와 대응하는 성공 안내 태그를 연결한다.
function getSuccessElementId(errorElementId) {
	const successElementIds = {
		emailError: "emailSuccess",
		usernameError: "usernameSuccess",
		nicknameError: "nicknameSuccess",
		passwordConfirmError: "passwordConfirmSuccess"
	};
	return successElementIds[errorElementId] || "";
}

// [수정] 응답 형식 오류를 네트워크 오류와 구분하기 위한 전용 오류다.
class ResponseFormatError extends Error {
	constructor(status) {
		super(`서버 응답을 처리하지 못했습니다. (HTTP ${status})`);
		this.name = "ResponseFormatError";
	}
}

function validateSignupForm(showErrors) {
	// 각 필드 검증 결과가 모두 true일 때만 회원가입 요청을 허용한다.
	return [
		validateName(showErrors),
		validateUsername(showErrors),
		validateLoginIdDuplicateCheck(showErrors),
		validatePassword(showErrors),
		validatePasswordConfirm(showErrors),
		validateEmail(showErrors),
		validateEmailVerification(showErrors),
		validateNickname(showErrors),
		validateNicknameDuplicateCheck(showErrors),
		validateBirth(showErrors),
		validatePhone(showErrors),
		// TODO: 사업자 승인 기능 추가 시 사업자등록증 파일 검증 활성화
		// validateBusinessFile(showErrors),
		validatePrivacyAgreement(showErrors)
	].every(Boolean);
}

function validateName(showError = true) {
	// 이름의 길이와 공백 포함 여부를 검사한다.
	const value = document.querySelector("#name").value;
	const valid = value.length >= 2 && value.length <= 50 && !hasWhitespace(value);
	if (showError) {
		setError("nameError", valid ? "" : "이름은 공백 없이 2~50자로 입력해주세요.");
	}
	return valid;
}

function validateUsername(showError = true) {
	// 아이디는 영문/숫자 조합 5~20자만 허용한다.
	const value = document.querySelector("#login_id").value;
	const valid = /^[A-Za-z0-9]{5,20}$/.test(value);
	if (showError) {
		setError("usernameError", valid ? "" : "아이디는 영문 또는 숫자 5~20자로 입력해주세요.");
	}
	return valid;
}

function validateLoginIdDuplicateCheck(showError = true) {
	// 현재 입력값이 중복 확인을 통과한 값과 같은지 검사한다.
	const value = document.querySelector("#login_id").value;
	const formatValid = /^[A-Za-z0-9]{5,20}$/.test(value);
	const valid = formatValid && duplicateCheckState.loginId === value;

	if (showError && formatValid && !valid) {
		setError("usernameError", "아이디 중복 확인이 필요합니다.");
	}
	return valid;
}

function validatePassword(showError = true) {
	// 비밀번호는 영문과 숫자를 포함한 8~64자로 검사한다.
	const value = document.querySelector("#password").value;
	const valid = /^(?=.*[A-Za-z])(?=.*\d)[^\s]{8,64}$/.test(value);
	if (showError) {
		setError("passwordError", valid ? "" : "비밀번호는 공백 없이 영문과 숫자를 포함한 8~64자로 입력해주세요.");
	}
	return valid;
}

function validatePasswordConfirm(showError = true) {
	// 비밀번호와 확인값이 일치하는지 검사한다.
	const password = document.querySelector("#password").value;
	const confirmation = document.querySelector("#passwordConfirm").value;
	const valid = confirmation.length > 0 && password === confirmation;
	if (showError) {
		setError("passwordConfirmError", valid ? "" : "비밀번호가 일치하지 않습니다.");
	}
	return valid;
}

function validateEmail(showError = true) {
	// 클라이언트 형식 검사일 뿐, 실제 이메일 소유자 인증은 서버에서 처리한다.
	const value = document.querySelector("#email").value;
	const valid = !hasWhitespace(value) && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
	if (showError) {
		setError("emailError", valid ? "" : "올바른 이메일 주소를 입력해주세요.");
	}
	return valid;
}

// 인증 완료 이메일과 현재 입력 이메일이 같은지 확인한다.
function validateEmailVerification(showError = true) {
	const email = document.querySelector("#email").value;
	const valid = emailVerificationState.verified
		&& emailVerificationState.email === email;
	if (showError) {
		setError(
			"verificationCodeError",
			valid ? "" : "이메일 인증을 완료해주세요."
		);
	}
	return valid;
}

function validateNickname(showError = true) {
	// 닉네임의 길이와 공백 포함 여부를 검사한다.
	const value = document.querySelector("#nickname").value;
	const valid = value.length >= 2 && value.length <= 20 && !hasWhitespace(value);
	if (showError) {
		setError("nicknameError", valid ? "" : "닉네임은 공백 없이 2~20자로 입력해주세요.");
	}
	return valid;
}

function validateNicknameDuplicateCheck(showError = true) {
	// 현재 닉네임이 중복 확인을 통과한 값과 같은지 검사한다.
	const value = document.querySelector("#nickname").value;
	const formatValid = value.length >= 2 && value.length <= 20 && !hasWhitespace(value);
	const valid = formatValid && duplicateCheckState.nickname === value;

	if (showError && formatValid && !valid) {
		setError("nicknameError", "닉네임 중복 확인이 필요합니다.");
	}
	return valid;
}

function validateBirth(showError = true) {
	// 생년월일 입력 여부와 미래 날짜 여부를 검사한다.
	const value = document.querySelector("#birth").value;
	const valid = value.length > 0 && new Date(value) <= new Date();
	if (showError) {
		setError("birthError", valid ? "" : "생년월일을 확인해주세요.");
	}
	return valid;
}

function validatePhone(showError = true) {
	// 국내 휴대전화 번호 형식을 검사한다.
	const value = document.querySelector("#phone").value;
	const valid = !hasWhitespace(value) && /^01[016789]-?\d{3,4}-?\d{4}$/.test(value);
	if (showError) {
		setError("phoneError", valid ? "" : "휴대전화 번호를 확인해주세요.");
	}
	return valid;
}

function hasWhitespace(value) {
	// 공백 문자가 포함되어 있는지 공통으로 검사한다.
	return /\s/.test(value);
}

/* TODO: 사업자 승인 기능 추가 시 사업자등록증 파일 검증 구현 예정
function validateBusinessFile() {
	const form = document.querySelector("#signupForm");
	const fileInput = document.querySelector("#businessRegistrationFile");
	const valid = form.dataset.memberType !== "2" || (fileInput && fileInput.files.length > 0);

	if (form.dataset.memberType === "2") {
		setError("businessRegistrationFileError", valid ? "" : "사업자등록증을 첨부해주세요.");
	}

	return valid;
}
*/

function validatePrivacyAgreement(showError = true) {
	// 필수 개인정보 수집 동의 여부를 검사한다.
	const valid = document.querySelector("#privacyAgreed").checked;
	if (showError) {
		setError("privacyAgreedError", valid ? "" : "개인정보 수집 및 이용에 동의해주세요.");
	}
	return valid;
}

function setError(elementId, message) {
	// 각 필드 아래의 오류 메시지를 갱신한다.
	document.querySelector(`#${elementId}`).textContent = message;
	// [수정] 오류가 갱신되면 같은 필드의 이전 성공 안내를 제거한다.
	const successElementId = getSuccessElementId(elementId);
	if (successElementId !== "") {
		const successElement = document.querySelector(`#${successElementId}`);
		successElement.textContent = "";
		successElement.style.color = "";
	}
}

// 발송 성공 후 60초 동안 버튼을 잠가 중복 클릭을 줄인다.
function startEmailCooldown(button, seconds) {
	clearEmailCooldown(button);
	let remainingSeconds = seconds;
	button.disabled = true;
	button.textContent = `재발송 (${remainingSeconds})`;

	emailCooldownTimer = window.setInterval(() => {
		remainingSeconds -= 1;
		if (remainingSeconds <= 0) {
			clearEmailCooldown(button);
			return;
		}
		button.textContent = `재발송 (${remainingSeconds})`;
	}, 1000);
}

// 이메일 변경 또는 발송 실패 시 화면 카운트다운을 정리한다.
function clearEmailCooldown(button) {
	if (emailCooldownTimer !== null) {
		window.clearInterval(emailCooldownTimer);
		emailCooldownTimer = null;
	}
	button.disabled = false;
	button.textContent = "인증번호 발송";
}

function clearMessage() {
	// [수정] 약관 아래 공통 알림은 submit 시작 시 이전 내용을 제거한다.
	const messageBox = document.querySelector("#signupMessage");
	messageBox.hidden = true;
	messageBox.textContent = "";
	messageBox.className = "form-alert";
}

function showMessage(message, isError) {
	// [수정] submit 예외/유효성 요약만 약관 아래 공통 알림 영역에 표시한다.
	const messageBox = document.querySelector("#signupMessage");
	const hasMessage = typeof message === "string" && message.trim() !== "";
	messageBox.hidden = false;
	messageBox.textContent = hasMessage ? message : "요청 처리 중 오류가 발생했습니다.";
	messageBox.className = `form-alert ${isError || !hasMessage ? "form-alert--error" : "form-alert--notice"}`;
}
