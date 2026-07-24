(() => {
	"use strict";

	// [수정] 첫 화면에서도 동작하도록 DOM이 이미 준비된 경우에는 즉시 초기화한다.
	function initializeSocialSignup() {
		const form = document.querySelector("#socialSignupForm");
		if (!form || form.dataset.socialSignupBound === "true") {
			return;
		}
		form.dataset.socialSignupBound = "true";

		// [추가] 이 검사는 입력 편의를 위한 UX이며, 서버의 재검사와 DB UNIQUE 제약을 대신하지 않는다.

		const nameInput = document.querySelector("#name");
		const nicknameInput = document.querySelector("#nickname");
		const privacyAgreedInput = document.querySelector("#privacyAgreed");
		const checkNicknameButton = document.querySelector("#checkNicknameButton");
		const nameError = document.querySelector("#nameError");
		const nicknameError = document.querySelector("#nicknameError");
		const nicknameSuccess = document.querySelector("#nicknameSuccess");
		const privacyAgreedError = document.querySelector("#privacyAgreedError");
		let checkedNickname = "";

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

		privacyAgreedInput.addEventListener("change", () => {
			privacyAgreedError.textContent = "";
		});

		form.addEventListener("submit", (event) => {
			const nameValidation = window.SignupValidation.validateName(nameInput.value);
			const validation = window.SignupValidation.validateNickname(nicknameInput.value);
			const privacyValidation = window.SignupValidation.validatePrivacyAgreement(
				privacyAgreedInput.checked
			);
			nameError.textContent = nameValidation.message;
			privacyAgreedError.textContent = privacyValidation.message;

			if (!nameValidation.valid) {
				event.preventDefault();
				nameInput.focus();
			}
			if (!validation.valid) {
				event.preventDefault();
				clearNicknameMessage();
				showError(validation.message);
				if (nameValidation.valid) {
					nicknameInput.focus();
				}
				return;
			}

			if (checkedNickname !== nicknameInput.value) {
				event.preventDefault();
				clearNicknameMessage();
				showError("닉네임 중복 확인이 필요합니다.");
				if (nameValidation.valid) {
					nicknameInput.focus();
				}
				return;
			}

			if (!privacyValidation.valid) {
				event.preventDefault();
				if (nameValidation.valid) {
					privacyAgreedInput.focus();
				}
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
