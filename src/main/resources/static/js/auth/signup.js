// 검증 규칙과 제출 가능 여부 판단은 signup-validation.js가 담당한다.
const signupValidation = window.SignupValidation;

// 검증 파일의 의미 기반 오류 키를 현재 JSP의 메시지 태그와 연결한다.
const signupErrorElementIds = Object.freeze({
	name: "nameError",
	loginId: "usernameError",
	password: "passwordError",
	passwordConfirm: "passwordConfirmError",
	email: "emailError",
	emailVerification: "verificationCodeError",
	nickname: "nicknameError",
	birth: "birthError",
	phone: "phoneError",
	privacyAgreed: "privacyAgreedError"
});

// 오류 태그와 대응하는 성공 안내 태그를 연결한다.
const signupSuccessElementIds = Object.freeze({
	emailError: "emailSuccess",
	usernameError: "usernameSuccess",
	nicknameError: "nicknameSuccess",
	passwordConfirmError: "passwordConfirmSuccess"
});

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
// 배포 경로가 루트가 아니어도 회원가입 API URL을 현재 앱 경로로 만든다.
let appBasePath = "";

// 화면 초기화 및 이벤트 연결
document.addEventListener("DOMContentLoaded", () => {
	const elements = getSignupElements();
	// form action에서 애플리케이션 컨텍스트 경로를 한 번만 계산한다.
	appBasePath = resolveAppBasePath(elements.form.action);
	// UTC가 아닌 사용자 브라우저의 로컬 날짜를 기준으로 미래 날짜 선택을 막는다.
	elements.birth.max = signupValidation.toLocalDateString(new Date());

	bindPasswordMatchEvents(elements);
	bindFieldMessageClearEvents();
	bindWorkflowStateInvalidationEvents(elements);
	bindEmailVerificationEvents(elements);
	bindDuplicateCheckEvents(elements);
	bindSignupSubmitEvent(elements);
});

// 회원가입 화면 요소 수집
function getSignupElements() {
	const form = document.querySelector("#signupForm");
	const signupSubmitButton = form.querySelector("button[type='submit']");
	return {
		form,
		birth: document.querySelector("#birth"),
		loginId: document.querySelector("#login_id"),
		password: document.querySelector("#password"),
		passwordConfirm: document.querySelector("#passwordConfirm"),
		nickname: document.querySelector("#nickname"),
		email: document.querySelector("#email"),
		verificationCode: document.querySelector("#verificationCode"),
		verificationField: document.querySelector("#emailVerificationField"),
		sendEmailButton: document.querySelector("#sendEmailCodeButton"),
		verifyEmailButton: document.querySelector("#verifyEmailCodeButton"),
		checkUsernameButton: document.querySelector("#checkUsernameButton"),
		checkNicknameButton: document.querySelector("#checkNicknameButton"),
		signupSubmitButton,
		signupSubmitButtonText: signupSubmitButton.textContent
	};
}

// 비밀번호 일치 안내 이벤트 연결
function bindPasswordMatchEvents({ password, passwordConfirm }) {
	const updatePasswordMatchMessage = () => {
		// 비밀번호 관련 값을 수정하면 이전 제출 요약 메시지도 제거한다.
		clearMessage();
		clearFieldMessage("passwordError");
		clearFieldMessage("passwordConfirmError");
		if (passwordConfirm.value.length === 0) {
			return;
		}

		// 실시간 일치 여부도 같은 검증 규칙을 사용해 제출 검증과 차이가 없게 한다.
		const matchValidation = signupValidation.validatePasswordConfirm(
			password.value,
			passwordConfirm.value
		);
		if (matchValidation.valid) {
			showFieldMessage("passwordConfirmError", "비밀번호가 일치합니다.", false);
			return;
		}

		setError("passwordConfirmError", matchValidation.message);
	};

	password.addEventListener("change", updatePasswordMatchMessage);
	passwordConfirm.addEventListener("change", updatePasswordMatchMessage);
	// 비밀번호와 확인값을 한 글자씩 입력할 때마다 일치 여부를 갱신한다.
	password.addEventListener("input", updatePasswordMatchMessage);
	passwordConfirm.addEventListener("input", updatePasswordMatchMessage);
}

// 입력 변경 시 이전 오류 메시지 초기화 이벤트 연결
function bindFieldMessageClearEvents() {
	// 제출 유효성 메시지는 사용자가 해당 입력값을 변경할 때 즉시 제거한다.
	[
		["name", "nameError"],
		["login_id", "usernameError"],
		["email", "emailError"],
		// 인증번호를 수정하면 인증번호 입력 오류도 함께 숨긴다.
		["verificationCode", "verificationCodeError"],
		["nickname", "nicknameError"],
		["birth", "birthError"],
		["phone", "phoneError"],
		["privacyAgreed", "privacyAgreedError"]
	].forEach(([fieldId, messageId]) => {
		const field = document.querySelector(`#${fieldId}`);
		field.addEventListener("change", () => {
			// 변경한 입력의 필드 메시지와 약관 아래 제출 요약을 함께 숨긴다.
			clearFieldMessage(messageId);
			clearMessage();
		});
	});
}

// 중복 확인과 이메일 인증 상태 무효화 이벤트 연결
function bindWorkflowStateInvalidationEvents(elements) {
	const { loginId, nickname, email } = elements;
	// 아이디가 바뀌면 이전 중복 확인 결과를 무효화한다.
	loginId.addEventListener("input", () => {
		// 이전 제출 알림이 입력 변경 후 남지 않도록 초기화한다.
		clearMessage();
		if (duplicateCheckState.loginId !== "") {
			duplicateCheckState.loginId = "";
			setError("usernameError", "아이디가 변경되어 중복 확인이 필요합니다.");
		}
	});

	// 닉네임이 바뀌면 이전 중복 확인 결과를 무효화한다.
	nickname.addEventListener("input", () => {
		// 이전 제출 알림이 입력 변경 후 남지 않도록 초기화한다.
		clearMessage();
		if (duplicateCheckState.nickname !== "") {
			duplicateCheckState.nickname = "";
			setError("nicknameError", "닉네임이 변경되어 중복 확인이 필요합니다.");
		}
	});

	// 이메일이 변경되면 이전 인증 결과와 인증번호 입력값을 폐기한다.
	email.addEventListener("input", () => {
		// 이메일 입력을 다시 시작하면 이전 제출 알림을 숨긴다.
		clearMessage();
		resetEmailVerification(elements);
	});
}

// 이메일 인증 진행 상태 초기화
function resetEmailVerification({ verificationCode, verificationField, verifyEmailButton, sendEmailButton }) {
	// 인증 만료 또는 이메일 변경 시 인증 상태를 폐기하고 재발송 가능한 화면으로 되돌린다.
	emailVerificationState.email = "";
	emailVerificationState.verified = false;
	verificationCode.value = "";
	verificationField.hidden = true;
	verifyEmailButton.disabled = false;
	clearEmailCooldown(sendEmailButton);
}

// 이메일 인증번호 발송 및 검증 이벤트 연결
function bindEmailVerificationEvents(elements) {
	const {
		email,
		verificationCode,
		verificationField,
		sendEmailButton,
		verifyEmailButton
	} = elements;

	// 이메일 형식을 검사한 뒤 서버에 인증번호 발송을 요청한다.
	sendEmailButton.addEventListener("click", async () => {
		// 이메일 인증 결과는 약관 아래가 아닌 이메일 오류 태그에 표시한다.
		clearMessage();
		clearFieldMessage("emailError");
		const emailValidation = signupValidation.validateEmail(email.value);
		setError("emailError", emailValidation.message);
		if (!emailValidation.valid || sendEmailButton.disabled) {
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
			// 인증번호 발송 성공 결과는 이메일 아래 초록색 안내로 표시한다.
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
		// 이메일 인증 결과는 이메일 입력 아래 emailError 태그에 표시한다.
		clearMessage();
		clearFieldMessage("emailError");
		clearFieldMessage("verificationCodeError");
		const emailValidation = signupValidation.validateEmail(email.value);
		setError("emailError", emailValidation.message);
		if (!emailValidation.valid) {
			return;
		}

		const code = verificationCode.value.trim();
		const codeValidation = signupValidation.validateVerificationCode(code);
		if (!codeValidation.valid) {
			setError("verificationCodeError", codeValidation.message);
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
			// 이메일 인증 완료 결과는 이메일 아래 초록색 안내로 표시한다.
			showFieldMessage("emailError", result.message, false);
		} catch (error) {
			showRequestError(error, "emailError");
			verifyEmailButton.disabled = false;
		}
	});
}

// 아이디와 닉네임 중복 확인 이벤트 연결
function bindDuplicateCheckEvents({
	loginId,
	nickname,
	checkUsernameButton,
	checkNicknameButton
}) {
	// 아이디 형식을 검사한 뒤 서버 중복 확인 API를 비동기로 호출한다.
	checkUsernameButton.addEventListener("click", async () => {
		// 아이디 중복 확인 결과는 usernameError 태그에 표시한다.
		clearMessage();
		clearFieldMessage("usernameError");
		const loginIdValidation = signupValidation.validateLoginId(loginId.value);
		setError("usernameError", loginIdValidation.message);
		if (!loginIdValidation.valid) {
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
	checkNicknameButton.addEventListener("click", async () => {
		// 닉네임 중복 확인 결과는 nicknameError 태그에 표시한다.
		clearMessage();
		clearFieldMessage("nicknameError");
		const nicknameValidation = signupValidation.validateNickname(nickname.value);
		setError("nicknameError", nicknameValidation.message);
		if (!nicknameValidation.valid) {
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
}

// 회원가입 제출 이벤트 연결
function bindSignupSubmitEvent(elements) {
	const {
		form,
		birth,
		email,
		signupSubmitButton,
		signupSubmitButtonText
	} = elements;
	// 가입 요청이 완료되기 전까지 같은 form의 재전송을 막는다.
	let signupSubmitting = false;

	// 브라우저 기본 제출을 막고, 전체 검증 후 회원가입 API를 호출한다.
	form.addEventListener("submit", async (event) => {
		event.preventDefault();
		if (signupSubmitting) {
			return;
		}

		// 제출 결과와 제출 요약 오류만 약관 아래 공통 알림에 표시한다.
		clearMessage();
		// 제출 시점의 날짜를 사용한다.
		const localToday = signupValidation.toLocalDateString(new Date());
		birth.max = localToday;
		// 화면 값과 비동기 확인 상태의 스냅샷만 검증 모듈에 전달한다.
		const validation = signupValidation.validateSignup(
			readSignupValues(form),
			readSignupWorkflowState(),
			localToday
		);
		renderSignupErrors(validation.errors);

		if (!validation.valid) {
			// 브라우저 alert 대신 약관 아래 제출 알림 영역에 유효성 요약을 표시한다.
			showMessage("입력항목을 확인해주세요.", true);
			return;
		}

		// 네트워크 요청 중에는 버튼과 상태값을 함께 잠가 중복 가입을 차단한다.
		signupSubmitting = true;
		signupSubmitButton.disabled = true;
		signupSubmitButton.textContent = "가입 처리 중...";
		// 성공 응답 뒤 결과 페이지 이동이 시작될 때까지 잠금 상태를 유지한다.
		let signupSucceeded = false;
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
				// 인증 만료는 같은 이메일로 재발송할 수 있게 상태를 초기화하고 이메일 입력란으로 안내한다.
				// 세션 없음·만료·이미 사용됨은 같은 재인증 흐름으로 초기화한다.
				if (data.code === "EMAIL_REVERIFICATION_REQUIRED") {
					resetEmailVerification(elements);
					clearFieldMessage("emailError");
					clearFieldMessage("verificationCodeError");
					showFieldMessage("emailError", data.message, true);
					email.focus();
					return;
				}
				showMessage(data.message, true);
				return;
			}

			// 회원가입 성공 시 결과 페이지 컨트롤러를 직접 요청한다.
			window.location.assign(appUrl("/auth/signupresult"));
			signupSucceeded = true;

		} catch (error) {
			showRequestError(error);
		} finally {
			// 성공 후 페이지가 이동하는 동안에는 버튼을 계속 잠가 재클릭을 막는다.
			if (signupSucceeded) {
				return;
			}
			// 실패 응답이나 통신 오류 후에는 다시 제출할 수 있도록 원래 상태로 복구한다.
			signupSubmitting = false;
			signupSubmitButton.disabled = false;
			signupSubmitButton.textContent = signupSubmitButtonText;
		}
	});
}

// 중복 확인 서버 통신
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
			// 중복 확인 실패도 호출한 입력 필드 아래에 표시한다.
			showFieldMessage(messageElementId, result.message || "중복 확인에 실패했습니다.", true);
			return false;
		}

		if (input.value !== checkedValue) {
			return false;
		}

		// 중복 여부에 따른 안내와 오류를 필드 메시지로 처리한다.
		// available=true(중복 없음)일 때 별도 field-success 태그를 초록색으로 사용한다.
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

// URL 및 응답 처리
// form action 경로에서 현재 애플리케이션의 컨텍스트 경로를 추출한다.
function resolveAppBasePath(action) {
	const pathname = new URL(action, window.location.href).pathname;
	return pathname.replace(/\/auth\/membersignup\/?$/, "");
}

// 추출한 컨텍스트 경로를 API 상대 경로 앞에 붙인다.
function appUrl(path) {
	return `${appBasePath}${path}`;
}

// HTML 또는 빈 응답을 JSON 응답으로 오인하지 않도록 응답 형식을 검증한다.
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

// 화면 메시지 처리
// 네트워크 또는 응답 형식 오류를 지정된 필드 또는 제출 공통 영역에 표시한다.
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

// 중복 확인이나 이메일 인증처럼 필드에 종속된 결과를 해당 태그에 표시한다.
function showFieldMessage(elementId, message, isError) {
	const messageElement = document.querySelector(`#${elementId}`);
	const hasMessage = typeof message === "string" && message.trim() !== "";
	// field-error는 오류 전용으로 유지하고 성공 결과는 별도 태그에 표시한다.
	const successElementId = getSuccessElementId(elementId);
	const successElement = successElementId === ""
		? null
		: document.querySelector(`#${successElementId}`);
	const displayMessage = hasMessage ? message : "요청 처리 중 오류가 발생했습니다.";
	messageElement.textContent = !isError && hasMessage ? "" : displayMessage;
	if (successElement) {
		successElement.textContent = !isError && hasMessage ? message : "";
		// 성공 안내 태그 자체에도 초록색을 지정해 이전 CSS 캐시의 영향을 받지 않게 한다.
		successElement.style.color = !isError && hasMessage ? "#087f5b" : "";
	}
}

// 새 요청 전에 필드별 이전 안내 메시지를 초기화한다.
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

function getSuccessElementId(errorElementId) {
	return signupSuccessElementIds[errorElementId] || "";
}

// 응답 형식 오류를 네트워크 오류와 구분하기 위한 전용 오류다.
class ResponseFormatError extends Error {
	constructor(status) {
		super(`서버 응답을 처리하지 못했습니다. (HTTP ${status})`);
		this.name = "ResponseFormatError";
	}
}

// 회원가입 검증 데이터 구성
function readSignupValues(form) {
	// trim이나 정규화 없이 DTO로 전송될 원본 입력값을 복사한다.
	return {
		name: form.elements.name.value,
		loginId: form.elements.loginId.value,
		password: form.elements.password.value,
		passwordConfirm: form.elements.passwordConfirm.value,
		email: form.elements.email.value,
		nickname: form.elements.nickname.value,
		birth: form.elements.birth.value,
		phone: form.elements.phone.value,
		privacyAgreed: form.elements.privacyAgreed.checked
	};
}

function readSignupWorkflowState() {
	// 비동기 작업이 관리하는 상태 자체는 UI 파일에 두고, 검증 시점의 값만 전달한다.
	return {
		checkedLoginId: duplicateCheckState.loginId,
		checkedNickname: duplicateCheckState.nickname,
		verifiedEmail: emailVerificationState.email,
		emailVerified: emailVerificationState.verified
	};
}

function renderSignupErrors(errors) {
	// 모든 키를 순회해 기존처럼 한 번의 제출에서 전체 필드 오류를 함께 갱신한다.
	Object.entries(signupErrorElementIds).forEach(([field, elementId]) => {
		setError(elementId, errors[field] || "");
	});
}

function setError(elementId, message) {
	// 각 필드 아래의 오류 메시지를 갱신한다.
	// 버튼별 사전검증도 이 함수를 사용해 같은 위치에 오류를 표시한다.
	document.querySelector(`#${elementId}`).textContent = message;
	// 오류가 갱신되면 같은 필드의 이전 성공 안내를 제거한다.
	const successElementId = getSuccessElementId(elementId);
	if (successElementId !== "") {
		const successElement = document.querySelector(`#${successElementId}`);
		successElement.textContent = "";
		successElement.style.color = "";
	}
}

// 이메일 재발송 버튼 상태
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
	// 약관 아래 공통 알림은 제출 시작 시 이전 내용을 제거한다.
	const messageBox = document.querySelector("#signupMessage");
	messageBox.hidden = true;
	messageBox.textContent = "";
	messageBox.className = "form-alert";
}

function showMessage(message, isError) {
	// 제출 예외와 유효성 요약만 약관 아래 공통 알림 영역에 표시한다.
	const messageBox = document.querySelector("#signupMessage");
	const hasMessage = typeof message === "string" && message.trim() !== "";
	messageBox.hidden = false;
	messageBox.textContent = hasMessage ? message : "요청 처리 중 오류가 발생했습니다.";
	messageBox.className = `form-alert ${isError || !hasMessage ? "form-alert--error" : "form-alert--notice"}`;
}
