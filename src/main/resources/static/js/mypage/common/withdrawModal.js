(() => {
    const openBtn = document.getElementById("js-withdraw-open");
    const modal = document.getElementById("withdrawModal");
    const form = document.getElementById("withdrawForm");
    if (!openBtn || !modal || !form) return;

    const closeModal = () => {
        modal.hidden = true;
        form.reset();
    };

    openBtn.addEventListener("click", () => {
        modal.hidden = false;
        document.getElementById("currentPassword").focus();
    });

    modal.querySelectorAll(".js-withdraw-close").forEach((button) => {
        button.addEventListener("click", closeModal);
    });

    modal.addEventListener("click", (event) => {
        if (event.target === modal) closeModal();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) closeModal();
    });

    const postForm = (url) =>
        csrfFetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams(new FormData(form)),
        }).then((res) => res.json().then((data) => ({ ok: res.ok, data })));

    form.addEventListener("submit", (event) => {
        event.preventDefault();

        postForm(form.action + "/check-password")
            .then(({ ok, data }) => {
                if (!ok) {
                    alert(data.message);
                    return;
                }
                if (!confirm("정말 탈퇴하시겠습니까?")) {
                    return;
                }
                return postForm(form.action).then(({ ok: withdrawOk, data: withdrawData }) => {
                    alert(withdrawData.message);
                    if (withdrawOk) {
                        window.location.href = form.dataset.loginUrl;
                    }
                });
            })
            .catch(() => alert("잠시 후 다시 시도해 주세요."));
    });
})();
