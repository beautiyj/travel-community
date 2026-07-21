<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 고유 ID (한 페이지 내 멀티 드롭다운 충돌 방지) --%>
<c:set var="dropdownId" value="${empty param.dropdownId ? 'defaultSelect' : param.dropdownId}" />
<%-- Controller가 넘겨준 아이템 리스트 매핑 --%>
<c:set var="dropdownList" value="${requestScope[param.listAttr]}" />
<%-- 현재 선택된 값 (Code 및 Name) --%>
<c:set var="selectedValue" value="${requestScope[param.selectedAttr]}" />
<c:set var="selectedName" value="${requestScope[param.selectedNameAttr]}" />
<%-- 기본 라벨 텍스트 (예: '지역 선택', '정렬 기준' 등) --%>
<c:set var="defaultLabel" value="${empty param.defaultLabel ? '선택' : param.defaultLabel}" />
<%-- 아이콘 온오프 설정을 위한 이미지 경로 (있을 때만 매핑) --%>
<c:set var="iconSrc" value="${empty param.iconSrc ? '' : param.iconSrc}" />
<%-- 클릭 시 이동할 기본 URL 및 파라미터 키 --%>
<c:set var="targetUrl" value="${empty param.targetUrl ? '#' : param.targetUrl}" />
<c:set var="paramKey" value="${empty param.paramKey ? 'type' : param.paramKey}" />
<%-- 너비 커스텀 세팅 (px나 % 단위 포함 가능하게 처리, 미입력 시 CSS 기본값 작동) --%>
<c:set var="widthStyle" value="" />
<c:if test="${not empty param.width}">
    <c:set var="widthStyle" value="width: ${param.width};" />
</c:if>


<div class="drop-select-container" style="${widthStyle}">
    <button type="button"
    id="dropTrigger_${dropdownId}"
    class="drop-select-trigger ${not empty selectedValue ? 'is-selected' : ''}"
    data-bs-toggle="dropdown"
    aria-expanded="false">

    <div class="drop-select-left-box">
        <%-- 아이콘 온오프 (주입 시에만 렌더링) --%>
        <c:if test="${not empty iconSrc}">
            <img src="${iconSrc}" class="drop-select-icon" alt="icon" />
        </c:if>

        <%-- 선택된 값 노출 (없으면 기본 라벨) --%>
        <span class="drop-select-text">
            ${empty selectedValue ? defaultLabel : selectedName}
        </span>
    </div>

    <%-- 우측 아래 화살표 Chevron (시안 속 아래 방향 꺾쇠 SVG) --%>
    <svg class="drop-select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="6 9 12 15 18 9"></polyline>
    </svg>
</button>

<%-- 아래로 떨어지는 깔끔한 메뉴 리스트 영역 --%>
<ul class="dropdown-menu drop-select-menu" aria-labelledby="dropTrigger_${dropdownId}">
    <%-- '전체' 혹은 '선택 해제' 옵션 상단 고정 --%>
    <li>
        <a class="dropdown-item drop-menu-item ${empty selectedValue ? 'active' : ''}" href="${targetUrl}">
            전체
        </a>
    </li>
    <c:forEach var="item" items="${dropdownList}">
        <li>
            <a class="dropdown-item drop-menu-item ${selectedValue eq item.code ? 'active' : ''}"
            href="${targetUrl}?${paramKey}=${item.code}">
            ${item.name}
        </a>
    </li>
</c:forEach>
</ul>

</div>