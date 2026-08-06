// 업소 관리(읽기 뷰) 사진 갤러리 확대보기.
document.addEventListener("DOMContentLoaded", function () {
    var lightbox = document.getElementById("venueImageLightbox");
    var lightboxImg = document.getElementById("venueLightboxImg");
    if (!lightbox || !lightboxImg) return;

    document.querySelectorAll(".js-venue-gallery-trigger").forEach(function (item) {
        item.addEventListener("click", function () {
            var img = item.querySelector("img");
            if (!img) return;
            lightboxImg.src = img.src;
            lightboxImg.alt = img.alt;
            window.openModal("venueImageLightbox");
        });
    });
});
