<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <script src="${pageContext.request.contextPath}/js/csrf.js"></script>
    <title>마감 관리 - 관리자 - 갈래말래</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/components/buttonComponent.css">
    <link rel="stylesheet" href="/css/components/confirmModal.css">
    <link rel="stylesheet" href="/css/business.css">
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="closure" />
    </jsp:include>

    <div class="business-main">
        <jsp:include page="common/topbar.jsp">
            <jsp:param name="title" value="마감 관리" />
            <jsp:param name="date" value="${todayLabel}" />
            <jsp:param name="dateIcon" value="true" />
        </jsp:include>

        <div class="business-content">
            <div class="business-closure-page">
                <jsp:include page="common/closureCard.jsp">
                    <jsp:param name="placeId" value="${placeId}" />
                    <jsp:param name="isClosed" value="${isClosed}" />
                </jsp:include>
                <jsp:include page="common/closureDateCard.jsp">
                    <jsp:param name="placeId" value="${placeId}" />
                </jsp:include>
            </div>
        </div>
    </div>
</div>

<script src="/js/common.js"></script>
<script src="/js/business/business-closure.js"></script>
</body>
</html>
