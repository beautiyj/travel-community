document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("resetPasswordForm");
    if (!form) {
        return;
    }

    const password = document.getElementById("newPassword");
    const passwordConfirm = document.getElementById("newPasswordConfirm");

    // 두 입력값이 바뀔 때마다 일치 여부를 즉시 안내한다.
    [password, passwordConfirm].forEach((input) => {
        input.addEventListener("input", updatePasswordMatchMessage);
    });

    form.addEventListener("submit", (event) => {
        clearErrors();
        let valid = true;
        if (!/^(?=.*[A-Za-z])(?=.*\d)\S{8,20}$/.test(password.value)) {
            document.getElementById("newPasswordError").textContent =
                "비밀번호는 공백 없이 영문과 숫자를 포함한 8~20자로 입력해주세요.";
            valid = false;
        }
        if (password.value !== passwordConfirm.value) {
            document.getElementById("newPasswordConfirmError").textContent =
                "비밀번호와 비밀번호 확인이 일치하지 않습니다.";
            valid = false;
        }
        if (!valid) {
            event.preventDefault();
        }
    });

    function clearErrors() {
        document.getElementById("newPasswordError").textContent = "";
        document.getElementById("newPasswordConfirmError").textContent = "";
        document.getElementById("newPasswordConfirmSuccess").textContent = "";
    }

    function updatePasswordMatchMessage() {
        const error = document.getElementById("newPasswordConfirmError");
        const success = document.getElementById("newPasswordConfirmSuccess");
        error.textContent = "";
        success.textContent = "";

        if (passwordConfirm.value.length === 0) {
            return;
        }
        if (password.value === passwordConfirm.value) {
            success.textContent = "비밀번호가 일치합니다.";
            return;
        }
        error.textContent = "비밀번호가 일치하지 않습니다.";
    }
});
