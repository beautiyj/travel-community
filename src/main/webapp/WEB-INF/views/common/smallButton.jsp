<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 노출 텍스트 (기본값: '확인') --%>
<c:set var="btnText" value="${empty param.text ? '확인' : param.text}" />
<%-- 아이콘 이미지 경로 (있을 때만 노출하도록 방어 처리) --%>
<c:set var="btnIcon" value="${empty param.iconSrc ? '' : param.iconSrc}" />
<%-- 클릭 시 동작할 스크립트 (기본값: 없음) --%>
<c:set var="btnOnClick" value="${empty param.onclick ? '' : param.onclick}" />
<%--  컬러 테마 세팅 (기본값: theme-primary / 필요 시 확장용 param.theme에 secondary 등 주입해서 스타일 변경 가능 --%>
<c:set var="themeClass" value="theme-${empty param.theme ? 'primary' : param.theme}" />
<%-- 너비 커스텀 세팅 (param.width가 있을 경우에만 적용) --%>
<c:set var="widthStyle" value="" />
<c:if test="${not empty param.width}"><c:set var="widthStyle" value="width: ${param.width};" /></c:if>

	<button class="btn-small-row ${themeClass}" style="${widthStyle}"
	<c:if test="${not empty btnOnClick}">onclick="${btnOnClick}"</c:if> >
		<%-- 아이콘이 존재할 때만 이미지 태그 동적 렌더링 --%>
		<c:if test="${not empty btnIcon}">
			<img src="${btnIcon}" class="btn-small-icon" alt="button icon" />
		</c:if>
		<span class="btn-small-text">
			${btnText}
		</span>
	</button>