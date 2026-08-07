<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <title>${place.name} - 상세 정보</title>

        <!-- 공통 및 컴포넌트 CSS -->
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

        <jsp:include page="/WEB-INF/views/common/header.jsp" />
        <div class="detail-container">

            <a href="${pageContext.request.contextPath}/tour/list?placeType=${place.placeType}" class="back-link">
                &lt; 목록으로
            </a>

            <!-- 최상단 배너 (기존 banner.jsp 슬라이더 기능 100% 유지 + 크게보기 버튼) -->
            <div class="detail-top-banner-wrapper" id="bannerWrapper">
                <jsp:include page="/WEB-INF/views/common/banner.jsp">
                    <jsp:param name="bannerId" value="tourDetailBanner" />
                </jsp:include>
                <button type="button" class="btn-banner-zoom" id="btnBannerZoom">
                    🔍 크게 보기
                </button>
            </div>

            <!-- 배너 아래 좌우 2컬럼 레이아웃 시작 -->
            <div class="detail-layout">

                <!-- 좌측 메인 영역 -->
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
                        <span class="star-icon">⭐</span>
                        <span class="place-rating-count">리뷰 0개</span>
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

                    <!-- 지도 영역 -->
                    <div class="map-box">
                        <div class="map-box-title">🗺️ 지도 위치 안내</div>
                        <div class="map-box-addr">${place.address}</div>
                    </div>

                    <c:if test="${not empty extraInfoLines}">
                        <div class="extra-info">
                            <h4 class="extra-info-title">이용 안내</h4>
                            <ul class="extra-info-list">
                                <c:forEach var="line" items="${extraInfoLines}">
                                    <li>${line}</li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>

                    <!-- 리뷰 영역 -->
                    <div class="review-section">
                        <div class="review-title">여행 후기 (0)</div>
                        <div class="review-empty-box">
                            아직 후기가 없습니다. 첫 번째 후기를 남겨보세요!
                        </div>
                    </div>
                </div>

                <div class="detail-sidebar">
                    <div class="booking-card">
                        <div class="booking-price">
                            <c:choose>
                                <c:when test="${place.minPrice ne null and place.minPrice gt 0}">
                                    <fmt:formatNumber value="${place.minPrice}" pattern="#,###" />원 <span>/ 1회</span>
                                    </c:when>
                                    <c:otherwise>
                                        문의 <span>/ 변동</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <div class="booking-features">
                                <div class="feature-item">편리한 예약 가능</div>
                                <div class="feature-item">안전 결제 보장</div>
                            </div>

                            <div class="booking-action-group">
                                <c:choose>
                                    <c:when test="${not empty sessionScope.loginMember and sessionScope.loginMember.memberRole eq 'BUSINESS'}">
                                        <div class="booking-disabled-notice">
                                            사업자 계정은 찜하기 & 예약을 이용할 수 없습니다
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <!-- 좌측 20% 원형 찜하기 버튼 -->
                                        <div class="booking-wish-box">
                                            <jsp:include page="/WEB-INF/views/common/wishButton.jsp">
                                                <jsp:param name="placeId" value="${place.placeId}" />
                                                <jsp:param name="isBookmarked" value="${isBookmarked}" />
                                            </jsp:include>
                                        </div>

                                        <!-- 우측 80% 예약하기 버튼 -->
                                        <div class="booking-submit-box">
                                            <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                                                <jsp:param name="text" value="예약하기" />
                                                <jsp:param name="width" value="100%" />
                                                <jsp:param name="onclick" value="location.href='${pageContext.request.contextPath}/reservations/new?placeId=${place.placeId}'" />
                                            </jsp:include>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>


                        </div>
                    </div>

                </div>
            </div>

            <div class="img-modal-overlay" id="imageModal">
                <span class="img-modal-close" id="imageModalClose">&times;</span>
                <img class="img-modal-content" id="imageModalImg" alt="상세 이미지 크게 보기">
            </div>

            <jsp:include page="/WEB-INF/views/common/footer.jsp" />

            <script src="${pageContext.request.contextPath}/js/common.js"></script>
            <script src="${pageContext.request.contextPath}/js/tour/tourDetail.js"></script>
        </body>
    </html>