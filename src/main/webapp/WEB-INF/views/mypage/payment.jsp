<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="결제완료 목록" /></jsp:include></head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/components/sidebar.jsp"><jsp:param name="active" value="reservation" /></jsp:include>
        <section class="mypage-content mypage-content--wide" aria-labelledby="payment-title">
            <div class="mypage-content__header"><h2 id="payment-title">결제완료 목록</h2></div>
            <c:choose><c:when test="${empty paymentList}"><div class="mypage-empty-state"><strong>결제 완료된 예약이 없습니다</strong></div></c:when>
            <c:otherwise><div class="reservation-list"><c:forEach var="payment" items="${paymentList}"><article class="reservation-item"><div class="reservation-item__body"><h3>장소 #<c:out value="${payment.placeId}" /></h3><p>방문일 <c:out value="${payment.visitDate}" /></p><span class="mypage-status mypage-status--paid">결제완료</span></div></article></c:forEach></div></c:otherwise></c:choose>
        </section>
    </div>
</main>
</body>
</html>
