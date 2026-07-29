/* =========================================================
   payment-success.js — 결제 완료 페이지 스크립트
   사용 페이지: reservation/success.jsp
   서버 주입 전역값: paymentId, reservationId
   취소 요청은 alert/prompt 대신 공용 모달(cancelModal)에서 사유를 select 로 받는다.
   ========================================================= */

/** cancel_reason 컬럼 길이(VARCHAR(100))에 맞춘 제한 */
var REASON_MAX_LENGTH = 100;

/* "예약 취소 요청" 버튼 → 사유 선택 모달을 연다. (기존 prompt() 대체)
   공용 openModal(common.js)로 열고, 실제 전송은 모달의 "취소 요청" 버튼이 한다. */
function requestCancel() {
    var select = document.getElementById('cancelReason');
    var etc = document.getElementById('cancelReasonEtc');
    if (select) select.value = '';   // 열 때마다 이전 선택 초기화
    if (etc) { etc.value = ''; etc.style.display = 'none'; }
    openModal('cancelModal');
}

/* 모달의 "취소 요청" 버튼 → 선택한 사유로 취소 요청 전송.
   즉시 환불이 아니라 관리자 검토 대기 상태로 전환된다(실제 환불은 관리자 승인 시). */
function submitCancel() {
    var select = document.getElementById('cancelReason');
    var etc = document.getElementById('cancelReasonEtc');
    // "기타" 선택 시엔 select 값이 아니라 직접입력(textarea) 내용을 사유로 쓴다
    var reason = (select && select.value === '기타')
        ? (etc ? etc.value.trim() : '')
        : (select ? select.value.trim() : '');

    if (!reason) {
        alert('취소 사유를 선택해 주세요.');
        return;
    }
    if (reason.length > REASON_MAX_LENGTH) {
        alert('취소 사유는 ' + REASON_MAX_LENGTH + '자 이내로 입력해 주세요.');
        return;
    }

    fetch('/reservations/' + reservationId + '/cancel-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'reason=' + encodeURIComponent(reason)
    })
        .then(function (res) {
            if (!res.ok) throw new Error();
            closeModal('cancelModal');
            alert('취소 요청이 접수되었습니다.\n관리자 확인 후 환불됩니다.');
            location.href = '/mypage/reservations';
        })
        .catch(function () { alert('취소 요청에 실패했습니다.'); });
}

/* 모달 "취소 요청" 버튼 바인딩 (onclick 대신 addEventListener — 공용 normalizeButtons와 충돌 방지) */
document.addEventListener('DOMContentLoaded', function () {
    var confirmBtn = document.getElementById('cancelConfirmBtn');
    if (confirmBtn) confirmBtn.addEventListener('click', submitCancel);

    // "기타" 선택 시에만 직접입력 textarea 노출
    var select = document.getElementById('cancelReason');
    var etc = document.getElementById('cancelReasonEtc');
    if (select && etc) {
        select.addEventListener('change', function () {
            etc.style.display = (select.value === '기타') ? 'block' : 'none';
        });
    }
});
