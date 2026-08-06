<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<%--
  ── 인기 여행지 캐러셀 더미 데이터 ──────────────────────────────
  슬라이드 1장 = 이미지 ^ 지역 ^ 제목
  슬라이드끼리는 | 로 구분

  ※ 실제 데이터를 붙일 때는 이 c:set을 지우고
     아래 c:forEach를 items="${destinationList}"로 바꾸면 된다.
--%>
<c:set var="destinationSlides" value="
https://picsum.photos/id/1069/560/360^제주^에메랄드빛 바다가 펼쳐지는 여름|
https://picsum.photos/id/1041/560/360^부산^광안대교와 함께하는 낭만적인 야경|
https://picsum.photos/id/1015/560/360^경주^천년의 숨결이 남아있는 고도 여행|
https://picsum.photos/id/1024/560/360^전주^전통과 현대가 어우러진 골목 여행
" />

<%--
  ── 숙박/맛집/관광지 카드 더미 데이터 ───────────────────────────
  카드 1장 = 이미지 ^ placeId ^ place_type(stay/food/tour) ^ 업소명 ^ 지역 ^ 평점 ^ 리뷰수 ^ 가격(숫자) ^ 해시태그(콤마구분)
  카드끼리는 | 로 구분

  ※ 실제 데이터를 붙일 때는 이 c:set을 지우고
     아래 c:forEach를 items="${popularPlaceList}"로 바꾸면 된다.
--%>
<c:set var="popularPlaceSlides" value="
https://picsum.photos/id/1074/400/300^101^stay^제주 오션뷰 리조트^제주^4.8^1200^250000^오션뷰,스파|
https://picsum.photos/id/1076/400/300^102^stay^해운대 비치 호텔^부산^4.6^800^190000^해운대,비즈니스|
https://picsum.photos/id/1080/400/300^103^stay^경주 한옥스테이^경주^4.7^300^120000^한옥,전통|
https://picsum.photos/id/292/400/300^104^food^제주 흑돼지 맛집^제주^4.9^2100^45000^흑돼지,현지맛집|
https://picsum.photos/id/365/400/300^105^food^전주 한정식 본가^전주^4.8^1500^38000^한정식,전통음식|
https://picsum.photos/id/431/400/300^106^food^광안리 해산물 식당^부산^4.6^900^42000^해산물,오션뷰|
https://picsum.photos/id/1043/400/300^107^tour^성산일출봉^제주^4.9^5000^3000^자연,일출명소|
https://picsum.photos/id/1028/400/300^108^tour^불국사^경주^4.8^4200^6000^유네스코,역사
" />

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>여행 커뮤니티 메인</title>

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

<%-- ===================== 검색 히어로 ===================== --%>
<div class="main-hero">
    <div class="main-hero-inner">
        <p class="main-hero-eyebrow">갈래말래</p>
        <h1 class="main-hero-title">어디로 떠나고 싶으세요?</h1>
        <p class="main-hero-subtitle">숙박, 맛집, 여행지를 한 번에 검색하세요</p>

        <div class="main-hero-search">
            <%-- TODO: tour 도메인 검색 파라미터 확정되면 action/name 값 맞추기 (header.jsp 지역 드롭다운과 동일 사유) --%>
            <form action="${cp}/tour/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                    <jsp:param name="useDropdown" value="true" />
                    <jsp:param name="listAttr" value="parentRegionList" />
                    <jsp:param name="selectedAttr" value="selectedRegionId" />
                    <jsp:param name="placeholder" value="여행지, 숙소, 맛집 통합 검색" />
                    <jsp:param name="width" value="600px" />
                </jsp:include>
                <!-- index 에서의 검색은 전체 검색이므로 placeType은 빈값으로 전달 (결과적으로 TourController에서 전체조회로 처리됨) -->
                <input type="hidden" name="placeType" value="" />
            </form>
        </div>

        <div class="main-hero-pills">
            <c:forEach var="region" items="${fn:split('서울,부산,제주,경주,전주,강릉', ',')}">
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="${region}" />
                    <jsp:param name="onclick" value="location.href='${cp}/tour/list?areaName=${region}'" />
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

    <%-- ===================== 인기 여행지 캐러셀 ===================== --%>

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
                    <div class="destination-slide" onclick="location.href='${cp}/tour/list?areaName=${fn:trim(col[1])}'">
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

    <%-- ===================== 숙박/맛집/관광지 목록 ===================== --%>
    <div class="main-section">
        <div class="main-section-head">
            <div class="main-tab-group" data-main-tabs>
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="숙박" />
                    <jsp:param name="isActive" value="true" />
                    <jsp:param name="style" value="" />
                </jsp:include>
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="맛집" />
                </jsp:include>
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="관광지" />
                </jsp:include>
            </div>
            <a href="${cp}/tour/list" class="main-section-more">전체보기 →</a>
        </div>

        <%-- data-category는 탭 필터용 값이라 selectableButton.jsp의 표시 텍스트(숙박/맛집/관광지)와
             cardComponent가 쓰는 place_type(stay/food/tour) 사이를 여기서 직접 이어준다 --%>
        <script>
            (function () {
                var labelToType = { "숙박": "stay", "맛집": "food", "관광지": "tour" };
                document.querySelectorAll('[data-main-tabs] .btn-selectable').forEach(function (btn) {
                    var label = btn.querySelector('.btn-selectable-text').textContent.trim();
                    btn.dataset.category = labelToType[label];
                });
            })();
        </script>

        <div class="main-card-grid">
            <c:forEach var="row" items="${fn:split(popularPlaceSlides, '|')}">
                <c:set var="col" value="${fn:split(row, '^')}" />
                <fmt:formatNumber var="priceText" value="${fn:trim(col[7])}" pattern="#,###" />
                <div data-main-card data-category="${fn:trim(col[2])}">
                    <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                        <jsp:param name="firstimage"  value="${fn:trim(col[0])}" />
                        <jsp:param name="placeId"     value="${fn:trim(col[1])}" />
                        <jsp:param name="place_type"  value="${fn:trim(col[2])}" />
                        <jsp:param name="name"        value="${fn:trim(col[3])}" />
                        <jsp:param name="regionName"  value="${fn:trim(col[4])}" />
                        <jsp:param name="rating"      value="${fn:trim(col[5])}" />
                        <jsp:param name="reviewCount" value="${fn:trim(col[6])}" />
                        <jsp:param name="price"       value="${priceText}원" />
                        <jsp:param name="hashTags"    value="${fn:trim(col[8])}" />
                    </jsp:include>
                </div>
            </c:forEach>
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

<%@ include file="../common/footer.jsp" %>

<script src="${cp}/js/main/main.js"></script>

</body>
</html>
