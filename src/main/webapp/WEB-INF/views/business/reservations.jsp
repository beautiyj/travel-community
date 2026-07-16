<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>예약 관리 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/business.css">
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="reservations" />
    </jsp:include>

    <div class="business-main">
        <div class="business-topbar">
            <h1 class="business-topbar__title">예약 관리</h1>
            <span class="business-topbar__date">총 ${reservations.size()}건</span>
        </div>

        <div class="business-content">
            <div class="business-filter-row">
                <c:forEach var="s" items="${statusOptions}">
                    <c:url value="/business/reservations" var="filterUrl">
                        <c:param name="memberId" value="${memberId}" />
                        <c:if test="${s != '전체'}">
                            <c:param name="status" value="${s}" />
                        </c:if>
                    </c:url>
                    <a href="${filterUrl}" class="business-filter-btn${statusFilter == s ? ' is-active' : ''}">${s}</a>
                </c:forEach>
            </div>

            <div class="business-card">
                <c:choose>
                    <c:when test="${empty reservations}">
                        <p class="business-empty">해당 상태의 예약이 없습니다</p>
                    </c:when>
                    <c:otherwise>
                        <div class="business-reservation-list">
                            <c:forEach var="r" items="${reservations}">
                                <jsp:include page="common/reservationRow.jsp">
                                    <jsp:param name="name" value="${r.visitorName}" />
                                    <jsp:param name="phone" value="${r.phone}" />
                                    <jsp:param name="visitDate" value="${r.visitDate}" />
                                    <jsp:param name="headcount" value="${r.headcount}" />
                                    <jsp:param name="status" value="${r.status}" />
                                    <jsp:param name="amount" value="${r.amount}" />
                                    <jsp:param name="mode" value="actionable" />
                                    <jsp:param name="reservationId" value="${r.reservationId}" />
                                    <jsp:param name="memberId" value="${memberId}" />
                                </jsp:include>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

</body>
</html>
