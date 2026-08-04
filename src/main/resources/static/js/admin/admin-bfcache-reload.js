/**
 * 승인·반려 전 화면이 BFCache로 복원되면 오래된 상태가 잠깐 보일 수 있다.
 * 캐시될 때 관리자 화면을 숨기고, 복원될 때 최신 서버 상태를 다시 요청한다.
 */
(function () {
    window.addEventListener('pagehide', function (event) {
        // BFCache에 저장되는 경우에만 숨김 클래스를 붙여 일반 페이지 이동의 표시에는 관여하지 않는다.
        if (event.persisted) {
            document.documentElement.classList.add('admin-bfcache-refreshing');
        }
    });

    window.addEventListener('pageshow', function (event) {
        // 복원된 DOM의 상태를 신뢰하지 않고 서버에서 최신 신청 상태와 권한 검사를 다시 받는다.
        if (event.persisted) {
            window.location.reload();
        }
    });
}());
