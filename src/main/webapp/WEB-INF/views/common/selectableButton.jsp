<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 노출 텍스트 (기본값: '선택') --%>
<c:set var="btnText" value="${empty param.text ? '선택' : param.text}" />
<%-- 이동할 링크 (기본값: '#') --%>
<c:set var="btnHref" value="${empty param.href ? '#' : param.href}" />
<%--
선택 유무 상태값 (Active / Inactive)
param.isActive가 'true'이면 서버가 전달한 값 그대로 활성화 스타일 클래스('is-active')를 붙임
--%>
<c:set var="activeClass" value="${param.isActive eq 'true' ? 'is-active' : ''}" />
<%--
2종 컬러 테마 세팅 (기본 계통: theme-primary / 댄저 계통: theme-danger)
param.theme이 'danger'로 들어오면 theme-danger가 적용 그 외 기본값 theme-primary
--%>
<c:set var="themeClass" value="${param.theme eq 'danger' ? 'theme-danger' : 'theme-primary'}" />

<a href="${btnHref}" class="btn-selectable ${themeClass} ${activeClass}"
style="<c:if test="${not empty param.width}">width: ${param.width};</c:if> ${param.style}">
	<span class="btn-selectable-text">
		${btnText}
	</span>
</a>