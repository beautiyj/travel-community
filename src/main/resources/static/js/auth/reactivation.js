/**
 * 탈퇴한 로컬 회원의 이메일 본인 확인 흐름을 처리한다.
 * 재활성화 대상 여부와 가입 이메일은 서버가 관리하고, 브라우저는 아이디와 인증번호만 전송한다.
 */
document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("reactivationForm");
    if (!form) {
        return;
    }

    const contextPath = form.dataset.contextPath;
    const completeUrl = form.dataset.completeUrl;
    const username = document.getElementById("reactivationUsername");
    const code = document.getElementById("reactivationCode");
    const sendButton = document.getElementById("sendReactivationCodeButton");
    const verifyButton = document.getElementById("verifyReactivationCodeButton");
    const verificationField = document.getElementById("reactivationVerificationField");
    let cooldownTimer;

    username.addEventListener("input", resetVerificationState);
    sendButton.addEventListener("click", sendVerificationCode);
    form.addEventListener("submit", verifyCode);

    async function sendVerificationCode() {
        clearMessages();
        if (!validateUsername()) {
            return;
        }

        sendButton.disabled = true;
        try {
            const result = await postForm("/auth/api/reactivation/send", {
                username: username.value
            });
            if (!result.success) {
                showError("reactivationUsernameError", result.message);
                sendButton.disabled = false;
                return;
            }

            verificationField.hidden = false;
            verifyButton.disabled = false;
            showSuccess(
                "reactivationSendSuccess",
                result.message || "입력한 정보로 재활성화 가능한 계정이 있으면 인증번호를 발송했습니다."
            );
            startCooldown();
            code.focus();
        } catch (error) {
            showError("reactivationUsernameError", "요청 처리 중 오류가 발생했습니다. 다시 시도해 주세요.");
            sendButton.disabled = false;
        }
    }

    async function verifyCode(event) {
        event.preventDefault();
        clearMessages();
        if (!validateUsername() || !validateCode()) {
            return;
        }

        verifyButton.disabled = true;
        try {
            const result = await postForm("/auth/api/reactivation/verify", {
                username: username.value,
                code: code.value.trim()
            });
            if (!result.success) {
                showError("reactivationCodeError", result.message);
                verifyButton.disabled = false;
                return;
            }

            window.location.assign(completeUrl);
        } catch (error) {
            showError("reactivationCodeError", "요청 처리 중 오류가 발생했습니다. 처음부터 다시 시도해 주세요.");
            verifyButton.disabled = false;
        }
    }

    async function postForm(path, values) {
        const response = await csrfFetch(`${contextPath}${path}`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            credentials: "same-origin",
            body: new URLSearchParams(values)
        });

        let result;
        try {
            result = await response.json();
        } catch (error) {
            throw new Error("invalid-json-response");
        }

        if (!response.ok && !result.message) {
            result.message = "요청 처리 중 오류가 발생했습니다. 다시 시도해 주세요.";
        }
        return result;
    }

    function validateUsername() {
        if (/^[a-z0-9]{5,20}$/.test(username.value)) {
            return true;
        }

        showError("reactivationUsernameError", "아이디는 소문자 영문 또는 숫자 5~20자로 입력해주세요.");
        return false;
    }

    function validateCode() {
        if (/^\d{6}$/.test(code.value.trim())) {
            return true;
        }

        showError("reactivationCodeError", "인증번호 6자리를 입력해 주세요.");
        return false;
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
        ["reactivationUsernameError", "reactivationSendSuccess", "reactivationCodeError"]
            .forEach((id) => {
                document.getElementById(id).textContent = "";
            });
    }

    function showError(id, message) {
        document.getElementById(id).textContent = message || "입력값을 확인해 주세요.";
    }

    function showSuccess(id, message) {
        document.getElementById(id).textContent = message;
    }
});
