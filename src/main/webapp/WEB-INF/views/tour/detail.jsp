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
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/header.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/banner.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/smallButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/confirmModal.css">
    </head>
    <body>

        <!-- 1. 상단 헤더 컴포넌트 인클루드 -->
        <jsp:include page="/WEB-INF/views/common/header.jsp" />

        <!-- 2. 배너 컴포넌트 인클루드 -->
        <jsp:include page="/WEB-INF/views/common/banner.jsp">
            <jsp:param name="bannerId" value="detailBanner" />
        </jsp:include>

        <!-- 3. 내부 레이아웃 및 콘텐츠 영역 -->
        <div class="container">
            <a href="${pageContext.request.contextPath}/tour/list?placeType=${place.placeType}" class="back-link">
                &lt; 목록으로
            </a>

            <div class="detail-layout">
                <!-- 좌측 메인 정보 -->
                <div class="detail-main">
                    <!-- 원본 사진 비율 유지 갤러리 슬라이더 -->
                    <div class="post-gallery" data-gallery>
                        <div class="post-gallery-track" data-gallery-track>
                            <c:choose>
                                <c:when test="${not empty placeImages}">
                                    <c:forEach var="img" items="${placeImages}">
                                        <div class="post-gallery-slide">
                                            <img src="${img.imageUrl}" alt="${place.name}" class="post-gallery-img">
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="post-gallery-slide">
                                        <img src="${not empty place.firstImage ? place.firstImage : pageContext.request.contextPath.concat('/images/default-thumbnail.png')}" alt="${place.name}" class="post-gallery-img">
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <button type="button" class="btn-wish-trigger gallery-arrow-prev" data-gallery-prev aria-label="이전 사진">
                            <svg class="banner-chevron" viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"></polyline></svg>
                        </button>
                        <button type="button" class="btn-wish-trigger gallery-arrow-next" data-gallery-next aria-label="다음 사진">
                            <svg class="banner-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                        </button>

                        <div class="gallery-counter" data-gallery-counter>1 / 1</div>
                    </div>

                    <!-- 타입 및 지역 (tagButton 컴포넌트 재사용) -->
                    <div class="place-badge-group">
                        <jsp:include page="/WEB-INF/views/common/tagButton.jsp">
                            <jsp:param name="place_type" value="${place.placeType}" />
                        </jsp:include>
                        <span class="place-region">${place.address}</span>
                    </div>

                    <h1 class="place-title">${place.name}</h1>

                    <div class="place-rating-area">
                        <span class="star-icon">★</span>
                        <span>0.0</span>
                        <span class="place-rating-count">(리뷰 0개)</span>
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
                </div>

                <!-- 4. 우측 예약 및 결제 카드 영역 -->
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
                                <div class="feature-item">✔ 즉시 예약 확정</div>
                                <div class="feature-item">🛡️ 안전 결제 보장</div>
                            </div>

                            <!-- todo: 리다이렉션 추가 -->
                            <div class="booking-action-group">
                                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                                    <jsp:param name="text" value="예약하기" />
                                    <jsp:param name="width" value="100%" />
                                    <jsp:param name="onclick" value="location.href='${pageContext.request.contextPath}/tour/booking?placeId=${place.placeId}'" />
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
                    </div>
                </div>

                <!-- 0803 부가정보(주차, 휴무일, 영업시간 등) 렌더링 -->
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

                <!-- 5. 여행 후기 코너 영역 -->
                <div class="review-section">
                    <div class="review-title">여행 후기 (0)</div>
                    <div class="review-empty-box">
                        아직 후기가 없습니다. 첫 번째 후기를 남겨보세요!
                    </div>
                </div>
            </div>

            <!-- 6. 하단 푸터 컴포넌트 인클루드 -->
            <jsp:include page="/WEB-INF/views/common/footer.jsp" />

            <script src="${pageContext.request.contextPath}/js/common.js"></script>
        </body>
    </html>