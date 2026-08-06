<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- tour(관광지)·food(맛집) = 예약금(만나서결제) --%>
<c:set var="payOnSite" value="${reservation.placeType eq 'tour' or reservation.placeType eq 'food'}" />
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제하기</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/reservation/reservation.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="page-wrap">

    <a href="javascript:history.back()" class="back-link">&lsaquo; 예약 정보로</a>
    <h1 class="page-title">결제하기</h1>

    <div class="booking-grid">

        <!-- ─── 왼쪽: 결제 수단 ─── -->
        <div>
            <h2 class="section-title">결제 수단</h2>
            <div class="method-grid">
                <button type="button" class="method-btn active" data-method="kakao">
                    <img src="/images/reservation/kakaopay-small.png" alt="카카오페이" class="method-icon">
                </button>
                <button type="button" class="method-btn" data-method="toss">
                    <img src="/images/reservation/toss-pay.svg" alt="토스페이" class="method-icon">
                </button>
                <button type="button" class="method-btn" data-method="bank">무통장입금</button>
                <button type="button" class="method-btn" data-method="vcard">카드결제</button>
            </div>

            <!-- 선택한 결제 수단 안내 패널 -->
            <div class="simple-pay-panel">
                <div class="pay-logo kakao has-icon" id="payLogo">
                    <img src="/images/reservation/kakaopay-small.png" alt="카카오페이로 결제합니다">
                </div>
                <p class="pay-name" id="payName">카카오페이로 결제합니다</p>
                <p class="pay-desc" id="payDesc">결제 버튼 클릭 시 결제창으로 이동합니다</p>
            </div>

            <!-- 가상카드 번호 입력 (가상카드 선택 시 노출) -->
            <div class="vcard-box" id="vcardBox" style="display:none; margin-top:12px;">
                <label for="cardNumber" style="display:block; margin-bottom:6px; font-weight:600;">카드번호</label>
                <input type="text" id="cardNumber" placeholder="숫자 16자리" maxlength="19"
                       autocomplete="off" inputmode="numeric"
                       style="width:100%; padding:10px; box-sizing:border-box;">
                <p style="margin-top:6px; font-size:12px; color:#888;">테스트: <b>42</b> 뒤에 <b>1</b>로 채우기 (예: 4211111111111111) 입력 시 결제 성공</p>
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
            <div class="summary-row"><span class="label">장소</span><span class="value">${fn:escapeXml(placeName)}</span></div>
            <c:choose>
                <c:when test="${not empty reservation.checkOutDate}">
                    <div class="summary-row"><span class="label">기간</span>
                        <span class="value">${reservation.visitDate} ~ ${reservation.checkOutDate}</span></div>
                </c:when>
                <c:otherwise>
                    <div class="summary-row"><span class="label">날짜</span><span class="value">${reservation.visitDate}</span></div>
                </c:otherwise>
            </c:choose>
            <div class="summary-row"><span class="label">인원</span><span class="value">${reservation.headcount}명</span></div>
            <c:if test="${payOnSite}">
                <div class="summary-row"><span class="label">결제 방식</span><span class="value">예약금 선결제 · 나머지 현장 결제</span></div>
            </c:if>
            <div class="summary-row summary-divider summary-total" style="padding-top:14px;">
                <span>${payOnSite ? '예약금' : '최종 금액'}</span>
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
<script src="/js/reservation/checkout.js"></script>
</body>
</html>
