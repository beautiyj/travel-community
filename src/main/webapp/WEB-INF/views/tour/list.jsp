<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <title>테스트용 -
            <c:choose>
                <c:when test="${selectedPlaceType eq 'stay'}">숙박</c:when>
                    <c:when test="${selectedPlaceType eq 'food'}">맛집</c:when>
                        <c:otherwise>관광지</c:otherwise>
                        </c:choose>
                    </title>
                    <!-- 공통 및 컴포넌트 CSS 링크 -->
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/header.css">
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/searchbar.css">
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/cardComponent.css">
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableButton.css">
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
                </head>
                <body>

                    <!-- 상단 헤더 컴포넌트 인클루드 -->
                    <jsp:include page="/WEB-INF/views/common/header.jsp" />

                    <div class="container">

                        <!-- 상단 타이틀 영역 (타입별 동적 변경) -->
                        <div style="margin-bottom: 24px;">
                            <h1 style="font-size: var(--text-2xl); font-weight: var(--font-weight-bold); color: var(--foreground); margin-bottom: 4px;">
                                <c:choose>
                                    <c:when test="${selectedPlaceType eq 'stay'}">숙박</c:when>
                                        <c:when test="${selectedPlaceType eq 'food'}">맛집</c:when>
                                            <c:otherwise>관광지</c:otherwise>
                                            </c:choose>
                                        </h1>
                                        <span style="font-size: var(--text-sm); color: var(--muted-foreground);">총 ${placeList.size()}개의 결과</span>
                                    </div>

                                    <!-- 검색바 컴포넌트 및 지역 필터 영역 -->
                                    <div style="margin-bottom: 24px;">
                                        <!-- 검색바 컴포넌트 인클루드 (경로 수정 완료) -->
                                        <jsp:include page="/WEB-INF/views/common/searchbar.jsp" />

                                        <!-- 지역 필터 버튼 바 -->
                                        <div style="display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; margin-top: 16px;">
                                            <button type="button" class="btn-selectable ${empty selectedRegionId ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}'">전체</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 1 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=1'">서울</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 2 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=2'">부산</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 3 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=3'">제주</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 4 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=4'">경주</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 5 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=5'">전주</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 6 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=6'">인천</button>
                                            <button type="button" class="btn-selectable ${selectedRegionId eq 7 ? 'is-active' : ''}" onclick="location.href='${pageContext.request.contextPath}/tour/list?placeType=${selectedPlaceType}&regionId=7'">강릉</button>
                                        </div>
                                    </div>

                                    <!-- 카드 그리드 영역 (cardComponent.jsp 재사용) -->
                                    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;">
                                        <c:choose>
                                            <c:when test="${not empty placeList}">
                                                <!-- 카드 전체를 감싸는 링크 태그 추가 -->
                                                <a href="${pageContext.request.contextPath}/tour/detail?placeId=${place.placeId}" style="text-decoration: none; color: inherit; display: block;">
                                                <c:forEach var="place" items="${placeList}">
                                                    <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                                                        <jsp:param name="firstimage"   value="${place.firstImage}" />
                                                        <jsp:param name="name"         value="${place.name}" />
                                                        <jsp:param name="placeId"      value="${place.placeId}" />
                                                        <jsp:param name="place_type"   value="${place.placeType}" />
                                                        <jsp:param name="hashTags"     value="${place.hashtags}" />
                                                        <jsp:param name="regionName"   value="${place.regionName}" />
                                                        <jsp:param name="price"        value="${place.displayPrice}" />
                                                        <%-- isBookmarked, rating, reviewCount는 별도 기능 붙기 전까지 카드 기본값 사용 --%>
                                                    </jsp:include>
                                                    </a>
                                                </c:forEach>
                                            </c:when>
                                            <c:otherwise>
                                                <div style="grid-column: span 4; text-align: center; padding: 80px 0; color: var(--muted-foreground);">
                                                    등록된 장소 데이터가 없습니다.
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                </div>

                                <!-- 공통 JS 인클루드 -->
                                <script src="${pageContext.request.contextPath}/js/common.js"></script>
                                <script src="${pageContext.request.contextPath}/js/dropdownSelector.js"></script>
                            </body>
                        </html>