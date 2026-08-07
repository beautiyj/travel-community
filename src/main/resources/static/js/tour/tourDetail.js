document.addEventListener("DOMContentLoaded", function () {
    var btnZoom = document.getElementById("btnBannerZoom");
    var bannerWrapper = document.getElementById("bannerWrapper");
    var modal = document.getElementById("imageModal");
    var modalImg = document.getElementById("imageModalImg");
    var modalClose = document.getElementById("imageModalClose");

    if (btnZoom && bannerWrapper && modal && modalImg) {
        btnZoom.addEventListener("click", function (e) {
            e.stopPropagation(); // 배너 자체 클릭 이벤트와 분리

            var targetSrc = "";
            var imgs = bannerWrapper.querySelectorAll("img");

            if (imgs.length > 0) {
                // 현재 슬라이더에서 화면에 노출 중인(visible) 이미지 탐색
                for (var i = 0; i < imgs.length; i++) {
                    var img = imgs[i];
                    var parentSlide = img.closest('.banner-slide, .carousel-item, .swiper-slide, .slick-slide');
                    var isVisible = img.offsetWidth > 0 && img.offsetHeight > 0 && window.getComputedStyle(img).display !== 'none';

                    // 슬라이더 라이브러리별 활성화 클래스 체크
                    if (parentSlide) {
                        if (parentSlide.classList.contains('active') || parentSlide.classList.contains('is-active') || parentSlide.classList.contains('swiper-slide-active')) {
                            targetSrc = img.currentSrc || img.src;
                            break;
                        }
                    } else if (isVisible) {
                        targetSrc = img.currentSrc || img.src;
                        break;
                    }
                }

                // 감지 실패 시 첫 번째 이미지 fallback
                if (!targetSrc) {
                    targetSrc = imgs[0].currentSrc || imgs[0].src;
                }
            }

            if (targetSrc) {
                modalImg.src = targetSrc;
                modal.classList.add("show");
            }
        });

        // 모달 닫기
        if (modalClose) {
            modalClose.addEventListener("click", function (e) {
                e.stopPropagation();
                modal.classList.remove("show");
            });
        }

        modal.addEventListener("click", function (e) {
            if (e.target === modal || e.target === modalClose) {
                modal.classList.remove("show");
            }
        });
    }
});