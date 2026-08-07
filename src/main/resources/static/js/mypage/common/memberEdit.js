const profileImage = document.getElementById("profileImage");
profileImage.addEventListener("change", function () {
    document.getElementById("profileImageName").textContent =
        this.files.length ? this.files[0].name : "선택된 파일 없음";
});
