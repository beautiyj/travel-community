<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>관리자 대시보드 - 갈래말래</title>
    <c:url var="commonCssUrl" value="/css/common.css"/>
    <c:url var="adminCssUrl" value="/css/admin/admin.css"/>
    <c:url var="businessApplicationsUrl" value="/admin/business-applications"/>
    <link rel="stylesheet" href="${commonCssUrl}">
    <link rel="stylesheet" href="${adminCssUrl}">
</head>
<body>
<div class="admin-layout">
    <%-- include 파라미터는 메뉴 강조에만 쓰이며 관리자 인가는 컨트롤러 진입 전에 서버가 보장해야 한다. --%>
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="dashboard"/>
    </jsp:include>

    <main class="admin-main">
        <header class="admin-topbar">
            <h1 class="admin-topbar__title">관리자 대시보드</h1>
            <span class="admin-topbar__caption">ADMIN</span>
        </header>

        <div class="admin-content">
            <div class="admin-page-heading">
                <div>
                    <h2 class="admin-page-heading__title">사업자 인증 현황</h2>
                    <p class="admin-page-heading__description">
                        사업자 회원이 제출한 인증 신청의 처리 현황을 확인할 수 있습니다.
                    </p>
                </div>
            </div>

            <%--
              summary는 서버가 DB에서 집계한 total/pending/approved/rejected 건수 모델이다.
              화면에서 목록을 다시 세지 않아 조회 시점의 서버 집계 결과를 그대로 표시한다.
            --%>
            <section class="admin-dashboard-kpi-grid" aria-label="사업자 인증 신청 요약">
                <article class="admin-dashboard-kpi">
                    <p class="admin-dashboard-kpi__label">전체 신청</p>
                    <p class="admin-dashboard-kpi__value">
                        <c:out value="${summary.totalCount}"/>
                        <span class="admin-dashboard-kpi__unit">건</span>
                    </p>
                </article>
                <article class="admin-dashboard-kpi admin-dashboard-kpi--pending">
                    <p class="admin-dashboard-kpi__label">심사 대기</p>
                    <p class="admin-dashboard-kpi__value">
                        <c:out value="${summary.pendingCount}"/>
                        <span class="admin-dashboard-kpi__unit">건</span>
                    </p>
                </article>
                <article class="admin-dashboard-kpi admin-dashboard-kpi--approved">
                    <p class="admin-dashboard-kpi__label">승인</p>
                    <p class="admin-dashboard-kpi__value">
                        <c:out value="${summary.approvedCount}"/>
                        <span class="admin-dashboard-kpi__unit">건</span>
                    </p>
                </article>
                <article class="admin-dashboard-kpi admin-dashboard-kpi--rejected">
                    <p class="admin-dashboard-kpi__label">반려</p>
                    <p class="admin-dashboard-kpi__value">
                        <c:out value="${summary.rejectedCount}"/>
                        <span class="admin-dashboard-kpi__unit">건</span>
                    </p>
                </article>
            </section>

            <section class="admin-card admin-dashboard-cta" aria-labelledby="business-application-cta-title">
                <div>
                    <h3 id="business-application-cta-title" class="admin-card__title">사업자 인증 관리</h3>
                    <p class="admin-card__description">
                        신청 목록을 확인하고 상세 페이지에서 승인 또는 반려를 결정합니다.
                    </p>
                </div>
                <a class="admin-btn admin-btn--approve admin-dashboard-cta__link"
                   href="${businessApplicationsUrl}">
                    인증 신청 관리
                </a>
            </section>
        </div>
    </main>
</div>
</body>
</html>
