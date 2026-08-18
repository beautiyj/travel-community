<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <script src="${pageContext.request.contextPath}/js/csrf.js"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>찜목록</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/mypage/user.css">
    <link rel="stylesheet" href="/css/mypage/modal.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
</head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">찜목록</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp"><jsp:param name="accountType" value="USER"/><jsp:param name="active" value="wishlist"/></jsp:include>
        <section class="mypage-content">
            <div class="mypage-content__header"><h2 id="wishlist-title"><em id="wishlistCount">(${empty wishlist ? 0 : wishlist.size()})</em></h2></div>
            <c:choose>
                <c:when test="${empty wishlist}">
                    <div class="mypage-empty-state"><strong>아직 찜한 항목이 없습니다</strong><a href="${pageContext.request.contextPath}/">여행지 탐색하기</a></div>
                </c:when>
                <c:otherwise>
                    <div class="wishlist-grid">
                        <c:forEach var="wish" items="${wishlist}">
<a href="${pageContext.request.contextPath}/tour/detail?placeId=${wish.placeId}" class="wishlist-item-link">
                                <article class="wishlist-item">
                                    <div class="wishlist-item__thumbnail" aria-hidden="true"
                                         <c:if test="${not empty wish.firstImage}">style="background-image:url('${wish.firstImage}');"</c:if>>
                                        <c:if test="${empty wish.firstImage}">
                                            <span><c:out value="${wish.placeName}" default="장소" /></span>
                                        </c:if>
                                    </div>
                                    <div class="wishlist-item__body">
                                        <h3>
                                            <c:choose>
                                                <c:when test="${not empty wish.placeName}">
                                                    <c:out value="${wish.placeName}" />
                                                </c:when>
                                                <c:otherwise>장소 #<c:out value="${wish.placeId}" /></c:otherwise>
                                            </c:choose>
                                        </h3>
                                        <p>찜한 날짜 <c:out value="${fn:replace(wish.createdAt, 'T', ' ')}" default="-" /></p>
                                    </div>
                                    <jsp:include page="/WEB-INF/views/common/wishButton.jsp">
                                        <jsp:param name="placeId" value="${wish.placeId}"/>
                                        <jsp:param name="isBookmarked" value="true"/>
                                    </jsp:include>
                                </article>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</main>

<script>
    window.__CONTEXT_PATH__ = "${pageContext.request.contextPath}";
</script>
<script src="${pageContext.request.contextPath}/js/common.js"></script>
</body>
</html>
