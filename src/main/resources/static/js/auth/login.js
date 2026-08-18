/**
 * 로그인 폼의 표시 전환과 빈 값 검사를 담당한다.
 * 유효한 자격증명인지, 계정이 잠겼는지, 로그인 가능한 상태인지는 POST 요청을 받는 서버가 판단한다.
 */
document.addEventListener("DOMContentLoaded", () => {
	// 로그인 화면 요소
	const form = document.querySelector("#loginForm");
	const username = document.querySelector("#username");
	const password = document.querySelector("#password");
	const togglePassword = document.querySelector("#togglePassword");

	// 비밀번호 표시 전환
	togglePassword.addEventListener("click", () => {
		const shouldShow = password.type === "password";
		password.type = shouldShow ? "text" : "password";
		togglePassword.textContent = shouldShow ? "숨기기" : "보기";
		togglePassword.setAttribute("aria-label", shouldShow ? "비밀번호 숨기기" : "비밀번호 표시");
	});

	// 제출 전에 필수 입력만 확인하고, 통과하면 브라우저의 일반 form POST 흐름을 그대로 진행한다.
	form.addEventListener("submit", (event) => {
		let valid = true;

		if (!username.value) {
			setError("usernameError", "아이디를 입력해주세요.");
			valid = false;
		} else if (!/^[a-z0-9]{5,20}$/.test(username.value)) {
			setError("usernameError", "아이디는 소문자 영문 또는 숫자 5~20자로 입력해주세요.");
			valid = false;
		} else {
			setError("usernameError", "");
		}

		if (!password.value) {
			setError("passwordError", "비밀번호를 입력해주세요.");
			valid = false;
		} else {
			setError("passwordError", "");
		}

		if (!valid) {
			event.preventDefault();
		}
	});
});

// 입력 필드 아래의 오류 메시지 갱신
function setError(elementId, message) {
	document.querySelector(`#${elementId}`).textContent = message;
}
