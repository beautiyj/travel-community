<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>대시보드 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/business.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/wordcloud@1.2.2/src/wordcloud2.js"></script>
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="overview" />
    </jsp:include>

    <div class="business-main">
        <div class="business-topbar">
            <h1 class="business-topbar__title">대시보드</h1>
            <span class="business-topbar__date">${todayLabel}</span>
        </div>

        <div class="business-content">
            <!-- KPI 카드 -->
            <div class="business-kpi-grid">
                <div class="business-kpi-card">
                    <p class="business-kpi-card__label">이번 달 예약</p>
                    <p class="business-kpi-card__value">${monthlyCount}<span class="business-kpi-card__unit">건</span></p>
                </div>
                <div class="business-kpi-card business-kpi-card--warn">
                    <p class="business-kpi-card__label">결제 대기 예약</p>
                    <p class="business-kpi-card__value">${pendingCount}<span class="business-kpi-card__unit">건</span></p>
                </div>
                <div class="business-kpi-card business-kpi-card--good">
                    <p class="business-kpi-card__label">오늘 방문 예정</p>
                    <p class="business-kpi-card__value">${todayVisitors}<span class="business-kpi-card__unit">명</span></p>
                </div>
            </div>

            <!-- 월별 예약 추이 차트 -->
            <div class="business-card">
                <h2 class="business-card__title">월별 예약 추이 <span class="business-card__subtitle">최근 6개월</span></h2>
                <div class="business-chart-wrap">
                    <canvas id="monthlyTrendChart"></canvas>
                </div>
            </div>

            <!-- 후기 감성분석 -->
            <div class="business-card">
                <h2 class="business-card__title">후기 감성분석</h2>
                <div class="business-kpi-grid">
                    <div class="business-kpi-card business-kpi-card--good">
                        <p class="business-kpi-card__label">긍정 후기</p>
                        <p class="business-kpi-card__value">${reviewSentiment.positiveCount}<span class="business-kpi-card__unit">건</span></p>
                    </div>
                    <div class="business-kpi-card">
                        <p class="business-kpi-card__label">중립 후기</p>
                        <p class="business-kpi-card__value">${reviewSentiment.neutralCount}<span class="business-kpi-card__unit">건</span></p>
                    </div>
                    <div class="business-kpi-card business-kpi-card--warn">
                        <p class="business-kpi-card__label">부정 후기</p>
                        <p class="business-kpi-card__value">${reviewSentiment.negativeCount}<span class="business-kpi-card__unit">건</span></p>
                    </div>
                </div>

                <h3 class="business-card__subtitle2">키워드 워드클라우드</h3>
                <c:choose>
                    <c:when test="${empty reviewSentiment.keywords}">
                        <p class="business-empty">아직 분석된 후기 키워드가 없습니다</p>
                    </c:when>
                    <c:otherwise>
                        <div class="business-wordcloud-wrap">
                            <canvas id="reviewWordcloud"></canvas>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- 오늘 예약 현황 -->
            <div class="business-card">
                <h2 class="business-card__title">오늘 예약 현황 <span class="business-card__subtitle">${todayLabel}</span></h2>

                <c:choose>
                    <c:when test="${empty todayReservations}">
                        <p class="business-empty">오늘 예약된 방문이 없습니다</p>
                    </c:when>
                    <c:otherwise>
                        <div class="business-reservation-list">
                            <c:forEach var="r" items="${todayReservations}">
                                <jsp:include page="common/reservationRow.jsp">
                                    <jsp:param name="name" value="${r.visitorName}" />
                                    <jsp:param name="phone" value="${r.phone}" />
                                    <jsp:param name="headcount" value="${r.headcount}" />
                                    <jsp:param name="status" value="${r.status}" />
                                    <jsp:param name="statusLabel" value="${r.status.label}" />
                                    <jsp:param name="amount" value="${r.amount}" />
                                </jsp:include>
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

    const keywordCloudData = ${keywordCloudJson};
    const wordcloudCanvas = document.getElementById('reviewWordcloud');
    if (wordcloudCanvas && keywordCloudData.length > 0) {
        const wrap = wordcloudCanvas.parentElement;
        wordcloudCanvas.width = wrap.clientWidth;
        wordcloudCanvas.height = 360;

        const cloudWeights = keywordCloudData.map(function (pair) { return pair[1]; });
        const minWeight = Math.min.apply(null, cloudWeights);
        const maxWeight = Math.max.apply(null, cloudWeights);

        WordCloud(wordcloudCanvas, {
            list: keywordCloudData,
            weightFactor: function (count) {
                return 18 + count * 18;
            },
            fontFamily: '"Pretendard", "Apple SD Gothic Neo", "Malgun Gothic", sans-serif',
            // 색조는 브랜드 블루(hue 200)로 통일하고, 빈도가 높을수록 진하게(명도만 차등)
            color: function (word, weight) {
                const ratio = maxWeight === minWeight ? 1 : (weight - minWeight) / (maxWeight - minWeight);
                const lightness = 60 - ratio * 32;
                return 'hsl(200, 80%, ' + lightness + '%)';
            },
            backgroundColor: 'transparent',
            rotateRatio: 0,
            gridSize: 8
        });
    }
</script>

</body>
</html>
