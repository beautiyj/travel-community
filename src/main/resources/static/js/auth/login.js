document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#loginForm");
    const username = document.querySelector("#username");
    const password = document.querySelector("#password");
    const togglePassword = document.querySelector("#togglePassword");

    togglePassword.addEventListener("click", () => {
        const shouldShow = password.type === "password";
        password.type = shouldShow ? "text" : "password";
        togglePassword.textContent = shouldShow ? "숨기기" : "보기";
        togglePassword.setAttribute("aria-label", shouldShow ? "비밀번호 숨기기" : "비밀번호 표시");
    });

    form.addEventListener("submit", (event) => {
        let valid = true;

        if (!username.value.trim()) {
            setError("usernameError", "아이디를 입력해주세요.");
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

function setError(elementId, message) {
    document.querySelector(`#${elementId}`).textContent = message;
}
