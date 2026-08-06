<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>부산 해운대 패키지 - 2인 특별 혜택</title>

    <link rel="stylesheet" href="${cp}/css/common.css">
    <link rel="stylesheet" href="${cp}/css/components/tagButton.css">
    <link rel="stylesheet" href="${cp}/css/event.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<%-- ===================== 이벤트 히어로 ===================== --%>
<div class="event-hero" style="background-image: url('https://picsum.photos/id/1036/1600/500');">
    <div class="event-hero-inner">
        <jsp:include page="/WEB-INF/views/common/tagButton.jsp">
            <jsp:param name="text" value="해변 여행" />
        </jsp:include>
        <h1 class="event-hero-title">부산 해운대 패키지<br>2인 특별 혜택</h1>
        <p class="event-hero-subtitle">숙박 + 레스토랑 결합 시 10% 추가 할인</p>
    </div>
</div>

<div class="event-container">

    <%-- ===================== 이벤트 기본 정보 ===================== --%>
    <div class="event-section">
        <h2 class="event-section-title">이벤트 정보</h2>
        <div class="event-info-grid">
            <div class="event-info-card">
                <div class="event-info-card-label">이벤트 기간</div>
                <div class="event-info-card-value">2026.08.05 ~ 2026.09.30</div>
            </div>
            <div class="event-info-card">
                <div class="event-info-card-label">이용 인원</div>
                <div class="event-info-card-value">2인 기준 (기준 인원 초과 시 별도 요금)</div>
            </div>
            <div class="event-info-card">
                <div class="event-info-card-label">지역</div>
                <div class="event-info-card-value">부산 해운대</div>
            </div>
            <div class="event-info-card">
                <div class="event-info-card-label">할인 혜택</div>
                <div class="event-info-card-value">숙박 + 레스토랑 결합 예약 시 10% 추가 할인</div>
            </div>
        </div>
    </div>

    <%-- ===================== 포함 내역 ===================== --%>
    <div class="event-section">
        <h2 class="event-section-title">포함 내역</h2>
        <ul class="event-list">
            <li>해운대 인근 숙소 2박 숙박권</li>
            <li>조식 2인 이용권</li>
            <li>제휴 레스토랑 할인 쿠폰 (결합 예약 시 10% 추가 할인)</li>
        </ul>
    </div>

    <%-- ===================== 유의사항 ===================== --%>
    <div class="event-section">
        <h2 class="event-section-title">유의사항</h2>
        <div class="event-notice">
            본 이벤트는 안내 페이지이며 실제 예약 가능 여부와 최종 가격은 각 숙소·레스토랑 페이지에서 확인해 주세요.<br>
            할인 혜택은 숙박과 레스토랑을 함께 결합 예약하는 경우에만 적용됩니다.<br>
            이벤트 내용은 시즌 및 제휴 업체 사정에 따라 변경될 수 있습니다.
        </div>
    </div>

</div>

<%@ include file="../common/footer.jsp" %>

</body>
</html>
