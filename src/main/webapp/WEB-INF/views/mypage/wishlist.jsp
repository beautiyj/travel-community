<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="찜목록" /></jsp:include></head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/components/sidebar.jsp"><jsp:param name="active" value="wishlist" /></jsp:include>
        <section class="mypage-content" aria-labelledby="wishlist-title">
            <div class="mypage-content__header"><h2 id="wishlist-title">찜목록 <em>(${empty wishlist ? 0 : wishlist.size()})</em></h2></div>
            <c:choose>
                <c:when test="${empty wishlist}">
                    <div class="mypage-empty-state"><strong>아직 찜한 항목이 없습니다</strong><a href="${pageContext.request.contextPath}/">여행지 탐색하기</a></div>
                </c:when>
                <c:otherwise>
                    <div class="wishlist-grid">
                        <c:forEach var="wish" items="${wishlist}">
                            <article class="wishlist-item">
                                <div class="wishlist-item__thumbnail" aria-hidden="true"><span>장소</span></div>
                                <div class="wishlist-item__body"><h3>장소 #<c:out value="${wish.placeId}" /></h3><p>찜한 날짜 <c:out value="${wish.createdAt}" default="-" /></p></div>
                                <form action="${pageContext.request.contextPath}/mypage/wishlist/delete" method="post" onsubmit="return confirm('찜목록에서 삭제하시겠습니까?');">
                                    <input type="hidden" name="wishlistId" value="<c:out value='${wish.wishlistId}'/>">
                                    <button class="wishlist-item__delete" type="submit" aria-label="찜 삭제">♥</button>
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
