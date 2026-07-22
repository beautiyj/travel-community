<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>마감 관리 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/business.css">
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="closure" />
    </jsp:include>

    <div class="business-main">
        <div class="business-topbar">
            <h1 class="business-topbar__title">마감 관리</h1>
        </div>

        <div class="business-content">
            <div class="business-closure-page">
                <jsp:include page="common/closureCard.jsp">
                    <jsp:param name="placeId" value="${placeId}" />
                    <jsp:param name="memberId" value="${memberId}" />
                    <jsp:param name="isClosed" value="${isClosed}" />
                </jsp:include>
                <jsp:include page="common/closureDateCard.jsp" />
            </div>
        </div>
    </div>
</div>

<script src="/js/business-closure.js"></script>
</body>
</html>
