/* =========================================================
   reservation-test.js — 예약/결제 테스트 허브 (개발 전용)
   사용 페이지: reservation/test.jsp
   ========================================================= */

/* 타입별 예약 폼 이동. 장소 타입(tour/food/stay)은 서버가 PLACE 조회로 판단한다.
   타입마다 placeId 입력칸을 따로 둔다 — 같은 placeId를 공유하면 회원+장소+날짜
   중복체크(findActiveBySlot)가 타입 구분 없이 걸려 "이미 예약된 날짜"로 막히기 때문.
   freeTest만 PLACE.place_type에 없는 테스트 전용 오버라이드라 파라미터로 넘긴다. */
function goForm(inputId, freeTest) {
    var placeId = document.getElementById(inputId).value || 1;
    var url = '/reservations/new?placeId=' + placeId;
    if (freeTest) url += '&freeTest=true';
    location.href = url;
}

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
