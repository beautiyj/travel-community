<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <title>0728 미완성, 컨트롤러연결테스트용 / 컴포넌트 조정필요 - <c:choose><c:when test="${selectedPlaceType == 'stay'}">숙박</c:when><c:when test="${selectedPlaceType == 'food'}">맛집</c:when><c:otherwise>관광지</c:otherwise></c:choose></title>

        <!-- 공통 CSS 및 모듈 스타일 포함 -->
        <link rel="stylesheet" href="/css/common.css">
        <link rel="stylesheet" href="/css/business.css">
    </head>
    <body>

        <!-- 1. 공통 헤더 컴포넌트 재사용 -->
        <jsp:include page="/WEB-INF/views/common/header.jsp" />

        <main class="container">

            <!-- 2. 동적 페이지 타이틀 -->
            <div class="page-header">
                <h2 class="page-title">
                    <c:choose>
                        <c:when test="${selectedPlaceType == 'stay'}">숙박</c:when>
                            <c:when test="${selectedPlaceType == 'food'}">맛집</c:when>
                                <c:otherwise>관광지</c:otherwise>
                                </c:choose>
                            </h2>
                            <p class="result-count">총 ${placeList.size()}개의 결과</p>
                        </div>

                        <!-- 3. 동적 검색바 -->
                        <div class="search-bar-wrap">
                            <input type="text" id="searchInput" class="search-input"
                            placeholder="<c:choose><c:when test="${selectedPlaceType == 'stay'}">숙박</c:when><c:when test="${selectedPlaceType == 'food'}">맛집</c:when><c:otherwise>관광지</c:otherwise></c:choose> 내 검색 — 이름, 지역, 태그" />
                        </div>

                        <!-- 4. 공통 지역 선택 드롭다운/버튼 컴포넌트 재사용 -->
                        <div class="region-select-area">
                            <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp" />
                        </div>

                        <!-- 5. 장소 카드 목록 영역 -->
                        <div class="place-card-grid">
                            <c:forEach var="place" items="${placeList}">
                                <div class="place-card" onclick="location.href='/tour/detail/${place.placeId}'">

                                    <!-- 썸네일 & 뱃지 -->
                                    <div class="card-image-box">
                                        <img src="${place.firstImage}" alt="${place.name}" loading="lazy" />
                                        <span class="badge ${place.placeType}">
                                            <c:choose>
                                                <c:when test="${place.placeType == 'stay'}">숙박</c:when>
                                                    <c:when test="${place.placeType == 'food'}">맛집</c:when>
                                                        <c:otherwise>관광지</c:otherwise>
                                                        </c:choose>
                                                    </span>
                                                </div>

                                                <!-- 카드 정보 -->
                                                <div class="card-content">
                                                    <h3 class="place-name">${place.name}</h3>
                                                    <p class="place-address">${place.address}</p>

                                                    <!-- 가격 정보 (minPrice / useFeeInfo) -->
                                                    <div class="price-box">
                                                        <c:choose>
                                                            <c:when test="${place.minPrice == 0}">
                                                                <span class="price free">무료</span>
                                                            </c:when>
                                                            <c:when test="${not empty place.minPrice}">
                                                                <span class="price"><fmt:formatNumber value="${place.minPrice}" type="number"/>원~</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="price-info">${not empty place.useFeeInfo ? place.useFeeInfo : '가격 정보 없음'}</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>

                                                    <!-- 해시태그 목록 -->
                                                    <c:if test="${not empty place.hashtags}">
                                                        <div class="hashtag-box">
                                                            <c:forEach var="tag" items="${place.hashtags.split(',')}">
                                                                <span class="tag">${tag}</span>
                                                            </c:forEach>
                                                        </div>
                                                    </c:if>

                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>

                                </main>

                                <!-- 공통 스크립트 및 드롭다운 연동 JS 재사용 -->
                                <script src="/js/dropdownSelector.js"></script>

                            </body>
                        </html>