/* =========================================================
   checkout.js — 결제하기 페이지 스크립트
   사용 페이지: reservation/checkout.jsp
   서버 주입 전역값: reservationId (JSP에서 선언 후 이 파일 로드)
   ========================================================= */

var currentMethod = "kakao";
var popupOptions = "width=500,height=750,left=" + ((screen.width - 500) / 2)
                 + ",top=" + ((screen.height - 750) / 2);

/* ── 결제 수단 선택 ── */
var payInfo = {
    kakao: { cls: "kakao", initial: "K", name: "카카오페이로 결제합니다" },
    toss:  { cls: "toss",  initial: "T", name: "토스페이로 결제합니다" }
};

document.querySelectorAll(".method-btn").forEach(function (btn) {
    btn.addEventListener("click", function () {
        document.querySelectorAll(".method-btn").forEach(function (b) { b.classList.remove("active"); });
        btn.classList.add("active");
        currentMethod = btn.dataset.method;

        var info = payInfo[currentMethod];
        var logo = document.getElementById("payLogo");
        logo.className = "pay-logo " + info.cls;
        logo.textContent = info.initial;
        document.getElementById("payName").textContent = info.name;
    });
});

/* ── 동의해야 결제 버튼 활성화 ── */
document.getElementById("agree").addEventListener("change", function () {
    document.getElementById("payBtn").disabled = !this.checked;
});

/* ── 결제 진행 ── */
function lockButtons(lock) {
    document.getElementById("payBtn").disabled = lock || !document.getElementById("agree").checked;
    document.getElementById("waiting").style.display = lock ? "block" : "none";
}

function watchPopup(popup) {
    // 사용자가 팝업을 결제 없이 그냥 닫으면 버튼 다시 활성화
    var timer = setInterval(function () {
        if (!popup || popup.closed) {
            clearInterval(timer);
            lockButtons(false);
        }
    }, 500);
}

// 카카오: 팝업을 먼저 열고(차단 방지), ready 응답의 URL로 이동시키기
function payKakao() {
    var popup = window.open("about:blank", "kakaoPay", popupOptions);
    if (!popup) { alert("팝업이 차단되었습니다. 팝업을 허용해 주세요."); return; }
    lockButtons(true);
    watchPopup(popup);

    fetch("/payments/kakao/ready/" + reservationId, { method: "POST" })
        .then(function (res) {
            if (!res.ok) throw new Error("ready 실패");
            return res.json();
        })
        .then(function (data) {
            popup.location.href = data.redirectUrl;
        })
        .catch(function () {
            popup.close();
            lockButtons(false);
            alert("카카오페이 결제 준비에 실패했습니다.");
        });
}

/* ── 토스 SDK 초기화 ── */
var toss = (typeof TossPayments !== "undefined" && tossClientKey)
    ? TossPayments(tossClientKey)
    : null;

// 토스: SDK 가 결제창 이동까지 직접 처리 (팝업 불필요, requestPayment 가 알아서 리다이렉트)
function payToss() {
    if (!toss) {
        alert("결제 모듈을 불러오지 못했습니다. 새로고침 후 다시 시도해 주세요.");
        return;
    }
    lockButtons(true);

    fetch("/payments/toss/ready/" + reservationId, { method: "POST" })
        .then(function (res) {
            if (!res.ok) throw new Error("ready 실패");
            return res.json();
        })
        .then(function (data) {
            return toss.requestPayment("카드", {
                amount: data.amount,
                orderId: data.orderId,
                orderName: "여행 예약 #" + reservationId,
                successUrl: window.location.origin + "/payments/toss/success",
                failUrl: window.location.origin + "/payments/toss/fail"
            });
            // 성공 시 브라우저가 successUrl 로 이동해버려서 이 아래 then 은 보통 안 탐
        })
        .catch(function (err) {
            lockButtons(false);
            // 사용자가 결제창을 그냥 닫은 경우 err.code === "USER_CANCEL" 로 들어옴
            if (err && err.code !== "USER_CANCEL") {
                alert("토스페이 결제 준비에 실패했습니다.");
            }
        });
}

document.getElementById("payBtn").addEventListener("click", function () {
    if (currentMethod === "kakao") {
        payKakao();
    } else {
        payToss();
    }
});
