<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 검색창 플레이스홀더 기본값 --%>
<c:set var="searchPlaceholder" value="${empty param.placeholder ? '여행지, 숙소, 맛집을 검색해보세요' : param.placeholder}" />
<%-- 기존 검색어 유지를 위한 value 값(검색한 데이터)유지 --%>
<c:set var="searchValue" value="${empty param.value ? '' : param.value}" />
<%-- 인풋 네임값 세팅 (서버 전송용 기본 파라미터명 keyword) --%>
<c:set var="inputName" value="${empty param.name ? 'keyword' : param.name}" />
<c:set var="btnText" value="${empty param.btnText ? '검색' : param.btnText}" />

<div class="search-row-view">
	<img src="${pageContext.request.contextPath}/resources/images/search.png" class="search-icon" alt="검색" />
	<input type="text"
	class="search-input"
	name="${inputName}"
	value="${searchValue}"
	placeholder="${searchPlaceholder}"
	autocomplete="off" />
	<button type="submit" class="search-btn">
		<span class="search-btn-text">
			${btnText}
		</span>
	</button>

</div>