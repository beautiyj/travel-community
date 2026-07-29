<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>무통장 입금 안내</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/reservation.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="page-wrap">

    <a href="/payments/checkout/${reservation.reservationId}" class="back-link">&lsaquo; 결제수단 다시 선택</a>
    <h1 class="page-title">무통장 입금 안내</h1>

    <div class="booking-grid">
        <div>
            <h2 class="section-title">아래 계좌로 입금해 주세요</h2>

            <div class="summary-card">
                <div class="summary-row"><span class="label">은행</span><span class="value">가상은행 (테스트)</span></div>
                <div class="summary-row"><span class="label">계좌번호</span><span class="value">000-0000-000000</span></div>
                <div class="summary-row"><span class="label">입금자명</span><span class="value">${reservation.visitorName}</span></div>
                <div class="summary-row"><span class="label">주문번호</span><span class="value">${reservation.orderId}</span></div>
                <div class="summary-row summary-divider summary-total" style="padding-top:14px;">
                    <span>입금 금액</span>
                    <span><fmt:formatNumber value="${amount}" pattern="#,###"/>원</span>
                </div>
            </div>

            <div style="margin-top:20px;">
                <label for="depositorName" style="display:block; margin-bottom:6px; font-weight:600;">입금자명 확인</label>
                <input type="text" id="depositorName" placeholder="입금하신 분의 성함"
                       autocomplete="off" style="width:100%; padding:10px; box-sizing:border-box;">
                <p style="margin-top:6px; font-size:12px; color:#888;">
                    안내: 예약자명(<b>${reservation.visitorName}</b>)으로 입금 후 동일하게 입력하면 결제가 완료됩니다. (가상)
                </p>
            </div>

            <div class="btn-row">
                <button type="button" class="btn btn-outline"
                        onclick="location.href='/payments/checkout/${reservation.reservationId}'">뒤로</button>
                <button type="button" class="btn btn-primary" id="confirmBtn">입금 확인</button>
            </div>
        </div>
    </div>
</div>

<script>
    var reservationId = "${reservation.reservationId}";
    document.getElementById("confirmBtn").addEventListener("click", function () {
        var name = (document.getElementById("depositorName").value || "").trim();
        if (!name) { alert("입금자명을 입력해 주세요."); return; }
        var btn = this;
        btn.disabled = true;
        fetch("/payments/bank/confirm/" + reservationId, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "depositorName=" + encodeURIComponent(name)
        })
        .then(function (res) {
            return res.json().then(function (data) {
                if (!res.ok) throw new Error(data.message || "입금 확인에 실패했습니다.");
                return data;
            });
        })
        .then(function (data) { window.location.href = data.target; })
        .catch(function (err) { btn.disabled = false; alert(err.message); });
    });
</script>
</body>
</html>
