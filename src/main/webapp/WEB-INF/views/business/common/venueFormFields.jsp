<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
업소 등록/수정 폼 공통 입력 필드 (업소명 / 업종 / 주소 / 소개)

사용법 (jsp:param으로 전달하는 값):
- idPrefix    : input id 접두어 (등록: 미전달(빈 값) / 수정: "edit-")
- name        : 업소명 값 (등록은 미전달 → 빈 값, 수정은 기존 값 채움)
- placeType   : 업종 코드 값 ("1"/"2"/"3", 등록은 미전달)
- address     : 주소 값 (등록은 미전달)
- description : 소개 값 (등록은 미전달)
--%>
<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}name">업소명</label>
    <input class="business-form-input" type="text" id="${param.idPrefix}name" name="name" value="${param.name}" required />
</div>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}placeType">업종</label>
    <select class="business-form-select" id="${param.idPrefix}placeType" name="placeType" required>
        <option value="1" ${param.placeType == '1' ? 'selected' : ''}>숙박</option>
        <option value="2" ${param.placeType == '2' ? 'selected' : ''}>맛집</option>
        <option value="3" ${param.placeType == '3' ? 'selected' : ''}>관광지</option>
    </select>
</div>

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
    <input class="business-form-input" type="text" id="${param.idPrefix}address" name="address" value="${param.address}" required />
</div>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}description">소개</label>
    <textarea class="business-form-textarea" id="${param.idPrefix}description" name="description" rows="4">${param.description}</textarea>
</div>
