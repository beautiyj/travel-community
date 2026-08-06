<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<%--
  ── 인기 여행지 캐러셀 더미 데이터 (유지) ──────────────────────────────
  슬라이드 1장 = 이미지 ^ 지역 ^ 제목
  슬라이드끼리는 | 로 구분
--%>
<c:set var="destinationSlides" value="
https://picsum.photos/id/1069/560/360^제주^에메랄드빛 바다가 펼쳐지는 여름|
https://picsum.photos/id/1041/560/360^부산^광안대교와 함께하는 낭만적인 야경|
https://picsum.photos/id/1015/560/360^경주^천년의 숨결이 남아있는 고도 여행|
https://picsum.photos/id/1024/560/360^전주^전통과 현대가 어우러진 골목 여행
" />

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>갈래말래 - 여행 커뮤니티 메인</title>

    <link rel="stylesheet" href="${cp}/css/common.css">
    <link rel="stylesheet" href="${cp}/css/main.css">
    <link rel="stylesheet" href="${cp}/css/components/searchbar.css">
    <link rel="stylesheet" href="${cp}/css/components/selectableButton.css">
    <link rel="stylesheet" href="${cp}/css/components/buttonComponent.css">
    <link rel="stylesheet" href="${cp}/css/components/tagButton.css">
    <link rel="stylesheet" href="${cp}/css/components/wishButton.css">
    <link rel="stylesheet" href="${cp}/css/components/cardComponent.css">
    <link rel="stylesheet" href="${cp}/css/components/banner.css">
    <link rel="stylesheet" href="${cp}/css/components/smallButton.css">
</head>
<body style="margin: 0; padding: 0;">

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<%-- ===================== 검색 히어로 (통합 지역 드롭다운 검색바) ===================== --%>
<div class="main-hero">
    <div class="main-hero-inner">
        <p class="main-hero-eyebrow">갈래말래</p>
        <h1 class="main-hero-title">어디로 떠나고 싶으세요?</h1>
        <p class="main-hero-subtitle">숙박, 맛집, 여행지를 한 번에 검색하세요</p>

        <div class="main-hero-search">
            <form action="${cp}/tour/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                    <jsp:param name="useDropdown"    value="true" />
                    <jsp:param name="listAttr"       value="parentRegionList" />
                    <jsp:param name="selectedAttr"   value="selectedRegionId" />
                    <jsp:param name="placeholder"    value="어디로 떠나고 싶으신가요? (지역, 장소명, #해시태그)" />
                    <jsp:param name="width"          value="640px" />
                </jsp:include>
            </form>
        </div>

        <div class="main-hero-pills">
            <c:forEach var="region" items="${fn:split('서울,부산,제주,경주,전주,강릉', ',')}">
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="${region}" />
                    <jsp:param name="onclick" value="location.href='${cp}/tour/list?keyword=${region}'" />
                </jsp:include>
            </c:forEach>
        </div>
    </div>
</div>

<div class="main-container">

    <%-- ===================== 광고 배너 슬라이더 ===================== --%>
    <div class="main-section">
        <jsp:include page="/WEB-INF/views/common/banner.jsp" />
    </div>

    <%-- ===================== 인기 여행지 캐러셀 (상단 더미 데이터 연동) ===================== --%>
    <div class="main-section" data-destination>
        <div class="main-section-head">
            <h2 class="main-section-title">지금 인기 여행지</h2>
            <div class="destination-controls">
                <button type="button" class="btn-wish-trigger" data-destination-prev aria-label="이전 여행지">
                    <svg class="destination-chevron" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="15 5 8 12 15 19" />
                    </svg>
                </button>
                <button type="button" class="btn-wish-trigger" data-destination-next aria-label="다음 여행지">
                    <svg class="destination-chevron" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="9 5 16 12 9 19" />
                    </svg>
                </button>
            </div>
        </div>

        <div class="destination-carousel">
            <div class="destination-track" data-destination-track>
                <c:forEach var="row" items="${fn:split(destinationSlides, '|')}">
                    <c:set var="col" value="${fn:split(row, '^')}" />
                    <div class="destination-slide" onclick="location.href='${cp}/tour/list?keyword=${fn:trim(col[1])}'">
                        <img src="${fn:trim(col[0])}" alt="${fn:trim(col[2])}" />
                        <div class="destination-caption">
                            <span class="destination-region">${fn:trim(col[1])}</span>
                            <p class="destination-title">${fn:trim(col[2])}</p>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </div>

    <%-- ===================== 1. 추천 숙박 코너 (실제 DB 4개) ===================== --%>
    <div class="main-section">
        <div class="main-section-head">
            <h2 class="main-section-title">🏨 추천 숙박</h2>
            <a href="${cp}/tour/list?placeType=stay" class="main-section-more">숙박 전체보기 →</a>
        </div>

        <div class="place-grid">
            <c:choose>
                <c:when test="${not empty stayList}">
                    <c:forEach var="place" items="${stayList}">
                        <a href="${cp}/tour/detail?placeId=${place.placeId}" class="place-card-link">
                            <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                                <jsp:param name="firstimage"  value="${place.firstImage}" />
                                <jsp:param name="placeId"     value="${place.placeId}" />
                                <jsp:param name="place_type"  value="${place.placeType}" />
                                <jsp:param name="name"        value="${place.name}" />
                                <jsp:param name="regionName"  value="${place.regionName}" />
                                <jsp:param name="price"       value="${place.displayPrice}" />
                                <jsp:param name="hashTags"    value="${place.hashtags}" />
                            </jsp:include>
                        </a>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div style="grid-column: span 4; text-align: center; padding: 40px 0; color: var(--muted-foreground);">
                        등록된 숙박 데이터가 없습니다.
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- ===================== 2. 인기 맛집 코너 (실제 DB 4개) ===================== --%>
    <div class="main-section">
        <div class="main-section-head">
            <h2 class="main-section-title">🍽️ 인기 맛집</h2>
            <a href="${cp}/tour/list?placeType=food" class="main-section-more">맛집 전체보기 →</a>
        </div>

        <div class="place-grid">
            <c:choose>
                <c:when test="${not empty foodList}">
                    <c:forEach var="place" items="${foodList}">
                        <a href="${cp}/tour/detail?placeId=${place.placeId}" class="place-card-link">
                            <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                                <jsp:param name="firstimage"  value="${place.firstImage}" />
                                <jsp:param name="placeId"     value="${place.placeId}" />
                                <jsp:param name="place_type"  value="${place.placeType}" />
                                <jsp:param name="name"        value="${place.name}" />
                                <jsp:param name="regionName"  value="${place.regionName}" />
                                <jsp:param name="price"       value="${place.displayPrice}" />
                                <jsp:param name="hashTags"    value="${place.hashtags}" />
                            </jsp:include>
                        </a>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div style="grid-column: span 4; text-align: center; padding: 40px 0; color: var(--muted-foreground);">
                        등록된 맛집 데이터가 없습니다.
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- ===================== 3. 핫플 관광지 코너 (실제 DB 4개) ===================== --%>
    <div class="main-section">
        <div class="main-section-head">
            <h2 class="main-section-title">🌴 핫플 관광지</h2>
            <a href="${cp}/tour/list?placeType=tour" class="main-section-more">관광지 전체보기 →</a>
        </div>

        <div class="place-grid">
            <c:choose>
                <c:when test="${not empty tourList}">
                    <c:forEach var="place" items="${tourList}">
                        <a href="${cp}/tour/detail?placeId=${place.placeId}" class="place-card-link">
                            <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                                <jsp:param name="firstimage"  value="${place.firstImage}" />
                                <jsp:param name="placeId"     value="${place.placeId}" />
                                <jsp:param name="place_type"  value="${place.placeType}" />
                                <jsp:param name="name"        value="${place.name}" />
                                <jsp:param name="regionName"  value="${place.regionName}" />
                                <jsp:param name="price"       value="${place.displayPrice}" />
                                <jsp:param name="hashTags"    value="${place.hashtags}" />
                            </jsp:include>
                        </a>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div style="grid-column: span 4; text-align: center; padding: 40px 0; color: var(--muted-foreground);">
                        등록된 관광지 데이터가 없습니다.
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

</div>

<%-- ===================== 하단 CTA 배너 ===================== --%>
<div class="main-cta-banner">
    <h2 class="main-cta-title">지금 바로 나만의 여행을 시작하세요</h2>
    <p class="main-cta-subtitle">숙박, 맛집, 관광지를 한 번에 예약하고 특별한 혜택을 누리세요</p>

    <c:choose>
        <c:when test="${not empty sessionScope.loginMember}">
            <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                <jsp:param name="text" value="여행 탐색하기" />
                <jsp:param name="color" value="var(--card)" />
                <jsp:param name="onclick" value="location.href='${cp}/tour/list'" />
            </jsp:include>
        </c:when>
        <c:otherwise>
            <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                <jsp:param name="text" value="무료로 가입하기" />
                <jsp:param name="color" value="var(--card)" />
                <jsp:param name="onclick" value="location.href='${cp}/auth/signup'" />
            </jsp:include>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
<script src="${cp}/js/dropdownSelector.js"></script>
<script src="${cp}/js/main/main.js"></script>

</body>
</html>