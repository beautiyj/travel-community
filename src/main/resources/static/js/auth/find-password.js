/**
 * 비밀번호 재설정용 이메일 인증 흐름을 제어한다.
 * 계정 입력 검증 → 인증번호 발송 → 인증번호 확인 → 새 비밀번호 화면 이동 순서이며,
 * 인증 성공과 재설정 대상 회원은 브라우저 상태가 아닌 서버의 세션·DB 기록으로 확정한다.
 */
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

        // 발송 요청이 끝날 때까지 버튼을 잠가 같은 화면에서의 연속 클릭을 줄인다.
        // 서버도 재시도 제한과 중복 발송을 별도로 통제해야 한다.
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
        // form의 기본 제출 대신 JSON API로 인증한 뒤 성공할 때만 재설정 화면으로 이동한다.
        event.preventDefault();
        clearMessages();
        if (!validateAccountInput() || !validateCode()) {
            return;
        }

        // 검증 요청 중 중복 제출을 막고, 실패하거나 통신 오류가 나면 다시 사용할 수 있게 복구한다.
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
        // 컨텍스트 경로를 포함해 배포 위치와 무관하게 같은 애플리케이션의 인증 API를 호출한다.
        const response = await csrfFetch(`${contextPath}${path}`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams(values)
        });
        return response.json();
    }

    function validateAccountInput() {
        // 화면 검증은 요청 횟수를 줄이는 편의 기능이며 서버의 형식·계정 일치 검증을 대체하지 않는다.
        let valid = true;
        if (!/^[a-z0-9]{5,20}$/.test(username.value)) {
            showError("findPasswordUsernameError", "아이디는 소문자 영문 또는 숫자 5~20자로 입력해주세요.");
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
        // 계정 정보가 달라지면 이전 계정으로 받은 인증번호와 화면상 성공 상태를 재사용하지 않는다.
        verificationField.hidden = true;
        verifyButton.disabled = true;
        code.value = "";
        clearMessages();
        clearCooldown();
    }

    function startCooldown() {
        // 60초 카운트다운은 UI 중복 클릭 방지용이다. 새로고침으로 우회할 수 있으므로 서버 제한이 기준이다.
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
