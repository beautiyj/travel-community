/* =========================================================
   business-reservations.js — 예약 관리 화면 스크립트
   사용 페이지: business/reservations.jsp
   예약 거절 모달(rejectReasonModal.jsp)의 select→"기타" 직접입력 토글과
   최종 사유 값을 hidden input에 조립하는 로직을 담당한다.
   (reservation/success.jsp의 취소요청 모달과 동일한 UX 패턴)
   ========================================================= */

/** RESERVATION.reject_reason 컬럼 길이(VARCHAR(100))에 맞춘 제한 */
var REJECT_REASON_MAX_LENGTH = 100;

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-reject-form]').forEach(function (form) {
        var select = form.querySelector('[data-reject-select]');
        var etc = form.querySelector('[data-reject-etc]');
        var hidden = form.querySelector('[data-reject-reason]');
        if (!select || !etc || !hidden) return;

        // "기타" 선택 시에만 직접입력 textarea 노출
        select.addEventListener('change', function () {
            etc.style.display = (select.value === '기타') ? 'block' : 'none';
        });

        // 제출 직전, select/textarea 중 실제 사유로 쓸 값을 hidden(name=reason)에 조립
        form.addEventListener('submit', function (event) {
            var reason = (select.value === '기타') ? etc.value.trim() : select.value;

            if (!reason) {
                event.preventDefault();
                alert('거절 사유를 선택해 주세요.');
                return;
            }
            if (reason.length > REJECT_REASON_MAX_LENGTH) {
                event.preventDefault();
                alert('거절 사유는 ' + REJECT_REASON_MAX_LENGTH + '자 이내로 입력해 주세요.');
                return;
            }

            hidden.value = reason;
        });
    });
});
