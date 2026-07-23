<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<link rel="stylesheet" href="${cp}/css/common.css">
<link rel="stylesheet" href="${cp}/css/components/dropdownSelector.css">
<link rel="stylesheet" href="${cp}/css/components/buttonComponent.css">

<header class="header">
    <div class="header-inner">
        <a href="${cp}/" class="brand">
            <span class="logo">TA</span>
            <span class="name">갈래말래</span>
        </a>

        <nav class="nav">
            <a href="${cp}/tour/list">숙박</a>
            <a href="${cp}/tour/list">맛집</a>
            <a href="${cp}/tour/list">여행지</a>
            <a href="${cp}/community/list">커뮤니티</a>
        </nav>

        <div class="header-actions">
            <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
                <jsp:param name="dropdownId" value="header_region" />
                <jsp:param name="listAttr" value="regionList" />
                <jsp:param name="selectedAttr" value="areaCode" />
                <jsp:param name="selectedNameAttr" value="areaName" />
                <jsp:param name="targetUrl" value="/tour/list" />
                <jsp:param name="paramKey" value="areaCode" />
                <jsp:param name="defaultLabel" value="지역 선택" />
                <jsp:param name="width" value="140px" />
            </jsp:include>

            <a href="${cp}/member/login" class="link-text">로그인</a>

            <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                <jsp:param name="text" value="회원가입" />
                <jsp:param name="size" value="var(--text-sm)" />
                <jsp:param name="onclick" value="location.href='${cp}/member/signup'" />
            </jsp:include>
        </div>
    </div>
</header>

<script src="${cp}/js/dropdownSelector.js"></script>
