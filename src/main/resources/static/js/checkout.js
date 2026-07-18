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

document.getElementById("payBtn").addEventListener("click", function () {
    if (currentMethod === "kakao") {
        payKakao();
    } else {
        // TODO(토스 담당): 카카오와 동일 패턴으로 교체
        // (팝업 열기 -> /payments/toss/ready/{id} POST -> redirectUrl 이동)
        alert("토스페이 결제는 준비 중입니다. 카카오페이를 이용해 주세요.");
    }
});
