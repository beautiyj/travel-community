function getContextPath() {
    // Prefer explicit context path injected from server-side JSP (footer.jsp)
    if (typeof window.__CONTEXT_PATH__ !== 'undefined') {
        return window.__CONTEXT_PATH__ || '';
    }
    // Fallback: assume root context when unsure
    return '';
}

function toggleWishLocal(buttonElement, isActive) {
    const iconElement = buttonElement.querySelector('.wish-icon');

    if (isActive) {
        iconElement.classList.add('is-active');
        buttonElement.setAttribute('data-active', 'true');
        buttonElement.setAttribute('aria-pressed', 'true');
    } else {
        iconElement.classList.remove('is-active');
        buttonElement.setAttribute('data-active', 'false');
        buttonElement.setAttribute('aria-pressed', 'false');
    }
}

async function toggleWishlist(buttonElement) {
    const placeId = buttonElement.dataset.placeId;
    if (!placeId) {
        showWishToast(false, '찜할 장소 정보를 찾을 수 없습니다.');
        return;
    }

    const isActive = buttonElement.getAttribute('data-active') === 'true';
    const url = `${getContextPath()}/mypage/wishlist/toggle`;

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({ placeId })
        });

        const data = await response.json();
        if (!response.ok) {
            showWishToast(false, data?.message || '찜 상태 변경에 실패했습니다.');
            if (response.status === 401) {
                window.location.href = `${getContextPath()}/auth/login`;
            }
            return;
        }

        toggleWishLocal(buttonElement, data.wishlisted);
        showWishToast(data.wishlisted, data.message);
    } catch (error) {
        console.error('wishlist toggle failed', error);
        showWishToast(false, '네트워크 오류로 찜 상태를 변경할 수 없습니다.');
    }
}

/* 셀렉터블 컴포넌트 인터랙션 핸들러 */
document.addEventListener("DOMContentLoaded", function () {

    // 카드 내 위시버튼 클릭 시 상세 페이지 이동 차단 및 찜 API 호출/토스트 표시
    const wishButtons = document.querySelectorAll(".btn-wish-trigger[data-place-id]");

    wishButtons.forEach(btn => {
        btn.addEventListener("click", function (e) {
            if (this.closest('a')) {
                e.preventDefault();   // 부모 <a> 태그의 이동 막기
                e.stopPropagation();  // 이벤트 전파 차단
            }

            toggleWishlist(this);
        });
    });

    document.addEventListener("click", function (event) {

        // 셀렉터블 역할 카드 감지 ([class^='sel-card-col-'])
        const roleCard = event.target.closest("[class^='sel-card-col-']");
        if (roleCard) {
            const groupName = roleCard.getAttribute("data-card-group");
            
            // 동일한 단일선택 그룹에 묶여 있는 카드들을 전부 불러와 초기화 (라디오 버튼 스위칭 메커니즘)
            const siblingCards = document.querySelectorAll(`[data-card-group="${groupName}"]`);
            siblingCards.forEach(card => {
                card.classList.remove("active");
            });

            // 클릭한 카드만 활성화
            roleCard.classList.add("active");

            // 비즈니스 로직 연동 - 회원가입 폼의 hidden input(id="memberRole") 값 동적 변경
            // BUSINESS 설정된 셀렉터블 카드 클릭 후 회원가입 -> 서버로 전송될 hidden input의 value=BUSINESS로 변경 역할
            const selectedRoleId = roleCard.getAttribute("data-card-id"); // ROLE_USER 또는 ROLE_BUSINESS 등
            const roleHiddenInput = document.getElementById("memberRole");
            if (roleHiddenInput) {
                roleHiddenInput.value = selectedRoleId;
            }
        }
    });
});

// 카드컴포넌트의 위시버튼 - 신규 토스트 알림 제어 함수 추가
let toastTimeout;

function createWishToastElement() {
    let toast = document.getElementById("wishToast");
    if (toast) return toast;

    toast = document.createElement("div");
    toast.id = "wishToast";
    toast.className = "wish-toast";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    toast.style.display = "none";

    const message = document.createElement("div");
    message.className = "wish-toast__message";
    const messageSpan = document.createElement("span");
    messageSpan.id = "wishToastMsg";
    message.appendChild(messageSpan);

    const link = document.createElement("a");
    link.id = "wishToastLink";
    link.className = "wish-toast-link";
    link.textContent = "내 찜목록 보기";
    link.href = `${getContextPath()}/mypage/wishlist`;

    toast.appendChild(message);
    toast.appendChild(link);
    document.body.appendChild(toast);

    return toast;
}

function showWishToast(isAdded, customMessage) {
    const toast = createWishToastElement();
    const toastMsg = document.getElementById("wishToastMsg");
    const toastLink = document.getElementById("wishToastLink");

    if (!toast || !toastMsg || !toastLink) return;

    toastMsg.textContent = customMessage || (isAdded ? "찜 목록에 추가되었습니다." : "찜 목록에서 삭제되었습니다.");
    toastLink.href = `${getContextPath()}/mypage/wishlist`;

    toast.style.display = "flex";
    setTimeout(() => { toast.classList.add("show"); }, 10);

    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        toast.classList.remove("show");
        setTimeout(() => { toast.style.display = "none"; }, 300);
    }, 3000);
}

/* ============================================================
   공통 모달 (confirmModal.jsp) 열기/닫기
   - 오버레이 클릭 / 취소 버튼([data-modal-close]) / ESC 로 닫힘
   - openModal(id, value) : value 를 주면 hidden 필드에 주입
     (목록에서 행마다 대상이 다를 때 모달 1개를 재사용)
   ============================================================ */
(function () {
    'use strict';

    /* buttonComponent.jsp 원본을 폼 안에서 쓰기 위한 보정
       - <button> 에 type 이 없으면 브라우저가 submit 으로 봄 → 취소를 눌러도 폼이 전송됨
       - 컴포넌트에 onclick="alert('Pressed!')" 가 박혀 있음 → 모달에서는 방해
       ※ buttonComponent.jsp 를 고칠 수 있게 되면(type 파라미터 추가 + alert 제거)
          이 함수는 지워도 됩니다. */
    function normalizeButtons(overlay) {
        const cancel  = overlay.querySelector('.modal-btn-cancel .btn-main');
        const confirm = overlay.querySelector('.modal-btn-confirm .btn-main');

        if (cancel) {
            cancel.type = 'button';      // 폼 전송 막기
            cancel.onclick = null;       // 컴포넌트의 alert 제거
        }
        if (confirm) {
            confirm.type = 'submit';     // 확정만 전송
            confirm.onclick = null;
        }
    }

    function openModal(id, value) {
        const overlay = document.getElementById(id);
        if (!overlay) return;

        normalizeButtons(overlay);

        // 목록에서 행마다 대상이 다른 경우 hidden 값 주입
        if (value !== undefined && value !== null) {
            const field = overlay.querySelector('[data-modal-value]');
            if (field) field.value = value;
        }

        overlay.classList.add('is-open');
        document.body.classList.add('modal-open');

        const cancel = overlay.querySelector('[data-modal-close]');
        if (cancel) cancel.focus();
    }

    function closeModal(id) {
        const overlay = id ? document.getElementById(id)
                           : document.querySelector('.modal-overlay.is-open');
        if (!overlay) return;

        overlay.classList.remove('is-open');
        document.body.classList.remove('modal-open');
    }

    // 오버레이 바깥 클릭 / 취소 버튼 (이벤트 위임)
    document.addEventListener('click', function (event) {
        // 오버레이 자체를 클릭했을 때만 (모달 안쪽 클릭은 무시)
        if (event.target.matches('[data-modal]')) {
            closeModal(event.target.id);
            return;
        }

        const closer = event.target.closest('[data-modal-close]');
        if (closer) {
            const overlay = closer.closest('[data-modal]');
            if (overlay) closeModal(overlay.id);
        }
    });

    // ESC 로 닫기
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') closeModal();
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-modal]').forEach(normalizeButtons);
    });

    // 전역 노출 (JSP 의 onclick 에서 호출)
    window.openModal = openModal;
    window.closeModal = closeModal;
})();


/* ============================================================
   공통 슬라이더 엔진
   - 배너(banner.jsp)와 게시글 갤러리(detail.jsp)가 이 로직을 공유
   - 각자 다른 CSS 변수(--banner-index / --gallery-index), 다른 셀렉터를
     설정값(config)으로 넘겨서 자기 마크업에 맞게 동작
   - 실제 이동은 track에 CSS 변수를 세팅하면 각 CSS의 transform이 처리
   ============================================================ */
function initSlider(container, config) {
    const track  = container.querySelector(config.trackSelector);
    const slides = container.querySelectorAll(config.slideSelector);
    if (!track || slides.length === 0) return null;
 
    let index = 0;
 
    function move(next) {
        // 끝에서 다음 → 처음으로, 처음에서 이전 → 끝으로 (순환)
        index = (next + slides.length) % slides.length;
 
        track.style.setProperty(config.cssVar, index);
 
        if (config.onMove) config.onMove(index, slides.length);
    }
 
    const prev = container.querySelector(config.prevSelector);
    const next = container.querySelector(config.nextSelector);
 
    if (prev) prev.addEventListener('click', function () { move(index - 1); });
    if (next) next.addEventListener('click', function () { move(index + 1); });
 
    move(0);
 
    return { move: move };
}
 
 
/* ============================================================
   배너 슬라이더 (banner.jsp)
   - 좌우 꺽쇠 / 인디케이터 클릭으로 슬라이드 이동
   - 한 페이지에 배너가 여러 개 있어도 각각 독립 동작
   ============================================================ */
(function () {
    'use strict';
 
    function initBanner(banner) {
        const dots = banner.querySelectorAll('[data-banner-dot]');
 
        const slider = initSlider(banner, {
            trackSelector: '[data-banner-track]',
            slideSelector: '.banner-slide',
            cssVar: '--banner-index',
            prevSelector: '[data-banner-prev]',
            nextSelector: '[data-banner-next]',
            onMove: function (index) {
                dots.forEach(function (dot, i) {
                    dot.classList.toggle('is-active', i === index);
                });
            }
        });
        if (!slider) return;
 
        dots.forEach(function (dot) {
            dot.addEventListener('click', function () {
                slider.move(Number(dot.dataset.bannerDot));
            });
        });
    }
 
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-banner]').forEach(initBanner);
    });
})();
 
 
/* ============================================================
   게시글 이미지 갤러리 (detail.jsp)
   - 화살표(prev/next) 클릭 + 카운터(1 / N) 갱신
   ============================================================ */
(function () {
    'use strict';
 
    function initGallery(gallery) {
        const counter = gallery.querySelector('[data-gallery-counter]');
 
        initSlider(gallery, {
            trackSelector: '[data-gallery-track]',
            slideSelector: '.post-gallery-slide',
            cssVar: '--gallery-index',
            prevSelector: '[data-gallery-prev]',
            nextSelector: '[data-gallery-next]',
            onMove: function (index, total) {
                if (counter) counter.textContent = (index + 1) + ' / ' + total;
            }
        });
    }
 
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-gallery]').forEach(initGallery);
    });
})();