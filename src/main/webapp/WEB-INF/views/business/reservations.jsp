<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>예약 관리 - 관리자 - 갈래말래</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/business.css">
    <link rel="stylesheet" href="/css/components/smallButton.css">
    <link rel="stylesheet" href="/css/components/buttonComponent.css">
    <link rel="stylesheet" href="/css/components/confirmModal.css">
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="reservations" />
    </jsp:include>

    <div class="business-main">
        <jsp:include page="common/topbar.jsp">
            <jsp:param name="title" value="예약 관리" />
            <jsp:param name="date" value="${todayLabel}" />
            <jsp:param name="dateIcon" value="true" />
        </jsp:include>

        <div class="business-content">
            <c:if test="${not empty reservationError}">
                <div class="business-alert business-alert--error"><c:out value="${reservationError}" /></div>
            </c:if>

            <jsp:include page="common/filterTabs.jsp">
                <jsp:param name="baseUrl" value="/business/reservations" />
                <jsp:param name="paramName" value="status" />
                <jsp:param name="totalCount" value="${reservations.size()}" />
            </jsp:include>

            <div class="business-card">
                <c:choose>
                    <c:when test="${empty reservations}">
                        <p class="business-empty">해당 상태의 예약이 없습니다</p>
                    </c:when>
                    <c:otherwise>
                        <div class="business-reservation-table">
                            <div class="business-reservation-table__row business-reservation-table__row--head">
                                <div class="business-reservation-table__cell">예약자</div>
                                <div class="business-reservation-table__cell">연락처</div>
                                <div class="business-reservation-table__cell">방문일</div>
                                <div class="business-reservation-table__cell">인원</div>
                                <div class="business-reservation-table__cell">금액</div>
                                <div class="business-reservation-table__cell">상태</div>
                                <div class="business-reservation-table__cell business-reservation-table__cell--action"></div>
                            </div>
                            <c:forEach var="r" items="${reservations}">
                                <jsp:include page="common/reservationRow.jsp">
                                    <jsp:param name="layout" value="table" />
                                    <jsp:param name="name" value="${r.visitorName}" />
                                    <jsp:param name="phone" value="${r.phone}" />
                                    <jsp:param name="visitDate" value="${r.visitDate}" />
                                    <jsp:param name="headcount" value="${r.headcount}" />
                                    <jsp:param name="status" value="${r.status}" />
                                    <jsp:param name="statusLabel" value="${r.status.label}" />
                                    <jsp:param name="amount" value="${r.amount}" />
                                    <jsp:param name="mode" value="actionable" />
                                    <jsp:param name="reservationId" value="${r.reservationId}" />
                                    <jsp:param name="cancelReason" value="${r.cancelReason}" />
                                    <jsp:param name="cancelRequestedAt" value="${r.cancelRequestedAt}" />
                                </jsp:include>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script src="/js/common.js"></script>
<script src="/js/business/business-reservations.js"></script>

</body>
</html>
