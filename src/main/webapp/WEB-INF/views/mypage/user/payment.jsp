<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제완료 목록</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/components/tagButton.css">
    <link rel="stylesheet" href="/css/mypage/user.css">
</head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">결제완료 목록</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp"><jsp:param name="accountType" value="USER"/><jsp:param name="active" value="reservation"/></jsp:include>
        <section class="mypage-content mypage-content--wide">
            <c:choose><c:when test="${empty paymentList}"><div class="mypage-empty-state"><strong>결제 완료된 예약이 없습니다</strong></div></c:when>
            <c:otherwise><div class="reservation-list"><c:forEach var="payment" items="${paymentList}"><article class="reservation-item"><div class="reservation-item__body"><h3>장소 #<c:out value="${payment.placeId}" /></h3><p>방문일 <c:out value="${payment.visitDate}" /></p><jsp:include page="/WEB-INF/views/mypage/common/reservationStatusBadge.jsp"><jsp:param name="status" value="PAID"/></jsp:include></div></article></c:forEach></div></c:otherwise></c:choose>
        </section>
    </div>
</main>
</body>
</html>
