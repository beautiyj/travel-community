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

// 아이디와 닉네임의 중복 확인 차이만 설정으로 두어 같은 흐름을 중복 구현하지 않는다.
const duplicateCheckConfigs = Object.freeze([
	Object.freeze({
		inputKey: "loginId",
		buttonKey: "checkUsernameButton",
		stateKey: "loginId",
		messageElementId: "usernameError",
		validator: signupValidation.validateLoginId,
		apiPath: "/auth/api/check-login-id",
		queryName: "loginId",
		changedMessage: "아이디가 변경되어 중복 확인이 필요합니다."
	}),
	Object.freeze({
		inputKey: "nickname",
		buttonKey: "checkNicknameButton",
		stateKey: "nickname",
		messageElementId: "nicknameError",
		validator: signupValidation.validateNickname,
		apiPath: "/auth/api/check-nickname",
		queryName: "nickname",
		changedMessage: "닉네임이 변경되어 중복 확인이 필요합니다."
	})
]);

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

// 화면이 준비되면 날짜 제한을 설정하고 회원가입 기능별 이벤트를 연결한다.
document.addEventListener("DOMContentLoaded", () => {
	const elements = getSignupElements();
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

// 회원가입 기능에서 사용하는 화면 요소를 한 곳에서 조회한다.
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

// 비밀번호와 비밀번호 확인값의 일치 여부를 입력 중에 안내한다.
function bindPasswordMatchEvents({ password, passwordConfirm }) {
	const updatePasswordMatchMessage = () => {
		clearMessage();
		clearFieldMessage("passwordError");
		clearFieldMessage("passwordConfirmError");
		if (passwordConfirm.value.length === 0) {
			return;
		}

		const validation = signupValidation.validatePasswordConfirm(
			password.value,
			passwordConfirm.value
		);
		if (validation.valid) {
			showFieldMessage("passwordConfirmError", "비밀번호가 일치합니다.", false);
			return;
		}
		setError("passwordConfirmError", validation.message);
	};

	[password, passwordConfirm].forEach((input) => {
		input.addEventListener("change", updatePasswordMatchMessage);
		input.addEventListener("input", updatePasswordMatchMessage);
	});
}

// 입력값이 바뀌면 해당 필드의 오류와 이전 제출 안내를 지운다.
function bindFieldMessageClearEvents() {
	[
		["name", "nameError"],
		["login_id", "usernameError"],
		["email", "emailError"],
		["verificationCode", "verificationCodeError"],
		["nickname", "nicknameError"],
		["birth", "birthError"],
		["phone", "phoneError"],
		["privacyAgreed", "privacyAgreedError"]
	].forEach(([fieldId, messageId]) => {
		document.querySelector(`#${fieldId}`).addEventListener("change", () => {
			clearFieldMessage(messageId);
			clearMessage();
		});
	});
}

// 확인 완료 후 원본 값이 바뀌면 서버에서 확인했던 상태를 무효화한다.
function bindWorkflowStateInvalidationEvents(elements) {
	duplicateCheckConfigs.forEach((config) => {
		elements[config.inputKey].addEventListener("input", () => {
			clearMessage();
			if (duplicateCheckState[config.stateKey] !== "") {
				duplicateCheckState[config.stateKey] = "";
				setError(config.messageElementId, config.changedMessage);
			}
		});
	});

	// 이메일이 변경되면 이전 인증 결과와 인증번호 입력값을 폐기한다.
	elements.email.addEventListener("input", () => {
		clearMessage();
		resetEmailVerification(elements);
	});
}

// 이메일 인증 상태와 인증번호 입력 화면을 초기 상태로 되돌린다.
function resetEmailVerification({ verificationCode, verificationField, verifyEmailButton, sendEmailButton }) {
	emailVerificationState.email = "";
	emailVerificationState.verified = false;
	verificationCode.value = "";
	verificationField.hidden = true;
	verifyEmailButton.disabled = false;
	clearEmailCooldown(sendEmailButton);
}

// 이메일 인증번호 발송과 검증 버튼의 동작을 연결한다.
function bindEmailVerificationEvents(elements) {
	elements.sendEmailButton.addEventListener("click", () => sendEmailVerificationCode(elements));
	elements.verifyEmailButton.addEventListener("click", () => verifyEmailVerificationCode(elements));
}

async function sendEmailVerificationCode(elements) {
	const { email, verificationCode, verificationField, sendEmailButton } = elements;
	clearMessage();
	clearFieldMessage("emailError");
	const validation = signupValidation.validateEmail(email.value);
	setError("emailError", validation.message);
	if (!validation.valid || sendEmailButton.disabled) {
		return;
	}

	sendEmailButton.disabled = true;
	try {
		const { response, result } = await postUrlEncoded(
			"/auth/api/email-verification/send",
			{ email: email.value }
		);
		if (!response.ok || !result.success) {
			showFieldMessage("emailError", result.message, true);
			clearEmailCooldown(sendEmailButton);
			return;
		}

		verificationField.hidden = false;
		showFieldMessage("emailError", result.message, false);
		startEmailCooldown(sendEmailButton, 60);
		verificationCode.focus();
	} catch (error) {
		showRequestError(error, "emailError");
		clearEmailCooldown(sendEmailButton);
	}
}

async function verifyEmailVerificationCode(elements) {
	const { email, verificationCode, sendEmailButton, verifyEmailButton } = elements;
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
		const { response, result } = await postUrlEncoded(
			"/auth/api/email-verification/verify",
			{ email: email.value, code }
		);
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
		showFieldMessage("emailError", result.message, false);
	} catch (error) {
		showRequestError(error, "emailError");
		verifyEmailButton.disabled = false;
	}
}

// 설정에 정의된 아이디·닉네임 중복 확인 버튼을 같은 흐름으로 연결한다.
function bindDuplicateCheckEvents(elements) {
	duplicateCheckConfigs.forEach((config) => {
		const input = elements[config.inputKey];
		elements[config.buttonKey].addEventListener("click", async () => {
			clearMessage();
			clearFieldMessage(config.messageElementId);
			const validation = config.validator(input.value);
			setError(config.messageElementId, validation.message);
			if (!validation.valid) {
				return;
			}

			const checkedValue = input.value;
			const query = `${config.queryName}=${encodeURIComponent(checkedValue)}`;
			const available = await checkAvailability(
				appUrl(`${config.apiPath}?${query}`),
				input,
				checkedValue,
				config.messageElementId
			);
			duplicateCheckState[config.stateKey] = available ? checkedValue : "";
		});
	});
}

// 최종 입력값을 검증하고 회원가입 요청을 한 번만 전송한다.
function bindSignupSubmitEvent(elements) {
	const { form, birth, email, signupSubmitButton, signupSubmitButtonText } = elements;
	let signupSubmitting = false;

	form.addEventListener("submit", async (event) => {
		event.preventDefault();
		if (signupSubmitting) {
			return;
		}

		clearMessage();
		const localToday = signupValidation.toLocalDateString(new Date());
		birth.max = localToday;
		const validation = signupValidation.validateSignup(
			readSignupValues(form),
			readSignupWorkflowState(),
			localToday
		);
		renderSignupErrors(validation.errors);

		if (!validation.valid) {
			showMessage("입력항목을 확인해주세요.", true);
			return;
		}

		// 네트워크 요청 중에는 버튼과 상태값을 함께 잠가 중복 가입을 차단한다.
		signupSubmitting = true;
		signupSubmitButton.disabled = true;
		signupSubmitButton.textContent = "가입 처리 중...";
		let signupSucceeded = false;
		try {
			const { response, result } = await requestJson(form.action, {
				method: "POST",
				body: new FormData(form),
				headers: { Accept: "application/json" }
			});

			if (!response.ok || !result.success) {
				// 세션 없음·만료·사용 완료는 같은 이메일로 다시 인증할 수 있게 초기화한다.
				if (result.code === "EMAIL_REVERIFICATION_REQUIRED") {
					resetEmailVerification(elements);
					clearFieldMessage("emailError");
					clearFieldMessage("verificationCodeError");
					showFieldMessage("emailError", result.message, true);
					email.focus();
					return;
				}
				showMessage(result.message, true);
				return;
			}

			window.location.assign(appUrl("/auth/signupresult"));
			signupSucceeded = true;
		} catch (error) {
			showRequestError(error);
		} finally {
			// 성공 후 페이지가 이동하는 동안에는 버튼을 잠근 상태로 유지한다.
			if (signupSucceeded) {
				return;
			}
			signupSubmitting = false;
			signupSubmitButton.disabled = false;
			signupSubmitButton.textContent = signupSubmitButtonText;
		}
	});
}

// 중복 확인 응답이 오기 전에 입력값이 바뀌면 해당 응답을 사용하지 않는다.
async function checkAvailability(url, input, checkedValue, messageElementId) {
	try {
		const { response, result } = await requestJson(url, {
			method: "GET",
			headers: { Accept: "application/json" }
		});
		if (!response.ok || !result.success) {
			showFieldMessage(
				messageElementId,
				result.message || "중복 확인에 실패했습니다.",
				true
			);
			return false;
		}

		if (input.value !== checkedValue) {
			return false;
		}

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

// form action 경로에서 현재 애플리케이션의 컨텍스트 경로를 추출한다.
function resolveAppBasePath(action) {
	const pathname = new URL(action, window.location.href).pathname;
	return pathname.replace(/\/auth\/membersignup\/?$/, "");
}

function appUrl(path) {
	return `${appBasePath}${path}`;
}

// fetch와 JSON 형식 검사를 한 곳에서 처리해 각 기능은 업무 결과만 판단한다.
async function requestJson(url, options) {
	const response = await fetch(url, options);
	const result = await parseJsonResponse(response);
	return { response, result };
}

function postUrlEncoded(path, values) {
	return requestJson(appUrl(path), {
		method: "POST",
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
			Accept: "application/json"
		},
		body: new URLSearchParams(values)
	});
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

// 오류·성공 메시지 태그 조회를 공통화해 세 메시지 함수의 DOM 분기를 일치시킨다.
function getFieldMessageElements(errorElementId) {
	const successElementId = signupSuccessElementIds[errorElementId];
	return {
		errorElement: document.querySelector(`#${errorElementId}`),
		successElement: successElementId
			? document.querySelector(`#${successElementId}`)
			: null
	};
}

function showFieldMessage(elementId, message, isError) {
	const { errorElement, successElement } = getFieldMessageElements(elementId);
	const hasMessage = typeof message === "string" && message.trim() !== "";
	const displayMessage = hasMessage ? message : "요청 처리 중 오류가 발생했습니다.";
	errorElement.textContent = !isError && hasMessage ? "" : displayMessage;
	if (successElement) {
		successElement.textContent = !isError && hasMessage ? message : "";
		successElement.style.color = !isError && hasMessage ? "#087f5b" : "";
	}
}

function clearFieldMessage(elementId) {
	const { errorElement, successElement } = getFieldMessageElements(elementId);
	errorElement.textContent = "";
	if (successElement) {
		successElement.textContent = "";
		successElement.style.color = "";
	}
}

function setError(elementId, message) {
	const { errorElement, successElement } = getFieldMessageElements(elementId);
	errorElement.textContent = message;
	if (successElement) {
		successElement.textContent = "";
		successElement.style.color = "";
	}
}

// 응답 형식 오류를 네트워크 오류와 구분하기 위한 전용 오류다.
class ResponseFormatError extends Error {
	constructor(status) {
		super(`서버 응답을 처리하지 못했습니다. (HTTP ${status})`);
		this.name = "ResponseFormatError";
	}
}

// trim이나 정규화 없이 DTO로 전송될 원본 입력값을 복사한다.
function readSignupValues(form) {
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
	return {
		checkedLoginId: duplicateCheckState.loginId,
		checkedNickname: duplicateCheckState.nickname,
		verifiedEmail: emailVerificationState.email,
		emailVerified: emailVerificationState.verified
	};
}

function renderSignupErrors(errors) {
	Object.entries(signupErrorElementIds).forEach(([field, elementId]) => {
		setError(elementId, errors[field] || "");
	});
}

// 발송 성공 후 지정 시간 동안 버튼을 잠가 중복 클릭을 줄인다.
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

function clearEmailCooldown(button) {
	if (emailCooldownTimer !== null) {
		window.clearInterval(emailCooldownTimer);
		emailCooldownTimer = null;
	}
	button.disabled = false;
	button.textContent = "인증번호 발송";
}

function clearMessage() {
	const messageBox = document.querySelector("#signupMessage");
	messageBox.hidden = true;
	messageBox.textContent = "";
	messageBox.className = "form-alert";
}

function showMessage(message, isError) {
	const messageBox = document.querySelector("#signupMessage");
	const hasMessage = typeof message === "string" && message.trim() !== "";
	messageBox.hidden = false;
	messageBox.textContent = hasMessage ? message : "요청 처리 중 오류가 발생했습니다.";
	messageBox.className = `form-alert ${isError || !hasMessage ? "form-alert--error" : "form-alert--notice"}`;
}
