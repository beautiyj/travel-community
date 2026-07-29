<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
업소 사진 업로드 드롭존 (등록/수정 폼 공용)

사용법 (jsp:param으로 전달하는 값):
- inputName  : 파일 input의 name (등록: images / 수정: newImages)
- remaining  : 추가로 올릴 수 있는 남은 장수
- isHidden   : 'true'면 드롭존을 숨김 처리 (수정 폼에서 이미 5장 꽉 찼을 때, 미전달 시 노출)
- isRequired : 'true'면 file input에 required 부여 (최초 등록 시 최소 1장 필수)
--%>
<c:set var="hiddenClass" value="${param.isHidden eq 'true' ? ' is-hidden' : ''}" />
<c:set var="requiredAttr" value="${param.isRequired eq 'true' ? 'required' : ''}" />

<label id="venue-photos-dropzone" class="venue-photo-dropzone${hiddenClass}" for="venue-photos-input">
    <svg class="venue-photo-dropzone__icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M4 8a2 2 0 0 1 2-2h1.17a2 2 0 0 0 1.66-.89l.34-.51a2 2 0 0 1 1.66-.89h2.34a2 2 0 0 1 1.66.89l.34.51a2 2 0 0 0 1.66.89H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
        <circle cx="12" cy="13" r="3.5" stroke="currentColor" stroke-width="2"/>
    </svg>
    <p class="venue-photo-dropzone__title">클릭하여 사진 추가</p>
    <p class="venue-photo-dropzone__hint">JPG, PNG, WEBP · 최대 10MB · 최대 <span id="venue-photos-remaining">${param.remaining}</span>장 추가 가능</p>
    <input class="venue-photo-dropzone__input" type="file" id="venue-photos-input" name="${param.inputName}" multiple accept="image/jpeg,image/png,image/webp" ${requiredAttr} />
</label>
