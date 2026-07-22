<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="btnWidth"   value="${empty param.width ? 'auto' : param.width}" />
<c:set var="btnColor"   value="${empty param.color ? 'var(--primary)' : param.color}" />
<c:set var="fontSize"   value="${empty param.size ? 'var(--text-base)' : param.size}" />
<%-- onclick 파라미터 유무 체크 (없으면 onclick 속성 자체를 비워둠) --%>
<c:set var="hasOnClick" value="${not empty param.onclick}" />

<button class="btn-main" style="width: ${btnWidth}; background: ${btnColor};"
<c:if test="${hasOnClick}">onclick="${param.onclick}"</c:if>>
	<span class="btn-main-text" style="font-size: ${fontSize};">
		${param.text}
	</span>
</button>