document.addEventListener('DOMContentLoaded', function () {
    const sortSelect = document.getElementById('sortSelect');

    if (sortSelect) {
        sortSelect.addEventListener('change', function () {
            const sortValue = this.value;
            const urlParams = new URLSearchParams(window.location.search);

            // 정렬값 변경
            urlParams.set('sort', sortValue);

            // 정렬 변경 시 1페이지로 리셋
            urlParams.set('page', '1');

            // 페이지 이동
            window.location.href = window.location.pathname + '?' + urlParams.toString();
        });
    }
});