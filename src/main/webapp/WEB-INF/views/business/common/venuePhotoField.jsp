<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
업소 사진 입력 영역 (등록 폼 / 수정 폼 공용)
사진 개수 헤더 + 그리드 + 드롭존 + 안내문구가 두 폼에서 동일해 여기로 합쳤다.

사용법 (jsp:param으로 전달하는 값):
- mode : 'edit'이면 수정 폼용. 요청 스코프의 placeDetail.images로 기존 사진 카드를 먼저 깔고,
         드래그로 정한 최종 순서를 photoOrder로 제출한다.
         그 외(미전달)면 등록 폼용 빈 그리드 + 사진 1장 필수(required)로 렌더링한다.

※ 그리드/카운트는 business-venue.js가 id가 아닌 공용 class(.venue-photo-grid / .venue-photos-count)로
   찾으므로 두 폼에서 같은 마크업을 그대로 써도 된다.
--%>
<c:set var="isEdit" value="${param.mode == 'edit'}" />
<c:set var="photoCount" value="${isEdit ? placeDetail.images.size() : 0}" />

<div class="business-form-group">
    <div class="venue-photo-header">
        <label class="business-form-label">업체 사진 <span class="venue-photo-header__count">(<span class="venue-photos-count">${photoCount}</span>/5)</span></label>
        <span class="venue-photo-header__hint">첫 번째 사진이 대표 이미지로 사용됩니다</span>
    </div>

    <div class="venue-photo-grid"<c:if test="${isEdit}"> data-order-field="photoOrder"</c:if>>
        <c:if test="${isEdit}">
            <c:forEach var="img" items="${placeDetail.images}" varStatus="loop">
                <div class="venue-photo-grid__item venue-photo-grid__item--existing${loop.index == 0 ? ' venue-photo-grid__item--main' : ''}" draggable="true">
                    <img src="${img}" alt="사진 ${loop.index + 1}" />
                    <span class="venue-photo-grid__badge">대표</span>
                    <button type="button" class="venue-photo-preview__remove" aria-label="사진 삭제">×</button>
                    <input type="checkbox" name="removeImageUrls" value="${img}" hidden />
                    <input type="hidden" name="photoOrder" value="${img}" />
                </div>
            </c:forEach>
        </c:if>
    </div>

    <jsp:include page="photoDropzone.jsp">
        <jsp:param name="inputName" value="${isEdit ? 'newImages' : 'images'}" />
        <jsp:param name="remaining" value="${5 - photoCount}" />
        <jsp:param name="isRequired" value="${not isEdit}" />
        <jsp:param name="isHidden" value="${photoCount >= 5}" />
    </jsp:include>

    <p class="venue-photo-caption">드래그해서 사진 순서를 바꿀 수 있습니다. 사진에 마우스를 올리면 삭제 버튼이 나타납니다.</p>
</div>
