(function () {
    var form = document.querySelector('.withdraw-card form');
    if (!form) return;

    form.addEventListener('submit', function (event) {
        if (!confirm('정말 회원 탈퇴하시겠습니까?')) {
            event.preventDefault();
        }
    });
}());
