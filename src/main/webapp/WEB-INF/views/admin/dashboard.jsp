<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>대시보드 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/admin.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
</head>
<body>

<div class="admin-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="overview" />
    </jsp:include>

    <div class="admin-main">
        <div class="admin-topbar">
            <h1 class="admin-topbar__title">대시보드</h1>
            <span class="admin-topbar__date">${todayLabel}</span>
        </div>

        <div class="admin-content">
            <!-- KPI 카드 -->
            <div class="admin-kpi-grid">
                <div class="admin-kpi-card">
                    <p class="admin-kpi-card__label">이번 달 예약</p>
                    <p class="admin-kpi-card__value">${monthlyCount}<span class="admin-kpi-card__unit">건</span></p>
                </div>
                <div class="admin-kpi-card admin-kpi-card--warn">
                    <p class="admin-kpi-card__label">대기중 예약</p>
                    <p class="admin-kpi-card__value">${pendingCount}<span class="admin-kpi-card__unit">건 처리 필요</span></p>
                </div>
                <div class="admin-kpi-card admin-kpi-card--good">
                    <p class="admin-kpi-card__label">오늘 방문 예정</p>
                    <p class="admin-kpi-card__value">${todayVisitors}<span class="admin-kpi-card__unit">명</span></p>
                </div>
            </div>

            <!-- 월별 예약 추이 차트 -->
            <div class="admin-card">
                <h2 class="admin-card__title">월별 예약 추이 <span class="admin-card__subtitle">최근 6개월</span></h2>
                <div class="admin-chart-wrap">
                    <canvas id="monthlyTrendChart"></canvas>
                </div>
            </div>

            <!-- 오늘 예약 현황 -->
            <div class="admin-card">
                <h2 class="admin-card__title">오늘 예약 현황 <span class="admin-card__subtitle">${todayLabel}</span></h2>

                <c:choose>
                    <c:when test="${empty todayReservations}">
                        <p class="admin-empty">오늘 예약된 방문이 없습니다</p>
                    </c:when>
                    <c:otherwise>
                        <div class="admin-reservation-list">
                            <c:forEach var="r" items="${todayReservations}">
                                <c:choose>
                                    <c:when test="${r.status == '확정'}"><c:set var="statusClass" value="confirmed"/></c:when>
                                    <c:when test="${r.status == '대기중'}"><c:set var="statusClass" value="pending"/></c:when>
                                    <c:when test="${r.status == '완료'}"><c:set var="statusClass" value="done"/></c:when>
                                    <c:otherwise><c:set var="statusClass" value="cancelled"/></c:otherwise>
                                </c:choose>
                                <div class="admin-reservation-row">
                                    <div>
                                        <span class="admin-reservation-row__name">${r.visitorName}</span>
                                        <span class="admin-reservation-row__meta">${r.phone} · ${r.headcount}명</span>
                                    </div>
                                    <div class="admin-reservation-row__right">
                                        <c:if test="${r.amount != null}">
                                            <span class="admin-reservation-row__price"><fmt:formatNumber value="${r.amount}" type="number" groupingUsed="true"/>원</span>
                                        </c:if>
                                        <span class="admin-badge-status admin-badge-status--${statusClass}">${r.status}</span>
                                        <c:if test="${r.status == '대기중'}">
                                            <!-- 실제 수락 액션 연결은 예약 관리 탭 구현 시 진행 -->
                                            <button type="button" class="admin-btn admin-btn--primary admin-btn--sm">수락</button>
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


<script>
    const monthlyTrendLabels = [<c:forEach var="m" items="${monthlyTrend}" varStatus="st">'${m.monthLabel}'<c:if test="${!st.last}">,</c:if></c:forEach>];
    const monthlyTrendCounts = [<c:forEach var="m" items="${monthlyTrend}" varStatus="st">${m.bookingCount}<c:if test="${!st.last}">,</c:if></c:forEach>];
    const monthlyTrendRevenue = [<c:forEach var="m" items="${monthlyTrend}" varStatus="st">${m.revenue}<c:if test="${!st.last}">,</c:if></c:forEach>];

    new Chart(document.getElementById('monthlyTrendChart'), {
        data: {
            labels: monthlyTrendLabels,
            datasets: [
                {
                    type: 'bar',
                    label: '예약 건수',
                    data: monthlyTrendCounts,
                    backgroundColor: 'rgba(2, 132, 199, 0.5)',
                    yAxisID: 'yCount'
                },
                {
                    type: 'line',
                    label: '매출',
                    data: monthlyTrendRevenue,
                    borderColor: '#f59e0b',
                    backgroundColor: 'rgba(245, 158, 11, 0.15)',
                    tension: 0.3,
                    yAxisID: 'yRevenue'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                yCount: {
                    type: 'linear',
                    position: 'left',
                    beginAtZero: true,
                    title: { display: true, text: '건' }
                },
                yRevenue: {
                    type: 'linear',
                    position: 'right',
                    beginAtZero: true,
                    grid: { drawOnChartArea: false },
                    title: { display: true, text: '원' }
                }
            }
        }
    });
</script>

</body>
</html>
