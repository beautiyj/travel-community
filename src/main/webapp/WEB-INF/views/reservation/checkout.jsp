<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제하기</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/reservation.css">
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

<%-- 서버값 주입 후 외부 JS 로드 --%>
<script>
    var reservationId = "${reservation.reservationId}";
    var tossClientKey = "${tossClientKey}";
</script>
<script src="https://js.tosspayments.com/v1/payment"></script>
<script src="/js/checkout.js"></script>
</body>
</html>
