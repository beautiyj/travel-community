<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>업소 관리 - 관리자 - 갈래말래</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/business.css">
    <script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="venue" />
    </jsp:include>

    <div class="business-main">
        <jsp:include page="common/topbar.jsp">
            <jsp:param name="title" value="업소 관리" />
        </jsp:include>

        <div class="business-content">
            <%-- 업소 미등록(일반회원 / 사업자) · 수정 폼 · 읽기 뷰 네 가지 화면을 한 경로에서 분기한다 --%>
            <c:choose>
                <c:when test="${empty place && !canRegister}">
                    <div class="business-card">
                        <h2 class="business-card__title">업소 등록</h2>
                        <p class="business-empty">일반 회원은 업소를 등록할 수 없습니다. 사업자 회원만 업소 등록이 가능합니다.</p>
                    </div>
                </c:when>

                <c:when test="${empty place}">
                    <div class="business-card">
                        <h2 class="business-card__title">업소 등록</h2>
                        <p class="business-empty">아직 등록된 업소가 없습니다. 아래 정보를 입력해 업소를 등록해주세요.</p>

                        <form class="business-form" action="/business/venue/register" method="post" enctype="multipart/form-data">
                            <jsp:include page="common/venueFormFields.jsp" />
                            <jsp:include page="common/venuePhotoField.jsp" />

                            <div class="business-form-actions">
                                <button class="business-btn business-btn--primary" type="submit">업소 등록</button>
                            </div>
                        </form>
                    </div>
                </c:when>

                <c:when test="${editing}">
                    <div class="business-card venue-panel">
                        <h2 class="business-card__title">업소 정보 수정</h2>

                        <form class="business-form" action="/business/venue/update" method="post" enctype="multipart/form-data">
                            <%-- address를 안 넘기면 수정 화면에서 주소가 빈 값으로 뜨고,
                                 business-venue.js의 제출 검증에 걸려 매번 주소를 다시 검색해야 한다 --%>
                            <jsp:include page="common/venueFormFields.jsp">
                                <jsp:param name="idPrefix" value="edit-" />
                                <jsp:param name="name" value="${placeDetail.name}" />
                                <jsp:param name="placeType" value="${placeDetail.placeType}" />
                                <jsp:param name="priceType" value="${placeDetail.priceType}" />
                                <jsp:param name="minPrice" value="${placeDetail.minPrice}" />
                                <jsp:param name="address" value="${placeDetail.address}" />
                                <jsp:param name="addressDetail" value="${placeDetail.addressDetail}" />
                                <jsp:param name="description" value="${placeDetail.description}" />
                                <jsp:param name="hashtags" value="${placeDetail.hashtags}" />
                            </jsp:include>

                            <jsp:include page="common/venuePhotoField.jsp">
                                <jsp:param name="mode" value="edit" />
                            </jsp:include>

                            <div class="venue-form-actions">
                                <a href="/business/venue" class="venue-btn venue-btn--outline">취소</a>
                                <button class="venue-btn venue-btn--solid" type="submit">저장</button>
                            </div>
                        </form>
                    </div>
                </c:when>

                <c:otherwise>
                    <jsp:include page="common/venueDetailCard.jsp" />
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- 사진 그리드·가격 토글·주소 검색은 입력 폼에서만 쓰인다 --%>
<c:if test="${(empty place && canRegister) || editing}">
    <script src="/js/business/business-venue.js"></script>
</c:if>

</body>
</html>
