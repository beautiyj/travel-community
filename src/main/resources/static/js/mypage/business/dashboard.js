const documentInput = document.getElementById("document");
if (documentInput) {
    documentInput.addEventListener("change", function () {
        document.getElementById("document-file-name").textContent =
            this.files.length ? this.files[0].name : "선택된 파일 없음";
    });
}

const approvalCancelModal =
    document.getElementById("businessApprovalCancelModal");
if (approvalCancelModal) {
    const openButton = document.querySelector(
        ".js-business-approval-cancel-open");
    const closeButtons = approvalCancelModal.querySelectorAll(
        ".js-business-approval-cancel-close");

    openButton.addEventListener("click", function () {
        approvalCancelModal.hidden = false;
    });
    closeButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            approvalCancelModal.hidden = true;
        });
    });
    approvalCancelModal.addEventListener("click", function (event) {
        if (event.target === approvalCancelModal) {
            approvalCancelModal.hidden = true;
        }
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !approvalCancelModal.hidden) {
            approvalCancelModal.hidden = true;
        }
    });
}
