<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/common/pageHead.jsp">
        <jsp:param name="title" value="내 사업장 목록"/>
    </jsp:include>
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp">
            <jsp:param name="active" value="places"/>
        </jsp:include>

        <section class="mypage-content" aria-labelledby="my-places-title">
            <div class="mypage-content__header">
                <h2 id="my-places-title">
                    내 사업장 목록
                    <em>(<c:out value="${empty places ? 0 : places.size()}"/>)</em>
                </h2>
            </div>

            <c:choose>
                <c:when test="${empty places}">
                    <div class="mypage-empty-state">
                        <strong>등록된 사업장이 없습니다.</strong>
                        <span>사업장 등록과 관리는 사업자 관리 페이지에서 진행해 주세요.</span>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="business-place-list">
                        <c:forEach var="place" items="${places}">
                            <article class="business-place-card">
                                <div class="business-place-card__head">
                                    <div>
                                        <span class="biz-badge">
                                            <c:out value="${place.placeType}"/>
                                        </span>
                                        <h3><c:out value="${place.name}"/></h3>
                                        <p><c:out value="${place.address}" default="주소 정보 없음"/></p>
                                    </div>
                                    <span>
                                        <c:out value="${place.closed ? '영업 종료' : '영업 중'}"/>
                                    </span>
                                </div>
                                <p class="business-place-description">
                                    <c:out value="${place.description}" default="사업장 설명이 없습니다."/>
                                </p>
                                <div class="business-place-gallery">
                                    <c:if test="${not empty place.firstImage}">
                                        <img src="<c:out value='${place.firstImage}'/>"
                                             alt="${place.name} 대표 이미지">
                                    </c:if>
                                    <c:forEach var="image" items="${place.images}">
                                        <img src="<c:out value='${image.imageUrl}'/>"
                                             alt="${place.name} 사업장 이미지">
                                    </c:forEach>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</main>
</body>
</html>
