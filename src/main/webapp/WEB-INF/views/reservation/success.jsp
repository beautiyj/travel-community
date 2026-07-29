<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제 완료</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/reservation.css">
    <link rel="stylesheet" href="/css/components/smallButton.css">
</head>
<body>
<div class="result-card">
    <div class="result-icon ok">&#10003;</div>
    <h2>결제가 완료되었습니다</h2>

    <div class="result-info">
        <div><span>예약번호</span><span>${payment.reservationId}</span></div>
        <div><span>주문번호</span><span>${payment.orderId}</span></div>
        <div><span>결제수단</span><span>${method}</span></div>
        <div><span>결제금액</span>
            <span><fmt:formatNumber value="${payment.amount}" pattern="#,###"/>원</span></div>
    </div>

    <div class="result-actions">
        <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
            <jsp:param name="text" value="내 예약 확인하기" />
            <jsp:param name="width" value="100%" />
            <jsp:param name="onclick" value="location.href='/mypage/reservations'" />
        </jsp:include>
        <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
            <jsp:param name="text" value="예약 취소 요청" />
            <jsp:param name="width" value="100%" />
            <jsp:param name="theme" value="danger" />
            <jsp:param name="onclick" value="requestCancel()" />
        </jsp:include>
    </div>
</div>

<%-- 서버값 주입 후 외부 JS 로드 --%>
<script>
    var paymentId = "${payment.paymentId}";
    var reservationId = "${payment.reservationId}";
</script>
<script src="/js/reservation/payment-success.js"></script>
</body>
</html>
