<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>예약 내역</title>
</head>

<body>

<h2>예약 내역</h2>

<!-- 예약 내역이 없는 경우 -->
<c:if test="${empty reservationList}">
    <p>예약 내역이 없습니다.</p>
</c:if>


<!-- 예약 내역이 있는 경우 -->
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
            <th>관리</th>
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

                <td>
                    <!-- 예약중 상태일 때만 취소 버튼 표시 -->
                    <c:if test="${reservation.status eq '예약중'}">

                        <form action="${pageContext.request.contextPath}/mypage/reservation/cancel"
                              method="post">

                            <input type="hidden"
                                   name="reservationId"
                                   value="${reservation.reservationId}">

                            <button type="submit"
                                    onclick="return confirm('예약을 취소하시겠습니까?');">
                                예약 취소
                            </button>

                        </form>

                    </c:if>


                    <!-- 이미 취소된 예약 -->
                    <c:if test="${reservation.status ne '예약중'}">
                        -
                    </c:if>
                </td>
            </tr>

        </c:forEach>

    </table>

</c:if>

<br>

<a href="${pageContext.request.contextPath}/mypage">
    마이페이지로 돌아가기
</a>

</body>
</html>