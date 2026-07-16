<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>업소 관리 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/business.css">
    <script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="venue" />
    </jsp:include>

    <div class="business-main">
        <div class="business-topbar">
            <h1 class="business-topbar__title">업소 관리</h1>
        </div>

        <div class="business-content">
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
                            <input type="hidden" name="memberId" value="${memberId}" />

                            <jsp:include page="common/venueFormFields.jsp" />

                            <div class="business-form-group">
                                <div class="venue-photo-header">
                                    <label class="business-form-label">업체 사진 <span class="venue-photo-header__count">(<span class="venue-photos-count" id="venue-photos-count">0</span>/5)</span></label>
                                    <span class="venue-photo-header__hint">첫 번째 사진이 대표 이미지로 사용됩니다</span>
                                </div>

                                <div id="venue-photos-grid" class="venue-photo-grid"></div>

                                <jsp:include page="common/photoDropzone.jsp">
                                    <jsp:param name="inputName" value="images" />
                                    <jsp:param name="remaining" value="5" />
                                    <jsp:param name="isRequired" value="true" />
                                </jsp:include>

                                <p class="venue-photo-caption">드래그해서 사진 순서를 바꿀 수 있습니다. 사진에 마우스를 올리면 삭제 버튼이 나타납니다.</p>
                            </div>

                            <div class="business-form-actions">
                                <button class="business-btn business-btn--primary" type="submit">업소 등록</button>
                            </div>
                        </form>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test="${editing}">
                            <div class="business-card venue-panel">
                                <h2 class="business-card__title">업소 정보 수정</h2>

                                <form class="business-form" action="/business/venue/update" method="post" enctype="multipart/form-data">
                                    <input type="hidden" name="memberId" value="${memberId}" />

                                    <jsp:include page="common/venueFormFields.jsp">
                                        <jsp:param name="idPrefix" value="edit-" />
                                        <jsp:param name="name" value="${placeDetail.name}" />
                                        <jsp:param name="placeType" value="${placeDetail.placeType}" />
                                        <jsp:param name="description" value="${placeDetail.description}" />
                                    </jsp:include>

                                    <div class="business-form-group">
                                        <div class="venue-photo-header">
                                            <label class="business-form-label">업체 사진 <span class="venue-photo-header__count">(<span class="venue-photos-count" id="venue-photos-count-edit">${placeDetail.images.size()}</span>/5)</span></label>
                                            <span class="venue-photo-header__hint">첫 번째 사진이 대표 이미지로 사용됩니다</span>
                                        </div>

                                        <div id="venue-photos-grid-edit" class="venue-photo-grid" data-order-field="photoOrder">
                                            <c:forEach var="img" items="${placeDetail.images}" varStatus="loop">
                                                <div class="venue-photo-grid__item venue-photo-grid__item--existing${loop.index == 0 ? ' venue-photo-grid__item--main' : ''}" draggable="true">
                                                    <img src="${img}" alt="사진 ${loop.index + 1}" />
                                                    <span class="venue-photo-grid__badge">대표</span>
                                                    <button type="button" class="venue-photo-preview__remove" aria-label="사진 삭제">×</button>
                                                    <input type="checkbox" name="removeImageUrls" value="${img}" hidden />
                                                    <input type="hidden" name="photoOrder" value="${img}" />
                                                </div>
                                            </c:forEach>
                                        </div>

                                        <jsp:include page="common/photoDropzone.jsp">
                                            <jsp:param name="inputName" value="newImages" />
                                            <jsp:param name="remaining" value="${5 - placeDetail.images.size()}" />
                                            <jsp:param name="isHidden" value="${placeDetail.images.size() >= 5}" />
                                        </jsp:include>

                                        <p class="venue-photo-caption">드래그해서 사진 순서를 바꿀 수 있습니다. 사진에 마우스를 올리면 삭제 버튼이 나타납니다.</p>
                                    </div>

                                    <div class="venue-form-actions">
                                        <a href="/business/venue?memberId=${memberId}" class="venue-btn venue-btn--outline">취소</a>
                                        <button class="venue-btn venue-btn--solid" type="submit">저장</button>
                                    </div>
                                </form>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="business-card venue-panel">
                                <div class="venue-header">
                                    <h2 class="business-card__title" style="margin:0;">업소 정보</h2>
                                    <c:url value="/business/venue" var="editUrl">
                                        <c:param name="memberId" value="${memberId}" />
                                        <c:param name="edit" value="true" />
                                    </c:url>
                                    <a href="${editUrl}" class="business-btn business-btn--primary business-btn--sm">정보 수정</a>
                                </div>

                                <c:if test="${not empty placeDetail.images}">
                                    <div class="venue-gallery">
                                        <c:forEach var="img" items="${placeDetail.images}" varStatus="loop">
                                            <div class="venue-gallery__item${loop.index == 0 ? ' venue-gallery__item--main' : ''}">
                                                <img src="${img}" alt="업소 사진 ${loop.index + 1}" />
                                                <c:if test="${loop.index == 0}">
                                                    <span class="venue-gallery__badge">대표사진</span>
                                                </c:if>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <c:choose>
                                    <c:when test="${placeDetail.placeType == 1}"><c:set var="categoryLabel" value="숙박"/></c:when>
                                    <c:when test="${placeDetail.placeType == 2}"><c:set var="categoryLabel" value="맛집"/></c:when>
                                    <c:otherwise><c:set var="categoryLabel" value="관광지"/></c:otherwise>
                                </c:choose>

                                <div class="venue-detail-list">
                                    <div class="venue-detail-row">
                                        <span class="venue-detail-row__label">업소명</span>
                                        <span class="venue-detail-row__value">${placeDetail.name}</span>
                                    </div>
                                    <div class="venue-detail-row">
                                        <span class="venue-detail-row__label">카테고리</span>
                                        <span class="venue-detail-row__value">${categoryLabel}</span>
                                    </div>
<%--                                    <div class="venue-detail-row">--%>
<%--                                        <span class="venue-detail-row__label">지역</span>--%>
<%--                                        <span class="venue-detail-row__value">${placeDetail.regionName}</span>--%>
<%--                                    </div>--%>
                                    <div class="venue-detail-row">
                                        <span class="venue-detail-row__label">주소</span>
                                        <span class="venue-detail-row__value">${placeDetail.address}</span>
                                    </div>
                                    <div class="venue-detail-row">
                                        <span class="venue-detail-row__label">소개</span>
                                        <span class="venue-detail-row__value">${placeDetail.description}</span>
                                    </div>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<c:if test="${(empty place && canRegister) || editing}">
    <script src="/js/business-venue.js"></script>
</c:if>

</body>
</html>
