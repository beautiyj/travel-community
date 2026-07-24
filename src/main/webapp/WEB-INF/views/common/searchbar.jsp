<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 서치바 전체 너비 조절 파라미터 --%>
<c:set var="barWidth"          value="${empty param.width ? 'fit-content' : param.width}" />
<%-- 검색창 플레이스홀더 기본값 --%>
<c:set var="searchPlaceholder" value="${empty param.placeholder ? '여행지, 숙소, 맛집을 검색해보세요' : param.placeholder}" />
<%-- 기존 검색어 유지를 위한 value 값(검색한 데이터)유지 --%>
<c:set var="searchValue"       value="${empty param.value ? '' : param.value}" />
<%-- 인풋 네임값 세팅 (서버 전송용 기본 파라미터명 keyword) --%>
<c:set var="inputName"         value="${empty param.name ? 'keyword' : param.name}" />
<%-- 버튼컴포넌트 값 --%>
<c:set var="btnText"           value="${empty param.btnText ? '검색' : param.btnText}" />
<c:set var="btnColor"          value="${empty param.btnColor ? 'var(--primary)' : param.btnColor}" />

<%-- 내장 드롭다운 셀렉터 옵션 추가 (기본 OFF) --%>
<c:set var="useDropdown"       value="${param.useDropdown eq 'true'}" />
<%-- 드롭다운 파라미터 --%>
<c:set var="dropdownId"        value="${empty param.dropdownId ? 'search_dropdown' : param.dropdownId}" />
<c:set var="dropdownListAttr"  value="${empty param.listAttr ? 'categoryList' : param.listAttr}" />
<c:set var="defaultLabel"      value="${empty param.defaultLabel ? '전체' : param.defaultLabel}" />
<c:set var="dropdownWidth"     value="${empty param.dropdownWidth ? '' : param.dropdownWidth}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/dropdownSelector.css">


<div class="search-row-view" style="width: ${barWidth};">
	<c:if test="${useDropdown}">
		<div class="search-dropdown-wrapper">
			<jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
				<jsp:param name="dropdownId"   value="${dropdownId}" />
				<jsp:param name="listAttr"     value="${dropdownListAttr}" />
				<jsp:param name="defaultLabel" value="${defaultLabel}" />
				<jsp:param name="width"        value="${dropdownWidth}" />
			</jsp:include>
		</div>
		<div class="search-divider"></div>
	</c:if>

	<img src="${pageContext.request.contextPath}/images/icons/search.png" class="search-icon" alt="검색" />
	<input type="text"
	class="search-input"
	name="${inputName}"
	value="${searchValue}"
	placeholder="${searchPlaceholder}"
	autocomplete="off" />
	<jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
		<jsp:param name="text"  value="${btnText}" />
		<jsp:param name="width" value="auto" />
		<jsp:param name="color" value="${btnColor}" />
	</jsp:include>

</div>