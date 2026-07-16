<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>예약 내역</title>
</head>
<body>

<h2>예약 내역</h2>

<c:if test="${empty reservationList}">
    예약 내역이 없습니다.
</c:if>

<c:if test="${not empty reservationList}">
<table border="1">
    <tr>
        <th>예약 번호</th>
        <th>장소 번호</th>
        <th>방문자명</th>
        <th>전화번호</th>
        <th>방문일</th>
        <th>인원</th>
        <th>상태</th>
        <th>예약일</th>
    </tr>

    <c:forEach var="reservation" items="${reservationList}">
    <tr>
        <td>${reservation.reservationId}</td>
        <td>${reservation.placeId}</td>
        <td>${reservation.visitorName}</td>
        <td>${reservation.phone}</td>
        <td>${reservation.visitDate}</td>
        <td>${reservation.headcount}</td>
        <td>${reservation.status}</td>
        <td>${reservation.createdAt}</td>
    </tr>
    </c:forEach>
</table>
</c:if>

<br>

<a href="/mypage/info">마이페이지로 돌아가기</a>

</body>
</html>