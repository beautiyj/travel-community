/* =========================================================
   payment-success.js — 결제 완료 페이지 스크립트
   사용 페이지: reservation/success.jsp
   서버 주입 전역값: paymentId
   ========================================================= */

/* 결제 취소(환불) — smallButton 컴포넌트의 onclick에서 호출 */
function cancelPayment() {
    if (!confirm("정말 결제를 취소하시겠습니까?")) return;
    fetch("/payments/" + paymentId + "/cancel", { method: "POST" })
        .then(function (res) {
            if (!res.ok) throw new Error();
            alert("결제가 취소되었습니다.");
            location.href = "/payments/failed?message=" + encodeURIComponent("결제가 취소(환불)되었습니다.");
        })
        .catch(function () { alert("취소에 실패했습니다."); });
}
