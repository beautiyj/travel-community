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

        <%--TODO: 0803 tour 도메인 링크 추가 완료 --%>
        <nav class="nav">
            <a href="${cp}/tour/list?placeType=stay">숙박</a>
            <a href="${cp}/tour/list?placeType=food">맛집</a>
            <a href="${cp}/tour/list?placeType=tour">여행지</a>
            <a href="${cp}/community/list">커뮤니티</a>
        </nav>

        <%-- TODO: 0803 헤더의 드롭다운셀렉터 삭제하기, 메인의 서치바 내부로 드롭다운 넣어서 메인, list 페이지 사용 예정 --%>
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

            <%-- 로그인 세션(loginMember) 유무에 따라 UI변경 --%>
            <c:choose>
                <c:when test="${not empty sessionScope.loginMember}">
                    <a href="${cp}/mypage" class="link-text">마이페이지</a>

                    <%-- 로그아웃은 POST 전용(AuthController)이라 링크가 아닌 폼 제출로 처리 --%>
                    <form action="${cp}/auth/logout" method="post" style="display:contents;">
                        <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                            <jsp:param name="text" value="로그아웃" />
                            <jsp:param name="size" value="var(--text-sm)" />
                        </jsp:include>
                    </form>
                </c:when>
                <c:otherwise>
                    <a href="${cp}/auth/login" class="link-text">로그인</a>

                    <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                        <jsp:param name="text" value="회원가입" />
                        <jsp:param name="size" value="var(--text-sm)" />
                        <jsp:param name="onclick" value="location.href='${cp}/auth/signup'" />
                    </jsp:include>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>

<script src="${cp}/js/dropdownSelector.js"></script>
