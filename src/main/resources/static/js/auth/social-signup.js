(() => {
	"use strict";

	// 첫 화면에서도 동작하도록 DOM이 이미 준비된 경우에는 즉시 초기화한다.
	function initializeSocialSignup() {
		const form = document.querySelector("#socialSignupForm");
		if (!form || form.dataset.socialSignupBound === "true") {
			return;
		}
		form.dataset.socialSignupBound = "true";

		// 이 검사는 입력 편의를 위한 UX이며, 서버의 재검사와 DB UNIQUE 제약을 대신하지 않는다.

		const nameInput = document.querySelector("#name");
		const nicknameInput = document.querySelector("#nickname");
		// 로컬 회원가입과 공통 생년월일·전화번호 검증 함수를 사용한다.
		const birthInput = document.querySelector("#birth");
		const phoneInput = document.querySelector("#phone");
		const genderInputs = Array.from(form.querySelectorAll("input[name='gender']"));
		const privacyAgreedInput = document.querySelector("#privacyAgreed");
		const checkNicknameButton = document.querySelector("#checkNicknameButton");
		const nameError = document.querySelector("#nameError");
		const nicknameError = document.querySelector("#nicknameError");
		const nicknameSuccess = document.querySelector("#nicknameSuccess");
		const birthError = document.querySelector("#birthError");
		const phoneError = document.querySelector("#phoneError");
		const genderError = document.querySelector("#genderError");
		const privacyAgreedError = document.querySelector("#privacyAgreedError");
		let checkedNickname = "";

		// 브라우저 날짜 선택에서도 미래 날짜를 고를 수 없게 제한한다.
		const today = window.SignupValidation.toLocalDateString(new Date());
		birthInput.max = today;

		checkNicknameButton.addEventListener("click", async () => {
			clearNicknameMessage();
			checkedNickname = "";

			const nickname = nicknameInput.value;
			const validation = window.SignupValidation.validateNickname(nickname);
			if (!validation.valid) {
				showError(validation.message);
				nicknameInput.focus();
				return;
			}

			checkNicknameButton.disabled = true;
			try {
				const url = createNicknameCheckUrl(form.action, nickname);
				const response = await fetch(url, {
					method: "GET",
					cache: "no-store",
					headers: { Accept: "application/json" }
				});
				const result = await response.json();

				// 요청 중 입력값이 바뀌었다면 이전 값에 대한 응답은 사용하지 않는다.
				if (nicknameInput.value !== nickname) {
					showError("닉네임이 변경되어 중복 확인이 필요합니다.");
					return;
				}

				if (!response.ok || !result.success) {
					showError(result.message || "닉네임 중복 확인에 실패했습니다.");
					return;
				}

				if (!result.available) {
					showError(result.message || "이미 사용 중인 닉네임입니다.");
					nicknameInput.focus();
					return;
				}

				checkedNickname = nickname;
				showSuccess(result.message || "사용 가능한 닉네임입니다.");
			} catch (error) {
				showError("닉네임 중복 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
			} finally {
				checkNicknameButton.disabled = false;
			}
		});

		nicknameInput.addEventListener("input", () => {
			const wasChecked = checkedNickname !== "";
			checkedNickname = "";
			clearNicknameMessage();
			if (wasChecked) {
				showError("닉네임이 변경되어 중복 확인이 필요합니다.");
			}
		});

		nameInput.addEventListener("input", () => {
			nameError.textContent = "";
		});

		birthInput.addEventListener("input", () => {
			birthError.textContent = "";
		});

		phoneInput.addEventListener("input", () => {
			phoneError.textContent = "";
		});

		genderInputs.forEach((input) => {
			input.addEventListener("change", () => {
				genderError.textContent = "";
			});
		});

		privacyAgreedInput.addEventListener("change", () => {
			privacyAgreedError.textContent = "";
		});

		form.addEventListener("submit", (event) => {
			const nameValidation = window.SignupValidation.validateName(nameInput.value);
			const validation = window.SignupValidation.validateNickname(nicknameInput.value);
			const birthValidation = window.SignupValidation.validateBirth(
				birthInput.value,
				today
			);
			const phoneValidation = window.SignupValidation.validatePhone(phoneInput.value);
			const selectedGender = genderInputs.find((input) => input.checked)?.value;
			const genderValid = selectedGender === undefined
				|| selectedGender === "MALE"
				|| selectedGender === "FEMALE";
			const privacyValidation = window.SignupValidation.validatePrivacyAgreement(
				privacyAgreedInput.checked
			);
			nameError.textContent = nameValidation.message;
			birthError.textContent = birthValidation.message;
			phoneError.textContent = phoneValidation.message;
			genderError.textContent = genderValid ? "" : "성별 값이 올바르지 않습니다.";
			privacyAgreedError.textContent = privacyValidation.message;

			if (!nameValidation.valid) {
				event.preventDefault();
				nameInput.focus();
				return;
			}
			if (!validation.valid) {
				event.preventDefault();
				clearNicknameMessage();
				showError(validation.message);
				nicknameInput.focus();
				return;
			}

			if (checkedNickname !== nicknameInput.value) {
				event.preventDefault();
				clearNicknameMessage();
				showError("닉네임 중복 확인이 필요합니다.");
				nicknameInput.focus();
				return;
			}

			if (!birthValidation.valid) {
				event.preventDefault();
				birthInput.focus();
				return;
			}

			if (!phoneValidation.valid) {
				event.preventDefault();
				phoneInput.focus();
				return;
			}

			if (!genderValid) {
				event.preventDefault();
				genderInputs[0]?.focus();
				return;
			}

			if (!privacyValidation.valid) {
				event.preventDefault();
				privacyAgreedInput.focus();
			}
		});

		function clearNicknameMessage() {
			nicknameError.textContent = "";
			nicknameSuccess.textContent = "";
		}

		function showError(message) {
			nicknameError.textContent = message;
			nicknameSuccess.textContent = "";
		}

		function showSuccess(message) {
			nicknameError.textContent = "";
			nicknameSuccess.textContent = message;
		}
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initializeSocialSignup, { once: true });
	} else {
		initializeSocialSignup();
	}

	// form action을 기준으로 URL을 만들어 컨텍스트 경로가 있는 배포 환경에서도 같은 API를 호출한다.
	function createNicknameCheckUrl(formAction, nickname) {
		const url = new URL(formAction, window.location.href);
		url.pathname = url.pathname.replace(
			/\/auth\/social\/signup\/?$/,
			"/auth/api/check-nickname"
		);
		url.search = "";
		url.searchParams.set("nickname", nickname);
		return url.toString();
	}
})();
