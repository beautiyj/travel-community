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
            <span class="business-topbar__date">
                <svg class="business-topbar__date-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                    <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                ${todayLabel}
            </span>
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
                    <c:choose>
                        <c:when test="${s == '취소요청'}"><c:set var="filterCount" value="${statusCounts.cancelRequestCount}" /></c:when>
                        <c:when test="${s == '확정'}"><c:set var="filterCount" value="${statusCounts.confirmedCount}" /></c:when>
                        <c:when test="${s == '완료'}"><c:set var="filterCount" value="${statusCounts.doneCount}" /></c:when>
                        <c:when test="${s == '취소'}"><c:set var="filterCount" value="${statusCounts.cancelledCount}" /></c:when>
                        <c:otherwise><c:set var="filterCount" value="" /></c:otherwise>
                    </c:choose>
                    <a href="${filterUrl}" class="business-filter-btn${statusFilter == s ? ' is-active' : ''}">${s}<c:if test="${not empty filterCount}"> <span class="business-filter-btn__count">${filterCount}</span></c:if></a>
                </c:forEach>
                <span class="business-filter-row__total">총 ${reservations.size()}건</span>
            </div>

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
                                <div class="business-reservation-table__cell business-reservation-table__cell--action">상태 / 처리</div>
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
