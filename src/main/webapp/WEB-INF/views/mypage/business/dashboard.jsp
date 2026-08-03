<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="사업자 마이페이지"/></jsp:include>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/mypage/business.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">사업자 마이페이지</h1>
    <c:if test="${not empty message}"><div class="biz-message"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="biz-message biz-message--error"><c:out value="${error}"/></div></c:if>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="dashboard"/></jsp:include>
        <section class="mypage-content" aria-labelledby="place-title">
            <div class="mypage-content__header">
                <h2 id="place-title">내 사업장 <em>(<c:out value="${empty places ? 0 : places.size()}"/>)</em></h2>
                <span class="business-approval-badge ${application.status eq 'APPROVED' ? 'is-approved' : ''}">
                    <c:out value="${empty application ? '신청정보 없음' : application.status}"/>
                </span>
            </div>
            <c:choose>
                <c:when test="${empty places}">
                    <div class="mypage-empty-state"><strong>등록된 사업장이 없습니다.</strong><span>사업장 등록은 담당 기능 또는 관리자에게 문의해 주세요.</span></div>
                </c:when>
                <c:otherwise>
                    <div class="business-place-list">
                        <c:forEach var="place" items="${places}">
                            <article class="business-place-card">
                                <div class="business-place-card__head">
                                    <div><span class="biz-badge"><c:out value="${place.placeType}"/></span><h3><c:out value="${place.name}"/></h3><p><c:out value="${place.address}" default="주소 정보 없음"/></p></div>
                                    <span><c:out value="${place.closed ? '영업 마감' : '영업 중'}"/></span>
                                </div>
                                <p class="business-place-description"><c:out value="${place.description}" default="사업장 설명이 없습니다."/></p>
                                <div class="business-place-gallery">
                                    <c:if test="${not empty place.firstImage}"><img src="<c:out value='${place.firstImage}'/>" alt="${place.name} 대표 이미지"></c:if>
                                    <c:forEach var="image" items="${place.images}"><img src="<c:out value='${image.imageUrl}'/>" alt="${place.name} 사업장 이미지"></c:forEach>
                                </div>
                                <form class="business-image-upload" action="${pageContext.request.contextPath}/business/mypage/places/${place.placeId}/images" method="post" enctype="multipart/form-data">
                                    <input name="placeImage" type="file" required accept=".jpg,.jpeg,.png,.webp">
                                    <button class="mypage-primary-link" type="submit">사업장 이미지 추가</button>
                                </form>
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
