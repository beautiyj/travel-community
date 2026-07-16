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
                                <c:choose>
                                    <c:when test="${r.status == '확정'}"><c:set var="statusClass" value="confirmed"/></c:when>
                                    <c:when test="${r.status == '대기중'}"><c:set var="statusClass" value="pending"/></c:when>
                                    <c:when test="${r.status == '완료'}"><c:set var="statusClass" value="done"/></c:when>
                                    <c:otherwise><c:set var="statusClass" value="cancelled"/></c:otherwise>
                                </c:choose>
                                <div class="business-reservation-row">
                                    <div>
                                        <span class="business-reservation-row__name">${r.visitorName}</span>
                                        <span class="business-reservation-row__meta">${r.phone} · ${r.visitDate} · ${r.headcount}명</span>
                                    </div>
                                    <div class="business-reservation-row__right">
                                        <c:if test="${r.amount != null}">
                                            <span class="business-reservation-row__price"><fmt:formatNumber value="${r.amount}" type="number" groupingUsed="true"/>원</span>
                                        </c:if>
                                        <span class="business-badge-status business-badge-status--${statusClass}">${r.status}</span>
                                        <c:if test="${r.status == '대기중'}">
                                            <form method="post" action="/business/reservations/${r.reservationId}/accept" class="business-inline-form">
                                                <input type="hidden" name="memberId" value="${memberId}" />
                                                <button type="submit" class="business-btn business-btn--primary business-btn--sm">수락</button>
                                            </form>
                                            <form method="post" action="/business/reservations/${r.reservationId}/reject" class="business-inline-form">
                                                <input type="hidden" name="memberId" value="${memberId}" />
                                                <button type="submit" class="business-btn business-btn--danger business-btn--sm">거절</button>
                                            </form>
                                        </c:if>
                                    </div>
                                </div>
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
