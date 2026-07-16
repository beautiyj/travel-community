<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- place_type 분류에 따른 디자인셋 --%>
<c:set var="typeText" value="관광지" />
<c:if test="${param.place_type eq 'food'}"><c:set var="typeText" value="맛집" /></c:if>
	<c:if test="${param.place_type eq 'stay'}"><c:set var="typeText" value="숙박" /></c:if>

		<%-- 기본 태그 - 원하는 텍스트 데이터 기입 --%>
		<c:if test="${not empty param.text}">
			<c:set var="typeText" value="${param.text}" />
		</c:if>

		<%--
		클래스명은 type-food/type-stay/type-tour or type-default 기본 디자인 적용
		텍스트는 맛집/숙박/관광지 or 직접 지정한 태그 텍스트 출력
		--%>
		<div class="tag-view type-${empty param.place_type ? 'default' : param.place_type}">
			<span class="tag-text">
				${typeText}
			</span>
		</div>