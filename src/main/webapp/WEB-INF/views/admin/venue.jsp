<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>업소 관리 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body>

<div class="admin-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="venue" />
    </jsp:include>

    <div class="admin-main">
        <div class="admin-topbar">
            <h1 class="admin-topbar__title">업소 관리</h1>
        </div>

        <div class="admin-content">
            <c:choose>
                <c:when test="${empty place && !canRegister}">
                    <div class="admin-card">
                        <h2 class="admin-card__title">업소 등록</h2>
                        <p class="admin-empty">일반 회원은 업소를 등록할 수 없습니다. 사업자 회원만 업소 등록이 가능합니다.</p>
                    </div>
                </c:when>
                <c:when test="${empty place}">
                    <div class="admin-card">
                        <h2 class="admin-card__title">업소 등록</h2>
                        <p class="admin-empty">아직 등록된 업소가 없습니다. 아래 정보를 입력해 업소를 등록해주세요.</p>

                        <form class="admin-form" action="/admin/venue/register" method="post" enctype="multipart/form-data">
                            <input type="hidden" name="memberId" value="${memberId}" />

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="name">업소명</label>
                                <input class="admin-form-input" type="text" id="name" name="name" required />
                            </div>

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="placeType">업종</label>
                                <select class="admin-form-select" id="placeType" name="placeType" required>
                                    <option value="1">숙박</option>
                                    <option value="2">맛집</option>
                                    <option value="3">관광지</option>
                                </select>
                            </div>

                            <%-- 지역코드는 당분간 사업자 직접등록 업소엔 null로 두기로 해서 지역 선택 UI 잠시 주석처리
                            <div class="admin-form-group">
                                <label class="admin-form-label" for="regionId">지역</label>
                                <select class="admin-form-select" id="regionId" name="regionId">
                                    <option value="">선택 안함</option>
                                    <c:forEach var="r" items="${regionOptions}">
                                        <option value="${r.regionId}">${r.regionName}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            --%>

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="address">주소</label>
                                <input class="admin-form-input" type="text" id="address" name="address" required />
                            </div>

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="description">소개</label>
                                <textarea class="admin-form-textarea" id="description" name="description" rows="4"></textarea>
                            </div>

                            <div class="admin-form-group">
                                <div class="venue-photo-header">
                                    <label class="admin-form-label">업체 사진 <span class="venue-photo-header__count">(<span id="venue-photos-count">0</span>/5)</span></label>
                                    <span class="venue-photo-header__hint">첫 번째 사진이 대표 이미지로 사용됩니다</span>
                                </div>

                                <div id="venue-photos-grid" class="venue-photo-grid"></div>

                                <label id="venue-photos-dropzone" class="venue-photo-dropzone" for="venue-photos-input">
                                    <svg class="venue-photo-dropzone__icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M4 8a2 2 0 0 1 2-2h1.17a2 2 0 0 0 1.66-.89l.34-.51a2 2 0 0 1 1.66-.89h2.34a2 2 0 0 1 1.66.89l.34.51a2 2 0 0 0 1.66.89H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                                        <circle cx="12" cy="13" r="3.5" stroke="currentColor" stroke-width="2"/>
                                    </svg>
                                    <p class="venue-photo-dropzone__title">클릭하여 사진 추가</p>
                                    <p class="venue-photo-dropzone__hint">JPG, PNG, WEBP · 최대 10MB · 최대 <span id="venue-photos-remaining">5</span>장 추가 가능</p>
                                    <input class="venue-photo-dropzone__input" type="file" id="venue-photos-input" name="images" multiple accept="image/jpeg,image/png,image/webp" required />
                                </label>

                                <p class="venue-photo-caption">드래그해서 사진 순서를 바꿀 수 있습니다. 사진에 마우스를 올리면 삭제 버튼이 나타납니다.</p>
                            </div>

                            <div class="admin-form-actions">
                                <button class="admin-btn admin-btn--primary" type="submit">업소 등록</button>
                            </div>
                        </form>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test="${editing}">
                            <div class="admin-card venue-panel">
                                <h2 class="admin-card__title">업소 정보 수정</h2>

                                <form class="admin-form" action="/admin/venue/update" method="post" enctype="multipart/form-data">
                                    <input type="hidden" name="memberId" value="${memberId}" />

                                    <div class="admin-form-group">
                                        <label class="admin-form-label" for="edit-name">업소명</label>
                                        <input class="admin-form-input" type="text" id="edit-name" name="name" value="${placeDetail.name}" required />
                                    </div>

                                    <div class="admin-form-group">
                                        <label class="admin-form-label" for="edit-placeType">업종</label>
                                        <select class="admin-form-select" id="edit-placeType" name="placeType" required>
                                            <option value="1" ${placeDetail.placeType == 1 ? 'selected' : ''}>숙박</option>
                                            <option value="2" ${placeDetail.placeType == 2 ? 'selected' : ''}>맛집</option>
                                            <option value="3" ${placeDetail.placeType == 3 ? 'selected' : ''}>관광지</option>
                                        </select>
                                    </div>

                                    <%-- 지역코드는 당분간 사업자 직접등록 업소엔 null로 두기로 해서 지역 선택 UI 잠시 주석처리
                                    <div class="admin-form-group">
                                        <label class="admin-form-label" for="edit-regionId">지역</label>
                                        <select class="admin-form-select" id="edit-regionId" name="regionId">
                                            <option value="">선택 안함</option>
                                            <c:forEach var="r" items="${regionOptions}">
                                                <option value="${r.regionId}" ${r.regionId == placeDetail.regionId ? 'selected' : ''}>${r.regionName}</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    --%>

                                    <div class="admin-form-group">
                                        <label class="admin-form-label" for="edit-address">주소</label>
                                        <input class="admin-form-input" type="text" id="edit-address" name="address" value="${placeDetail.address}" required />
                                    </div>

                                    <div class="admin-form-group">
                                        <label class="admin-form-label" for="edit-description">소개</label>
                                        <textarea class="admin-form-textarea" id="edit-description" name="description" rows="4">${placeDetail.description}</textarea>
                                    </div>

                                    <div class="admin-form-group">
                                        <div class="venue-photo-header">
                                            <label class="admin-form-label">업체 사진 <span class="venue-photo-header__count">(<span id="venue-photos-count">${placeDetail.images.size()}</span>/5)</span></label>
                                            <span class="venue-photo-header__hint">첫 번째 사진이 대표 이미지로 사용됩니다</span>
                                        </div>

                                        <div id="venue-photos-grid" class="venue-photo-grid" data-order-field="photoOrder">
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

                                        <label id="venue-photos-dropzone" class="venue-photo-dropzone${placeDetail.images.size() >= 5 ? ' is-hidden' : ''}" for="venue-photos-input">
                                            <svg class="venue-photo-dropzone__icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                <path d="M4 8a2 2 0 0 1 2-2h1.17a2 2 0 0 0 1.66-.89l.34-.51a2 2 0 0 1 1.66-.89h2.34a2 2 0 0 1 1.66.89l.34.51a2 2 0 0 0 1.66.89H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                                                <circle cx="12" cy="13" r="3.5" stroke="currentColor" stroke-width="2"/>
                                            </svg>
                                            <p class="venue-photo-dropzone__title">클릭하여 사진 추가</p>
                                            <p class="venue-photo-dropzone__hint">JPG, PNG, WEBP · 최대 10MB · 최대 <span id="venue-photos-remaining">${5 - placeDetail.images.size()}</span>장 추가 가능</p>
                                            <input class="venue-photo-dropzone__input" type="file" id="venue-photos-input" name="newImages" multiple accept="image/jpeg,image/png,image/webp" />
                                        </label>

                                        <p class="venue-photo-caption">드래그해서 사진 순서를 바꿀 수 있습니다. 사진에 마우스를 올리면 삭제 버튼이 나타납니다.</p>
                                    </div>

                                    <div class="venue-form-actions">
                                        <a href="/admin/venue?memberId=${memberId}" class="venue-btn venue-btn--outline">취소</a>
                                        <button class="venue-btn venue-btn--solid" type="submit">저장</button>
                                    </div>
                                </form>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="admin-card venue-panel">
                                <div class="venue-header">
                                    <h2 class="admin-card__title" style="margin:0;">업소 정보</h2>
                                    <c:url value="/admin/venue" var="editUrl">
                                        <c:param name="memberId" value="${memberId}" />
                                        <c:param name="edit" value="true" />
                                    </c:url>
                                    <a href="${editUrl}" class="admin-btn admin-btn--primary admin-btn--sm">정보 수정</a>
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
    <script src="/js/admin-venue.js"></script>
</c:if>

</body>
</html>
