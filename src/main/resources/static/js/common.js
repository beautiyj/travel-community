function toggleWishLocal(buttonElement) {
    // 현재 버튼의 활성화 상태 유무 (기본값 wish-off)
    const isActive = buttonElement.getAttribute('data-active') === 'true';
    const imgElement = buttonElement.querySelector('.wish-icon');
    
    // 프로젝트 Context Path 자동 계산
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/', 2));

    // 클릭할 때마다 상태 반전시키고 이미지 갈아끼우기
    if (isActive) {
        // ON -> OFF 상태로 전환
        imgElement.src = `${contextPath}/images/icons/wish-off.png`;
        buttonElement.setAttribute('data-active', 'false');
    } else {
        // OFF -> ON 상태로 전환
        imgElement.src = `${contextPath}/images/icons/wish-on.png`;
        buttonElement.setAttribute('data-active', 'true');
    }
}

/* 셀렉터블 컴포넌트 인터랙션 핸들러 */
document.addEventListener("DOMContentLoaded", function () {
    
    document.addEventListener("click", function (event) {
        
        // 셀렉터블 버튼 감지 (.btn-selectable)
        const button = event.target.closest(".btn-selectable");
        if (button) {
            button.classList.toggle("is-active");
        }

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

/* ============================================================
   공통 모달 (confirmModal.jsp) 열기/닫기
   - 오버레이 클릭 / 취소 버튼([data-modal-close]) / ESC 로 닫힘
   - openModal(id, value) : value 를 주면 hidden 필드에 주입
     (목록에서 행마다 대상이 다를 때 모달 1개를 재사용)
   ============================================================ */
(function () {
    'use strict';

    function openModal(id, value) {
        const overlay = document.getElementById(id);
        if (!overlay) return;

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

    // 전역 노출 (JSP 의 onclick 에서 호출)
    window.openModal = openModal;
    window.closeModal = closeModal;
})();


/* ============================================================
   배너 슬라이더 (banner.jsp)
   - 좌우 꺽쇠 / 인디케이터 클릭으로 슬라이드 이동
   - 한 페이지에 배너가 여러 개 있어도 각각 독립 동작
   - 실제 이동은 CSS 변수 --banner-index 를 바꾸면 .banner-track 이 transform 으로 처리
   ============================================================ */
(function () {
    'use strict';

    function initBanner(banner) {
        const track  = banner.querySelector('[data-banner-track]');
        const slides = banner.querySelectorAll('.banner-slide');
        const dots   = banner.querySelectorAll('[data-banner-dot]');
        if (!track || slides.length === 0) return;

        let index = 0;

        function move(next) {
            // 끝에서 다음 → 처음으로, 처음에서 이전 → 끝으로 (순환)
            index = (next + slides.length) % slides.length;

            track.style.setProperty('--banner-index', index);

            dots.forEach(function (dot, i) {
                dot.classList.toggle('is-active', i === index);
            });
        }

        const prev = banner.querySelector('[data-banner-prev]');
        const next = banner.querySelector('[data-banner-next]');

        if (prev) prev.addEventListener('click', function () { move(index - 1); });
        if (next) next.addEventListener('click', function () { move(index + 1); });

        dots.forEach(function (dot) {
            dot.addEventListener('click', function () {
                move(Number(dot.dataset.bannerDot));
            });
        });

        move(0);
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-banner]').forEach(initBanner);
    });
})();