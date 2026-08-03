/* main/index.jsp 전용 스크립트.
   슬라이더 이동 자체는 common.js의 공용 엔진(initSlider)을 그대로 재사용하고,
   여기서는 이 페이지에서만 필요한 배선(탭 필터)만 담당한다. */

/* ── 인기 여행지 캐러셀 (common.js의 initSlider 재사용) ── */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var carousel = document.querySelector('[data-destination]');
        if (!carousel) return;

        initSlider(carousel, {
            trackSelector: '[data-destination-track]',
            slideSelector: '.destination-slide',
            cssVar: '--destination-index',
            prevSelector: '[data-destination-prev]',
            nextSelector: '[data-destination-next]'
        });
    });
})();

/* ── 숙박/맛집/관광지 탭 필터 ──
   common.js의 전역 셀렉터블 핸들러는 클릭한 버튼의 is-active를 단순 toggle만 하고
   같은 그룹 내 다른 버튼은 건드리지 않는다(선택 카드처럼 그룹 배타 선택 로직이 없음).
   탭은 항상 하나만 선택돼야 하므로, 여기서 그룹 내 상태를 명시적으로 확정한다. */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var tabGroup = document.querySelector('[data-main-tabs]');
        var cards = document.querySelectorAll('[data-main-card]');
        if (!tabGroup || cards.length === 0) return;

        function applyFilter(category) {
            cards.forEach(function (card) {
                var match = card.dataset.category === category;
                card.style.display = match ? '' : 'none';
            });
        }

        tabGroup.addEventListener('click', function (event) {
            var btn = event.target.closest('.btn-selectable');
            if (!btn) return;

            // common.js의 document 레벨 전역 핸들러가 이 클릭에 또 반응해서
            // is-active를 toggle로 되돌려버리는 것을 막는다 (버블링 차단)
            event.stopPropagation();

            tabGroup.querySelectorAll('.btn-selectable').forEach(function (b) {
                b.classList.remove('is-active');
            });
            btn.classList.add('is-active');

            applyFilter(btn.dataset.category);
        });

        var initialActive = tabGroup.querySelector('.btn-selectable.is-active');
        if (initialActive) applyFilter(initialActive.dataset.category);
    });
})();
