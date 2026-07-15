<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 카드 식별자 ID (실제 DB의 role 값과 매핑: ROLE_USER, ROLE_BUSINESS 등) --%>
<c:set var="cardId" value="${empty param.id ? 'ROLE_USER' : param.id}" />
<%-- 카드들을 하나의 단일선택 그룹으로 묶어줄 그룹명 (기본값: 'role-group') --%>
<c:set var="cardGroup" value="${empty param.group ? 'role-group' : param.group}" />
<%-- 카드 테마 종류 선택 (기본값: 'blue' (일반) / 'amber' (사업자)) --%>
<c:set var="cardTheme" value="${param.theme eq 'amber' ? 'amber' : 'blue'}" />

<%--
최초 로딩 시 선택 유무 상태값 (Active / Inactive)
param.isActive가 'true'이면 활성화 스타일 클래스('active')
--%>
<c:set var="activeClass" value="${param.isActive eq 'true' ? 'active' : ''}" />
<c:set var="cardEmoji" value="${empty param.emoji ? '🧳' : param.emoji}" />
<c:set var="cardTitle" value="${empty param.title ? '역할 타이틀' : param.title}" />
<c:set var="cardDescription" value="${empty param.description ? '역할에 대한 상세한 기본 정의 설명입니다.' : param.description}" />

<div class="sel-card-col-${cardTheme}${activeClass}"
data-card-id="${cardId}"
data-card-group="${cardGroup}">

<div class="sel-card-row-center">
    <div class="sel-card-emoji-box">
        <span class="sel-card-emoji-text">${cardEmoji}</span>
    </div>

    <div class="sel-card-badge-check">
        <svg class="sel-card-check-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
    </div>
</div>

<div class="sel-card-view-center">
    <h3 class="sel-card-txt-xl">${cardTitle}</h3>
    <p class="sel-card-txt-base">${cardDescription}</p>
</div>

<div class="sel-card-view-wrapper">
    <div class="sel-card-row-padding">
        <svg class="sel-card-list-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span class="sel-card-txt-meta">
            ${empty param.feat1 ? '상세 기능 1' : param.feat1}
        </span>
    </div>
    <div class="sel-card-row-padding">
        <svg class="sel-card-list-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span class="sel-card-txt-meta">
            ${empty param.feat2 ? '상세 기능 2' : param.feat2}
        </span>
    </div>
    <div class="sel-card-row-padding">
        <svg class="sel-card-list-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span class="sel-card-txt-meta">
            ${empty param.feat3 ? '상세 기능 3' : param.feat3}
        </span>
    </div>
</div>

</div>