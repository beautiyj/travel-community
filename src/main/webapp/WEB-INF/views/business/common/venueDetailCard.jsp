<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%--
업소 정보 읽기 뷰 (수정 폼이 아닐 때의 업소 관리 화면)
요청 스코프의 placeDetail을 그대로 읽는다.
--%>
<c:choose>
    <c:when test="${placeDetail.placeType == 'stay'}"><c:set var="categoryLabel" value="숙박"/></c:when>
    <c:when test="${placeDetail.placeType == 'food'}"><c:set var="categoryLabel" value="맛집"/></c:when>
    <c:otherwise><c:set var="categoryLabel" value="관광지"/></c:otherwise>
</c:choose>

<%-- venue-detail-row__value는 소개글 줄바꿈을 살리려고 white-space: pre-line이라,
     값을 span 안에서 바로 c:choose로 분기하면 태그 사이 개행까지 그대로 렌더링되어
     세로로 빈 줄이 생긴다. categoryLabel과 같은 방식으로 밖에서 값만 만들어 넣는다 --%>
<c:choose>
    <c:when test="${not empty placeDetail.minPrice && placeDetail.minPrice > 0}">
        <fmt:formatNumber var="priceAmount" value="${placeDetail.minPrice}" pattern="#,###" />
        <c:set var="priceLabel" value="${priceAmount}원 / 1인"/>
    </c:when>
    <c:otherwise><c:set var="priceLabel" value="무료"/></c:otherwise>
</c:choose>

<div class="business-card venue-panel">
    <div class="venue-header">
        <h2 class="business-card__title" style="margin:0;">업소 정보</h2>
        <c:url value="/business/venue" var="editUrl">
            <c:param name="edit" value="true" />
        </c:url>
        <a href="${editUrl}" class="business-btn business-btn--primary business-btn--sm">정보 수정</a>
    </div>

    <c:if test="${not empty placeDetail.images}">
        <div class="venue-gallery">
            <c:forEach var="img" items="${placeDetail.images}" varStatus="loop">
                <%-- 클릭 시 아래 라이트박스(#venueImageLightbox)를 큰 이미지로 채워 연다 (business-venue-gallery.js) --%>
                <div class="venue-gallery__item${loop.index == 0 ? ' venue-gallery__item--main' : ''} js-venue-gallery-trigger">
                    <img src="${img}" alt="업소 사진 ${loop.index + 1}" />
                    <c:if test="${loop.index == 0}">
                        <span class="venue-gallery__badge">대표사진</span>
                    </c:if>
                </div>
            </c:forEach>
        </div>

        <%-- 업소 사진 확대보기 라이트박스: confirmModal.css의 .modal-overlay/.modal, common.js의
             openModal/closeModal(오버레이 클릭·ESC·닫기 버튼 처리 포함)을 그대로 재사용한다 --%>
        <div id="venueImageLightbox" class="modal-overlay venue-lightbox" data-modal
             role="dialog" aria-modal="true" aria-label="업소 사진 확대보기">
            <div class="modal venue-lightbox__modal">
                <button type="button" class="venue-lightbox__close" data-modal-close aria-label="닫기">&times;</button>
                <img class="venue-lightbox__img" id="venueLightboxImg" src="" alt="" />
            </div>
        </div>
    </c:if>

    <div class="venue-detail-list">
        <div class="venue-detail-row">
            <span class="venue-detail-row__label">업소명</span>
            <span class="venue-detail-row__value">${placeDetail.name}</span>
        </div>
        <div class="venue-detail-row">
            <span class="venue-detail-row__label">카테고리</span>
            <span class="venue-detail-row__value">${categoryLabel}</span>
        </div>
        <%-- 가격은 숙박만 설정 대상이라 숙박일 때만 보여준다 --%>
        <c:if test="${placeDetail.placeType == 'stay'}">
            <div class="venue-detail-row">
                <span class="venue-detail-row__label">가격</span>
                <span class="venue-detail-row__value">${priceLabel}</span>
            </div>
        </c:if>
        <div class="venue-detail-row">
            <span class="venue-detail-row__label">주소</span>
            <span class="venue-detail-row__value">${placeDetail.address}<c:if test="${not empty placeDetail.addressDetail}"> ${placeDetail.addressDetail}</c:if></span>
        </div>
        <div class="venue-detail-row">
            <span class="venue-detail-row__label">소개</span>
            <span class="venue-detail-row__value">${placeDetail.description}</span>
        </div>
        <%-- 부가정보는 "[라벨] 값" 줄들을 서비스에서 "라벨 -> 값"으로 풀어둔 것(extraInfoMap)을 저장 순서대로
             목록으로 보여준다. 공공데이터로 들어온 장소의 상세 화면과 같은 방식이다. --%>
        <c:if test="${not empty placeDetail.extraInfoMap}">
            <div class="venue-detail-row">
                <span class="venue-detail-row__label">부가정보</span>
                <%-- 목록 마크업의 개행이 그대로 빈 줄이 되지 않도록 pre-line을 끄는 --list 변형을 쓴다
                     (파일 상단의 categoryLabel 주석과 같은 이유) --%>
                <span class="venue-detail-row__value venue-detail-row__value--list">
                    <ul class="venue-extra-list">
                        <c:forEach var="entry" items="${placeDetail.extraInfoMap}">
                            <li><span class="venue-extra-list__label">${entry.key}</span>${entry.value}</li>
                        </c:forEach>
                    </ul>
                </span>
            </div>
        </c:if>
        <c:if test="${not empty placeDetail.hashtags}">
            <div class="venue-detail-row">
                <span class="venue-detail-row__label">해시태그</span>
                <span class="venue-detail-row__value"><c:forEach var="tag" items="${placeDetail.hashtags.split(',')}"><span class="venue-hashtag">${tag}</span></c:forEach></span>
            </div>
        </c:if>
    </div>
</div>
