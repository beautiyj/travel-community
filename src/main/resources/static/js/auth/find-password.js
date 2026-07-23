document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("findPasswordForm");
    if (!form) {
        return;
    }

    const contextPath = form.dataset.contextPath;
    const resetPasswordUrl = form.dataset.resetPasswordUrl;
    const username = document.getElementById("findPasswordUsername");
    const email = document.getElementById("findPasswordEmail");
    const code = document.getElementById("findPasswordCode");
    const sendButton = document.getElementById("sendPasswordCodeButton");
    const verifyButton = document.getElementById("verifyPasswordCodeButton");
    const verificationField = document.getElementById("passwordVerificationField");
    let cooldownTimer;

    // 아이디 또는 이메일을 바꾸면 이전 인증번호를 현재 입력값에 사용할 수 없게 화면 상태를 초기화한다.
    [username, email].forEach((input) => input.addEventListener("input", resetVerificationState));
    sendButton.addEventListener("click", sendVerificationCode);
    form.addEventListener("submit", verifyCode);

    async function sendVerificationCode() {
        clearMessages();
        if (!validateAccountInput()) {
            return;
        }

        sendButton.disabled = true;
        try {
            const result = await postForm("/auth/api/password-reset/send", {
                username: username.value,
                email: email.value
            });
            if (!result.success) {
                showError("findPasswordEmailError", result.message);
                sendButton.disabled = false;
                return;
            }

            verificationField.hidden = false;
            verifyButton.disabled = false;
            showSuccess("findPasswordEmailSuccess", result.message);
            startCooldown();
            code.focus();
        } catch (error) {
            showError("findPasswordEmailError", "요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
            sendButton.disabled = false;
        }
    }

    async function verifyCode(event) {
        event.preventDefault();
        clearMessages();
        if (!validateAccountInput() || !validateCode()) {
            return;
        }

        verifyButton.disabled = true;
        try {
            const result = await postForm("/auth/api/password-reset/verify", {
                username: username.value,
                email: email.value,
                code: code.value.trim()
            });
            if (!result.success) {
                showError("findPasswordCodeError", result.message);
                verifyButton.disabled = false;
                return;
            }

            window.location.href = resetPasswordUrl;
        } catch (error) {
            showError("findPasswordCodeError", "요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
            verifyButton.disabled = false;
        }
    }

    async function postForm(path, values) {
        const response = await fetch(`${contextPath}${path}`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams(values)
        });
        return response.json();
    }

    function validateAccountInput() {
        let valid = true;
        if (!/^[A-Za-z0-9]{5,20}$/.test(username.value)) {
            showError("findPasswordUsernameError", "아이디는 영문 또는 숫자 5~20자로 입력해주세요.");
            valid = false;
        }
        if (email.value.length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
            showError("findPasswordEmailError", "올바른 이메일 주소를 입력해주세요.");
            valid = false;
        }
        return valid;
    }

    function validateCode() {
        if (!/^\d{6}$/.test(code.value.trim())) {
            showError("findPasswordCodeError", "인증번호 6자리를 입력해주세요.");
            return false;
        }
        return true;
    }

    function resetVerificationState() {
        verificationField.hidden = true;
        verifyButton.disabled = true;
        code.value = "";
        clearMessages();
        clearCooldown();
    }

    function startCooldown() {
        let remainingSeconds = 60;
        sendButton.disabled = true;
        sendButton.textContent = `재발송 (${remainingSeconds})`;
        clearInterval(cooldownTimer);
        cooldownTimer = setInterval(() => {
            remainingSeconds -= 1;
            if (remainingSeconds <= 0) {
                clearCooldown();
                return;
            }
            sendButton.textContent = `재발송 (${remainingSeconds})`;
        }, 1000);
    }

    function clearCooldown() {
        clearInterval(cooldownTimer);
        sendButton.disabled = false;
        sendButton.textContent = "인증번호 발송";
    }

    function clearMessages() {
        ["findPasswordUsernameError", "findPasswordEmailError", "findPasswordEmailSuccess", "findPasswordCodeError"]
            .forEach((id) => { document.getElementById(id).textContent = ""; });
    }

    function showError(id, message) {
        document.getElementById(id).textContent = message || "입력값을 확인해주세요.";
    }

    function showSuccess(id, message) {
        document.getElementById(id).textContent = message;
    }
});
