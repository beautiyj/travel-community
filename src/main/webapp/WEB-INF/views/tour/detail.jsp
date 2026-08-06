<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <title>${place.name} - 상세 정보</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/tour/detail.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/banner.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/smallButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/confirmModal.css">
    </head>
    <body>

        <!-- 1. 상단 헤더 컴포넌트 -->
        <jsp:include page="/WEB-INF/views/common/header.jsp" />

        <!-- 2. 전체 컨테이너 시작 -->
        <div class="container">

            <!-- 💡 [요청사항 1] '목록으로' 버튼은 컨테이너 최상단에 위치 -->
            <a href="${pageContext.request.contextPath}/tour/list?placeType=${place.placeType}" class="back-link">
                &lt; 목록으로
            </a>

            <!-- [디버그] placeImages 개수 : ${placeImages != null ? placeImages.size() : 'null'} -->
            <!-- 💡 [요청사항 1] 최상단 전체 너비 배너 (실제 DB 이미지 연동 필요) -->
            <div class="detail-top-banner">
                <jsp:include page="/WEB-INF/views/common/banner.jsp">
                    <jsp:param name="bannerId" value="tourDetailBanner" />
                </jsp:include>
            </div>

            <!-- 3. 배너 아래부터 좌우 2컬럼 레이아웃 시작 -->
            <div class="detail-layout">
                <!-- 👈 좌측 메인 영역 -->
                <div class="detail-main">
                    <!-- 타입 및 지역 -->
                    <div class="place-badge-group">
                        <jsp:include page="/WEB-INF/views/common/tagButton.jsp">
                            <jsp:param name="place_type" value="${place.placeType}" />
                        </jsp:include>
                        <span class="place-region">${place.address}</span>
                    </div>

                    <h1 class="place-title">${place.name}</h1>

                    <div class="place-rating-area">
                        <span class="star-icon">리뷰수</span>
                        <span class="place-rating-count">(0개)</span>
                    </div>

                    <div class="place-desc">
                        <c:choose>
                            <c:when test="${not empty place.description}">
                                ${place.description}
                            </c:when>
                            <c:otherwise>
                                등록된 상세 설명이 없습니다.
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <div class="place-address">
                        📍 ${place.address}
                    </div>

                    <c:if test="${not empty place.hashtags}">
                        <div class="hashtag-group">
                            <c:forEach var="tag" items="${place.hashtags.split(',')}">
                                <c:if test="${not empty tag}">
                                    <jsp:include page="/WEB-INF/views/common/tagButton.jsp">
                                        <jsp:param name="text" value="${tag}" />
                                    </jsp:include>
                                </c:if>
                            </c:forEach>
                        </div>
                    </c:if>

                    <div class="map-box">
                        <div class="map-box-title">🗺️ 지도 영역</div>
                        <div class="map-box-addr">${place.address}</div>
                    </div>

                    <!-- 부가정보 렌더링 -->
                    <c:if test="${not empty extraInfoLines}">
                        <div class="extra-info">
                            <h4>이용 안내</h4>
                            <ul class="extra-info-list">
                                <c:forEach var="line" items="${extraInfoLines}">
                                    <li>${line}</li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>

                    <div class="review-section">
                        <div class="review-title">여행 후기 (0)</div>
                        <div class="review-empty-box">
                            아직 후기가 없습니다. 첫 번째 후기를 남겨보세요!
                        </div>
                    </div>
                </div> <!-- 👈 detail-main 끝 -->

                <!-- 👉 우측 사이드바 영역 -->
                <div class="detail-sidebar">
                    <div class="booking-card">
                        <div class="booking-price">
                            <c:choose>
                                <c:when test="${place.minPrice ne null and place.minPrice gt 0}">
                                    <fmt:formatNumber value="${place.minPrice}" pattern="#,###" />원 <span>/ 1회</span>
                                    </c:when>
                                    <c:otherwise>
                                        가격 문의 <span>/ 정보 변동</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <div class="booking-features">
                                <div class="feature-item">메세지0804</div>
                                <div class="feature-item">메시지조정필요함</div>
                            </div>

                            <div class="booking-action-group">
                                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                                    <jsp:param name="text" value="예약하기" />
                                    <jsp:param name="width" value="100%" />
                                    <jsp:param name="onclick" value="location.href='${pageContext.request.contextPath}/reservations/new?placeId=${place.placeId}'" />
                                </jsp:include>

                                <div class="booking-wish-box">
                                    <jsp:include page="/WEB-INF/views/common/wishButton.jsp">
                                        <jsp:param name="placeId" value="${place.placeId}" />
                                        <jsp:param name="isBookmarked" value="${isBookmarked}" />
                                    </jsp:include>
                                    <span class="booking-wish-text">찜하기</span>
                                </div>
                            </div>
                        </div>
                    </div> <!-- 👈 detail-sidebar 끝 -->

                </div> <!-- 👈 detail-layout 끝 -->
            </div> <!-- 👈 container 끝 -->

            <!-- 하단 푸터 -->
            <jsp:include page="/WEB-INF/views/common/footer.jsp" />

            <script src="${pageContext.request.contextPath}/js/common.js"></script>
        </body>
    </html>