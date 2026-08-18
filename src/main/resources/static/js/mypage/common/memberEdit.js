(() => {
    const profileImage = document.getElementById("profileImage");
    const profileImagePreview = document.getElementById("profileImagePreview");
    const profileImageFallback = document.getElementById("profileImageFallback");
    const profileImageName = document.getElementById("profileImageName");

    if (!profileImage || !profileImagePreview || !profileImageName) return;

    const originalPreviewSrc = profileImagePreview.getAttribute("src") || "";
    let previewObjectUrl = null;

    function clearPreviewObjectUrl() {
        if (previewObjectUrl) {
            URL.revokeObjectURL(previewObjectUrl);
            previewObjectUrl = null;
        }
    }

    function restoreOriginalPreview() {
        clearPreviewObjectUrl();
        profileImagePreview.src = originalPreviewSrc;
        profileImagePreview.hidden = !originalPreviewSrc;
        if (profileImageFallback) {
            profileImageFallback.hidden = Boolean(originalPreviewSrc);
        }
        profileImageName.textContent = "선택된 파일 없음";
    }

    profileImage.addEventListener("change", function () {
        const selectedFile = this.files && this.files[0];

        if (!selectedFile) {
            restoreOriginalPreview();
            return;
        }

        if (!selectedFile.type.startsWith("image/")) {
            this.value = "";
            restoreOriginalPreview();
            return;
        }

        clearPreviewObjectUrl();
        previewObjectUrl = URL.createObjectURL(selectedFile);
        profileImagePreview.src = previewObjectUrl;
        profileImagePreview.hidden = false;
        if (profileImageFallback) {
            profileImageFallback.hidden = true;
        }
        profileImageName.textContent = selectedFile.name;
    });

    window.addEventListener("beforeunload", clearPreviewObjectUrl);
})();
