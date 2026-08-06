<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <title>갈래말래 -
            <c:choose>
                <c:when test="${selectedPlaceType eq 'stay'}">숙박 - Travel</c:when>
                    <c:when test="${selectedPlaceType eq 'food'}">맛집 - Travel</c:when>
                        <c:when test="${selectedPlaceType eq 'tour'}">관광지 - Travel</c:when>
                            <c:otherwise>전체 검색 결과 - Travel</c:otherwise>
                            </c:choose>
                        </title>
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/header.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/searchbar.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/cardComponent.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableButton.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
                    </head>
                    <body>
                        <jsp:include page="/WEB-INF/views/common/header.jsp" />
                        <div class="container">

                            <!-- 상단 타이틀 영역 (타입별 동적 변경) -->
                            <div style="margin-bottom: 24px;">
                                <h1 style="font-size: var(--text-2xl); font-weight: var(--font-weight-bold); color: var(--foreground); margin-bottom: 4px;">
                                    <c:choose>
                                        <c:when test="${selectedPlaceType eq 'stay'}">숙박</c:when>
                                            <c:when test="${selectedPlaceType eq 'food'}">맛집</c:when>
                                                <c:when test="${selectedPlaceType eq 'tour'}">관광지</c:when>
                                                    <c:otherwise>전체 검색 결과</c:otherwise>
                                                    </c:choose>
                                                    <!-- 검색어 키워드가 있을 경우 상단 타이틀 옆 표시 -->
                                                    <c:if test="${not empty keyword}">
                                                        <span style="font-size: var(--text-lg); color: var(--primary); font-weight: normal;"> ('${keyword}' 검색)</span>
                                                    </c:if>
                                                </h1>
                                                <span style="font-size: var(--text-sm); color: var(--muted-foreground);">총 ${placeList.size()}개의 결과</span>
                                            </div>

                                            <!-- 검색바 컴포넌트 및 지역 필터 영역 -->
                                            <!-- 검색바 컴포넌트 및 지역 필터 영역 -->
                                            <div style="margin-bottom: 24px;">
                                                <!-- 제출용 form 태그 생성 및 searchbar.jsp 기존 내장 드롭다운 옵션(useDropdown) 활성화 -->
                                                <form action="${pageContext.request.contextPath}/tour/list" method="get">
                                                    <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                                                        <jsp:param name="useDropdown"       value="true" />
                                                        <jsp:param name="listAttr"          value="parentRegionList" />
                                                        <jsp:param name="selectedAttr"      value="selectedRegionId" />
                                                        <jsp:param name="value"             value="${keyword}" />
                                                        <jsp:param name="placeholder"       value="어디로 떠나고 싶으신가요? (지역, 장소명, #해시태그)" />
                                                    </jsp:include>

                                                    <!-- 기존 선택된 카테고리(숙박/맛집/관광지) 유지용 hidden 필드 -->
                                                    <c:if test="${not empty selectedPlaceType}">
                                                        <input type="hidden" name="placeType" value="${selectedPlaceType}" />
                                                    </c:if>
                                                </form>
                                            </div>

                                            <!-- 카드 그리드 영역 (cardComponent.jsp 재사용) -->
                                            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;">
                                                <c:choose>
                                                    <c:when test="${not empty placeList}">
                                                        <c:forEach var="place" items="${placeList}">
                                                            <!-- 카드 전체를 감싸는 링크 태그 추가 -->
                                                            <a href="${pageContext.request.contextPath}/tour/detail?placeId=${place.placeId}"
                                                            class="place-card-link"
                                                            style="text-decoration: none; color: inherit; display: block;">
                                                            <%-- isBookmarked, rating, reviewCount는 별도 기능 붙기 전까지 카드 기본값 사용 --%>
                                                            <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                                                                <jsp:param name="firstimage"   value="${place.firstImage}" />
                                                                <jsp:param name="name"         value="${place.name}" />
                                                                <jsp:param name="placeId"      value="${place.placeId}" />
                                                                <jsp:param name="place_type"   value="${place.placeType}" />
                                                                <jsp:param name="hashTags"     value="${place.hashtags}" />
                                                                <jsp:param name="regionName"   value="${place.regionName}" />
                                                                <jsp:param name="price"        value="${place.displayPrice}" />
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
                                    <jsp:include page="/WEB-INF/views/common/footer.jsp" />
                                    <script src="${pageContext.request.contextPath}/js/dropdownSelector.js"></script>
                                </body>
                            </html>