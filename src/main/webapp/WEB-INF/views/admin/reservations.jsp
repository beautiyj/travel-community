<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>예약 관리 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body>

<div class="admin-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="reservations" />
    </jsp:include>

    <div class="admin-main">
        <div class="admin-topbar">
            <h1 class="admin-topbar__title">예약 관리</h1>
            <span class="admin-topbar__date">총 ${reservations.size()}건</span>
        </div>

        <div class="admin-content">
            <div class="admin-filter-row">
                <c:forEach var="s" items="${statusOptions}">
                    <c:url value="/admin/reservations" var="filterUrl">
                        <c:param name="memberId" value="${memberId}" />
                        <c:if test="${s != '전체'}">
                            <c:param name="status" value="${s}" />
                        </c:if>
                    </c:url>
                    <a href="${filterUrl}" class="admin-filter-btn${statusFilter == s ? ' is-active' : ''}">${s}</a>
                </c:forEach>
            </div>

            <div class="admin-card">
                <c:choose>
                    <c:when test="${empty reservations}">
                        <p class="admin-empty">해당 상태의 예약이 없습니다</p>
                    </c:when>
                    <c:otherwise>
                        <div class="admin-reservation-list">
                            <c:forEach var="r" items="${reservations}">
                                <c:choose>
                                    <c:when test="${r.status == '확정'}"><c:set var="statusClass" value="confirmed"/></c:when>
                                    <c:when test="${r.status == '대기중'}"><c:set var="statusClass" value="pending"/></c:when>
                                    <c:when test="${r.status == '완료'}"><c:set var="statusClass" value="done"/></c:when>
                                    <c:otherwise><c:set var="statusClass" value="cancelled"/></c:otherwise>
                                </c:choose>
                                <div class="admin-reservation-row">
                                    <div>
                                        <span class="admin-reservation-row__name">${r.visitorName}</span>
                                        <span class="admin-reservation-row__meta">${r.phone} · ${r.visitDate} · ${r.headcount}명</span>
                                    </div>
                                    <div class="admin-reservation-row__right">
                                        <c:if test="${r.amount != null}">
                                            <span class="admin-reservation-row__price"><fmt:formatNumber value="${r.amount}" type="number" groupingUsed="true"/>원</span>
                                        </c:if>
                                        <span class="admin-badge-status admin-badge-status--${statusClass}">${r.status}</span>
                                        <c:if test="${r.status == '대기중'}">
                                            <form method="post" action="/admin/reservations/${r.reservationId}/accept" class="admin-inline-form">
                                                <input type="hidden" name="memberId" value="${memberId}" />
                                                <button type="submit" class="admin-btn admin-btn--primary admin-btn--sm">수락</button>
                                            </form>
                                            <form method="post" action="/admin/reservations/${r.reservationId}/reject" class="admin-inline-form">
                                                <input type="hidden" name="memberId" value="${memberId}" />
                                                <button type="submit" class="admin-btn admin-btn--danger admin-btn--sm">거절</button>
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
