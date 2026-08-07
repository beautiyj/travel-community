(() => {
    const editForm = document.querySelector("form[data-edit-confirm]");
    const editModal = document.getElementById("editConfirmModal");
    const confirmButton = document.getElementById("editConfirmSubmit");
    if (!editForm || !editModal || !confirmButton) return;

    const closeModal = () => {
        editModal.hidden = true;
    };

    editForm.addEventListener("submit", (event) => {
        event.preventDefault();
        editModal.hidden = false;
        confirmButton.focus();
    });

    document.querySelectorAll(".js-edit-confirm-close").forEach((button) => {
        button.addEventListener("click", closeModal);
    });

    confirmButton.addEventListener("click", () => {
        confirmButton.disabled = true;
        editForm.submit();
    });

    editModal.addEventListener("click", (event) => {
        if (event.target === editModal) closeModal();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !editModal.hidden) closeModal();
    });
})();
