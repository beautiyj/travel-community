<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- 노출 텍스트 (기본값: '선택') --%>
<c:set var="btnText" value="${empty param.text ? '선택' : param.text}" />
<%-- 클릭 시 동작할 스크립트 기본값 없음(null) --%>
<c:set var="btnOnClick" value="${empty param.onclick ? '' : param.onclick}" />
<%--
선택 유무 상태값 (Active / Inactive)
param.isActive가 'true'이면 활성화 스타일 클래스('is-active')를 최초에 붙여서 보내기 (common.js)
--%>
<c:set var="activeClass" value="${param.isActive eq 'true' ? 'is-active' : ''}" />
<%--
2종 컬러 테마 세팅 (기본 계통: theme-primary / 댄저 계통: theme-danger)
param.theme이 'danger'로 들어오면 theme-danger가 적용 그 외 기본값 theme-primary
--%>
<c:set var="themeClass" value="${param.theme eq 'danger' ? 'theme-danger' : 'theme-primary'}" />
<%-- param.width에 구체적인 값이 들어오면 스타일로 동적 주입 없으면 기본 스타일 적용 --%>
<c:set var="widthStyle" value="${empty param.width ? '' : fn:concat('width: ', param.width, ';')}" />

<button class="btn-selectable ${themeClass} ${activeClass}"
style="${widthStyle}"
<c:if test="${not empty btnOnClick}">onclick="${btnOnClick}"</c:if>>
	<span class="btn-selectable-text">
		${btnText}
	</span>
</button>