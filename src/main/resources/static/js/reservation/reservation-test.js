/* =========================================================
   reservation-test.js — 예약/결제 테스트 허브 (개발 전용)
   사용 페이지: reservation/test.jsp
   ========================================================= */

function goCheckout() {
    var id = document.getElementById('reservationId').value;
    if (!id) { alert('reservationId를 입력하세요.'); return; }
    location.href = '/payments/checkout/' + id;
}

function goComplete() {
    var id = document.getElementById('completePaymentId').value;
    if (!id) { alert('paymentId를 입력하세요.'); return; }
    location.href = '/payments/complete/' + id;
}

function cancelPayment() {
    var id = document.getElementById('cancelPaymentId').value;
    if (!id) { alert('paymentId를 입력하세요.'); return; }
    if (!confirm(id + '번 결제를 취소할까요?')) return;
    fetch('/payments/' + id + '/cancel', { method: 'POST' })
        .then(function (res) {
            if (!res.ok) throw new Error();
            alert('취소되었습니다.');
            location.reload();
        })
        .catch(function () { alert('취소 실패 (이미 취소됐거나 없는 결제)'); });
}
