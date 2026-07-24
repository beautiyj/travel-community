<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="cardImg" value="${empty param.firstimage ? 'https://storage.googleapis.com/tagjs-prod.appspot.com/v1/sdkNAxW71L/kxj3k02n_expires_30_days.png' : param.firstimage}" />
<c:set var="cardName" value="${empty param.name ? '기본 관광지 명칭' : param.name}" />
<c:set var="cardRegion" value="${empty param.regionName ? '지역' : param.regionName}" />
<%-- 평점 및 리뷰 수 기본값 처리 (평점은 삭제 가능) --%>
<c:set var="cardRating" value="${empty param.rating ? '0.0' : param.rating}" />
<c:set var="cardReviewCount" value="${empty param.reviewCount ? '0' : param.reviewCount}" />
<c:set var="cardPrice" value="${empty param.price ? '정보 없음' : param.price}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">


<div class="card-col">
    <div class="card-full-width">
        <div class="card-header-bg" style="background-image: url('${cardImg}');">
            <jsp:include page="/WEB-INF/views/common/tagButton.jsp">
                <jsp:param name="place_type" value="${param.place_type}" />
            </jsp:include>
            <jsp:include page="/WEB-INF/views/common/wishButton.jsp">
                <jsp:param name="placeId" value="${param.placeId}" />
                <jsp:param name="isBookmarked" value="${param.isBookmarked}" />
            </jsp:include>

        </div>
    </div>

    <div class="card-body">
        <div class="card-info-col">
            <div class="card-full-width">
                <span class="card-txt-title">
                    ${cardName}
                </span>
            </div>
            <div class="card-row-gap4">
                <img src="https://storage.googleapis.com/tagjs-prod.appspot.com/v1/sdkNAxW71L/r52yuzhj_expires_30_days.png" class="card-img-marker" />
                <span class="card-txt-muted">
                    ${cardRegion}
                </span>
            </div>
            <div class="card-row-between">
                <div class="card-row-gap5">
                    <%-- 별점 --%>
                    <div class="card-row-gap3">
                        <img src="https://storage.googleapis.com/tagjs-prod.appspot.com/v1/sdkNAxW71L/89jjs6gg_expires_30_days.png" class="card-img-star" />
                        <span class="card-txt-warning">
                            ${cardRating}
                        </span>
                    </div>
                    <%-- 리뷰 수 --%>
                    <span class="card-txt-caption">
                        (${cardReviewCount})
                    </span>
                </div>
                <span class="card-txt-primary">
                    ${cardPrice}
                </span>
            </div>

            <div class="card-row-center">
                <c:choose>
                    <c:when test="${not empty param.hashTags}">
                        <c:set var="tagArray" value="${fn:split(param.hashTags, ',')}" />
                        <c:forEach var="tag" items="${tagArray}">
                            <div class="card-badge-sky">
                                <span class="card-txt-accent">#${fn:trim(tag)}</span>
                            </div>
                        </c:forEach>
                    </c:when>

                    <%-- 기본 태그(태그 없으면 기본값 기입) --%>
                    <c:otherwise>
                        <div class="card-badge-sky">
                            <span class="card-txt-accent">#추천</span>
                        </div>
                        <div class="card-badge-sky">
                            <span class="card-txt-accent">#핫플레이스</span>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

        </div>
    </div>
</div>