/* =========================================================
   payment-success.js — 결제 완료 페이지 스크립트
   사용 페이지: reservation/success.jsp
   서버 주입 전역값: paymentId, reservationId
   ========================================================= */

/** cancel_reason 컬럼 길이(VARCHAR(100))에 맞춘 제한 */
var REASON_MAX_LENGTH = 100;

/* 예약 취소 요청 — 즉시 환불이 아니라 관리자 검토 대기 상태로 전환된다.
   실제 환불은 관리자가 승인할 때 실행된다. */
function requestCancel() {
    var reason = prompt("취소 사유를 입력해 주세요.");
    if (reason === null) return;                 // 사용자가 입력창을 닫음

    reason = reason.trim();
    if (!reason) {
        alert("취소 사유를 입력해 주세요.");
        return;
    }
    if (reason.length > REASON_MAX_LENGTH) {
        alert("취소 사유는 " + REASON_MAX_LENGTH + "자 이내로 입력해 주세요.");
        return;
    }

    fetch("/reservations/" + reservationId + "/cancel-request", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "reason=" + encodeURIComponent(reason)
    })
        .then(function (res) {
            if (!res.ok) throw new Error();
            alert("취소 요청이 접수되었습니다.\n관리자 확인 후 환불됩니다.");
            location.href = "/mypage/reservations";
        })
        .catch(function () { alert("취소 요청에 실패했습니다."); });
}
