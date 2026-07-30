<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
업소 등록/수정 폼 공통 입력 필드 (업소명 / 업종 / 주소 / 소개)

사용법 (jsp:param으로 전달하는 값):
- idPrefix    : input id 접두어 (등록: 미전달(빈 값) / 수정: "edit-")
- name        : 업소명 값 (등록은 미전달 → 빈 값, 수정은 기존 값 채움)
- placeType   : 업종 코드 값 ("stay"/"food"/"tour", 등록은 미전달)
- priceType   : 가격 유형 ("FIXED"/"VARIABLE"/"FREE", 등록은 미전달 → 가격입력이 기본 선택)
- minPrice    : 가격입력 선택 시의 금액 (등록은 미전달)
- address     : 주소 값 (등록은 미전달)
- addressDetail : 상세주소 값 (등록은 미전달)
- description : 소개 값 (등록은 미전달)
--%>
<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}name">업소명</label>
    <input class="business-form-input" type="text" id="${param.idPrefix}name" name="name" value="${param.name}" required />
</div>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}placeType">업종</label>
    <select class="business-form-select" id="${param.idPrefix}placeType" name="placeType" required>
        <option value="stay" ${param.placeType == 'stay' ? 'selected' : ''}>숙박</option>
        <option value="food" ${param.placeType == 'food' ? 'selected' : ''}>맛집</option>
        <option value="tour" ${param.placeType == 'tour' ? 'selected' : ''}>관광지</option>
    </select>
</div>

<%--
가격 설정: 숙박(placeType='stay')일 때만 노출한다. 맛집/관광지는 예약금을 받지 않기로 해서
서버가 무조건 FREE로 저장하므로 이 영역 자체를 숨긴다. (표시/숨김 토글은 business-venue.js)
가격입력(FIXED)을 고른 경우에만 금액 input이 열린다.
--%>
<c:set var="priceType" value="${empty param.priceType ? 'FIXED' : param.priceType}" />
<div class="business-form-group js-price-group" id="${param.idPrefix}priceGroup">
    <label class="business-form-label">가격</label>
    <div class="business-price-options">
        <label class="business-price-option">
            <input type="radio" name="priceType" value="FIXED" ${priceType == 'FIXED' ? 'checked' : ''} />
            <span>가격입력</span>
        </label>
        <label class="business-price-option">
            <input type="radio" name="priceType" value="VARIABLE" ${priceType == 'VARIABLE' ? 'checked' : ''} />
            <span>가격변동</span>
        </label>
        <label class="business-price-option">
            <input type="radio" name="priceType" value="FREE" ${priceType == 'FREE' ? 'checked' : ''} />
            <span>무료</span>
        </label>
    </div>

    <div class="business-price-input js-price-input${priceType == 'FIXED' ? '' : ' is-hidden'}">
        <input class="business-form-input" type="number" id="${param.idPrefix}minPrice" name="minPrice"
               min="0" step="1000" placeholder="1인 기준 금액" value="${param.minPrice}" />
        <span class="business-price-input__unit">원</span>
    </div>
    <p class="business-price-hint">가격변동은 현장 문의, 무료는 결제 없이 예약만 받습니다.</p>
</div>
<%--todo:주석내용 처리후 삭제--%>
<%-- 지역코드는 당분간 사업자 직접등록 업소엔 null로 두기로 해서 지역 선택 UI 잠시 주석처리
<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}regionId">지역</label>
    <select class="business-form-select" id="${param.idPrefix}regionId" name="regionId">
        <option value="">선택 안함</option>
        <c:forEach var="r" items="${regionOptions}">
            <option value="${r.regionId}" ${r.regionId == param.regionId ? 'selected' : ''}>${r.regionName}</option>
        </c:forEach>
    </select>
</div>
--%>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}address">주소</label>
    <div class="business-form-row">
        <input class="business-form-input" type="text" id="${param.idPrefix}address" name="address" value="${param.address}" readonly required />
        <button type="button" class="business-btn business-btn--outline js-address-search" data-target-prefix="${param.idPrefix}">주소 검색</button>
    </div>
    <input class="business-form-input" type="text" id="${param.idPrefix}addressDetail" name="addressDetail" placeholder="상세주소 입력" value="${param.addressDetail}" />
</div>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}description">소개</label>
    <textarea class="business-form-textarea" id="${param.idPrefix}description" name="description" rows="4">${param.description}</textarea>
</div>