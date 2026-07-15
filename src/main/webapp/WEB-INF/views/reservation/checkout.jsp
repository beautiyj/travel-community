<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제하기</title>
    <link rel="stylesheet" href="/resources/css/booking.css">
</head>
<body>

<div class="page-wrap">

    <a href="javascript:history.back()" class="back-link">&lsaquo; 예약 정보로</a>
    <h1 class="page-title">결제하기</h1>

    <div class="booking-grid">

        <!-- ─── 왼쪽: 결제 수단 ─── -->
        <div>
            <h2 class="section-title">결제 수단</h2>
            <div class="method-grid">
                <button type="button" class="method-btn active" data-method="kakao">카카오페이</button>
                <button type="button" class="method-btn" data-method="toss">토스페이</button>
            </div>

            <!-- 선택한 결제 수단 안내 패널 -->
            <div class="simple-pay-panel">
                <div class="pay-logo kakao" id="payLogo">K</div>
                <p class="pay-name" id="payName">카카오페이로 결제합니다</p>
                <p class="pay-desc">결제 버튼 클릭 시 결제창으로 이동합니다</p>
            </div>

            <label class="agree-box">
                <input type="checkbox" id="agree">
                <span class="agree-text">구매조건 및 <span class="link">개인정보 처리방침</span>에 동의하며 결제에 동의합니다</span>
            </label>

            <div class="btn-row">
                <button type="button" class="btn btn-outline" onclick="history.back()">취소</button>
                <button type="button" class="btn btn-primary" id="payBtn" disabled>
                    <fmt:formatNumber value="${amount}" pattern="#,###"/>원 결제
                </button>
            </div>
            <p id="waiting">팝업에서 결제를 진행해 주세요...</p>
        </div>

        <!-- ─── 오른쪽: 결제 정보 ─── -->
        <div class="summary-card">
            <h3>결제 정보</h3>
            <div class="summary-row"><span class="label">예약자</span><span class="value">${reservation.visitorName}</span></div>
            <div class="summary-row"><span class="label">연락처</span><span class="value">${reservation.phone}</span></div>
            <div class="summary-row"><span class="label">장소</span><span class="value">장소 #${reservation.placeId}</span></div>
            <div class="summary-row"><span class="label">날짜</span><span class="value">${reservation.visitDate}</span></div>
            <div class="summary-row"><span class="label">인원</span><span class="value">${reservation.headcount}명</span></div>
            <div class="summary-row summary-divider summary-total" style="padding-top:14px;">
                <span>최종 금액</span>
                <span><fmt:formatNumber value="${amount}" pattern="#,###"/>원</span>
            </div>
        </div>

    </div>
</div>

<script>
    var reservationId = "${reservation.reservationId}";
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
</script>
</body>
</html>
