<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>찜목록</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/mypage/user.css">
    <link rel="stylesheet" href="/css/mypage/modal.css">
</head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">찜목록</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp"><jsp:param name="accountType" value="USER"/><jsp:param name="active" value="wishlist"/></jsp:include>
        <section class="mypage-content">
            <div class="mypage-content__header"><h2 id="wishlist-title"><em>(${empty wishlist ? 0 : wishlist.size()})</em></h2></div>
            <c:choose>
                <c:when test="${empty wishlist}">
                    <div class="mypage-empty-state"><strong>아직 찜한 항목이 없습니다</strong><a href="${pageContext.request.contextPath}/">여행지 탐색하기</a></div>
                </c:when>
                <c:otherwise>
                    <div class="wishlist-grid">
                        <c:forEach var="wish" items="${wishlist}">
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
                                    <p>찜한 날짜 <c:out value="${wish.createdAt}" default="-" /></p>
                                </div>
                                <button class="wishlist-item__delete js-wishlist-delete-open"
                                        type="button" aria-label="찜 삭제"
                                        data-wishlist-id="<c:out value='${wish.wishlistId}'/>"
                                        data-place-id="<c:out value='${wish.placeId}'/>">♥</button>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</main>

<div class="cancel-modal" id="wishlistDeleteModal" hidden>
    <div class="cancel-modal__dialog"
         role="alertdialog"
         aria-modal="true"
         aria-labelledby="wishlist-delete-title"
         aria-describedby="wishlist-delete-description">
        <div class="cancel-modal__header">
            <div>
                <h2 id="wishlist-delete-title">찜목록 삭제</h2>
                <p id="wishlistDeletePlace">선택한 장소</p>
            </div>
            <button class="cancel-modal__close js-wishlist-delete-close"
                    type="button" aria-label="닫기">×</button>
        </div>
        <div class="cancel-modal__body">
            <div class="wishlist-delete-modal__icon" aria-hidden="true">!</div>
            <p id="wishlist-delete-description"
               class="wishlist-delete-modal__message">
                찜목록에서 삭제하시겠습니까?
            </p>
            <p class="cancel-modal__notice">
                삭제한 항목은 찜목록에서 사라지며,
                다시 추가하려면 해당 장소에서 찜하기를 눌러야 합니다.
            </p>
        </div>
        <form class="cancel-modal__actions"
              action="${pageContext.request.contextPath}/mypage/wishlist/delete"
              method="post">
            <input id="wishlistDeleteId" type="hidden" name="wishlistId">
            <button class="cancel-modal__back js-wishlist-delete-close"
                    type="button">돌아가기</button>
            <button class="cancel-modal__submit" type="submit">삭제하기</button>
        </form>
    </div>
</div>

<script src="/js/mypage/user/wishlist.js"></script>
</body>
</html>
