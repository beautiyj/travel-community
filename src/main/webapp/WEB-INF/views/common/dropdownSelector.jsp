<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
- dropdownId       : (필수) 화면 내 고유 식별자 (JS/HTML id 중복 방지)
ex) "dropdown_basic", "regionSelect"
- listAttr         : (필수) Controller가 request.setAttribute("이름", list)로 전달한 데이터 변수명
ex) "sortList", "regionList"
- defaultLabel     : (선택) 선택 전 기본 노출 문구 (미지정 시 '선택')
ex) "지역 선택", "카테고리"
- iconSrc          : (선택) 버튼 좌측 이미지 경로 (PNG/SVG 모두 가능)
ex) "${pageContext.request.contextPath}/images/icons/search.png"
- width            : (선택) 너비 커스텀 지정 (미지정 시 기본 CSS max-width: 200px)
ex) "120px", "240px", "50%", "100%"
- selectedAttr     : (선택) 초기 선택되어 있을 값(Code) 변수명
- selectedNameAttr : (선택) 초기 선택되어 있을 라벨(Name) 변수명
- targetUrl        : (선택) 선택 시 이동할 URL 경로
- paramKey         : (선택) URL 전달용 파라미터 키 이름
--%>

<c:set var="dropdownId" value="${empty param.dropdownId ? 'defaultSelect' : param.dropdownId}" />
<c:set var="dropdownList" value="${requestScope[param.listAttr]}" />
<c:set var="selectedValue" value="${requestScope[param.selectedAttr]}" />
<c:set var="selectedName" value="${requestScope[param.selectedNameAttr]}" />
<c:set var="defaultLabel" value="${empty param.defaultLabel ? '선택' : param.defaultLabel}" />
<c:set var="iconSrc" value="${param.iconSrc}" />
<c:set var="widthStyle" value="${not empty param.width ? 'width:'.concat(param.width).concat(';') : ''}" />

<div class="drop-select-container dropdown" style="${widthStyle}">
    <button type="button"
    id="dropTrigger_${dropdownId}"
    class="drop-select-trigger ${not empty selectedValue ? 'is-selected' : ''}"
    aria-expanded="false">

    <div class="drop-select-left-box">
        <c:if test="${not empty iconSrc}">
            <img src="${iconSrc}" class="drop-select-icon" alt="icon" onerror="this.style.display='none'" />
        </c:if>

        <span class="drop-select-text" id="dropLabel_${dropdownId}">
            ${empty selectedValue ? defaultLabel : selectedName}
        </span>
    </div>

    <svg class="drop-select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="6 9 12 15 18 9"></polyline>
    </svg>
</button>

<ul class="dropdown-menu drop-select-menu" id="dropMenu_${dropdownId}" aria-labelledby="dropTrigger_${dropdownId}">
    <li>
        <button type="button"
        class="dropdown-item drop-menu-item ${empty selectedValue ? 'is-active' : ''}"
        data-value=""
        data-label="${defaultLabel}">
        ${defaultLabel}
    </button>
</li>
<c:forEach var="item" items="${dropdownList}">
    <li>
        <button type="button"
        class="dropdown-item drop-menu-item ${selectedValue eq item.code ? 'is-active' : ''}"
        data-value="${item.code}"
        data-label="${item.name}">
        ${item.name}
    </button>
</li>
</c:forEach>
</ul>
</div>