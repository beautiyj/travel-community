// community/categoryTabFix.js — list.jsp 전용
// common.js의 전역 .btn-selectable 클릭 핸들러는 그룹 개념 없이 무조건 toggle만 하므로,
// 카테고리 탭 클릭 시 기존 active 버튼은 그대로 두고 새 버튼에도 is-active가 추가되어
// 2개가 동시에 active인 상태로 페이지 이동이 일어남(뒤로가기 시 bfcache에 그 상태가 그대로 캐싱되어 중복 체크로 보임).
// 탭은 항상 location.href로 즉시 페이지 이동하고 active 상태는 서버가 매 요청마다 정확히 렌더링하므로,
// 여기서는 클릭이 common.js의 document 핸들러까지 버블링되지 않도록만 막는다.
document.addEventListener('DOMContentLoaded', function () {
    const tabs = document.querySelector('.tabs');
    if (!tabs) return;

    tabs.addEventListener('click', function (event) {
        if (event.target.closest('.btn-selectable')) {
            event.stopPropagation();
        }
    });
});
