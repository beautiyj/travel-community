/* =========================================================
   payment-bridge.js — 결제 팝업 → 부모 창 이동 브릿지
   사용 페이지: reservation/paymentBridge.jsp
   서버 주입 전역값: bridgeTarget (이동할 목적지 URL)
   ========================================================= */

(function () {
    if (window.opener && !window.opener.closed) {
        // 팝업으로 열린 경우: 부모 창을 이동시키고 팝업 닫기
        window.opener.location.href = bridgeTarget;
        window.close();
    } else {
        // 팝업이 아닌 경우(모바일/직접 이동 등): 현재 창에서 이동
        window.location.href = bridgeTarget;
    }
})();
