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

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="regionId">지역</label>
                                <select class="admin-form-select" id="regionId" name="regionId">
                                    <option value="">선택 안함</option>
                                    <c:forEach var="r" items="${regionOptions}">
                                        <option value="${r.regionId}">${r.regionName}</option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="address">주소</label>
                                <input class="admin-form-input" type="text" id="address" name="address" required />
                            </div>

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="description">소개</label>
                                <textarea class="admin-form-textarea" id="description" name="description" rows="4"></textarea>
                            </div>

                            <div class="admin-form-group">
                                <label class="admin-form-label" for="images">사진</label>
                                <input class="admin-form-input" type="file" id="images" name="images" multiple accept="image/*" required />
                            </div>

                            <div class="admin-form-actions">
                                <button class="admin-btn admin-btn--primary" type="submit">업소 등록</button>
                            </div>
                        </form>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="admin-card">
                        <h2 class="admin-card__title">등록된 업소 정보</h2>
                        <c:if test="${not empty place.firstImage}">
                            <img src="${place.firstImage}" alt="${place.placeName}" style="max-width:240px; border-radius:12px; margin-bottom:12px;" />
                        </c:if>
                        <p><strong>${place.placeName}</strong></p>
                        <p class="admin-reservation-row__meta">대표자: ${place.ownerName}</p>
                        <p class="admin-reservation-row__meta">
                            상태: <c:choose>
                                <c:when test="${place.closed}">예약 마감</c:when>
                                <c:otherwise>예약 운영중</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

</body>
</html>
