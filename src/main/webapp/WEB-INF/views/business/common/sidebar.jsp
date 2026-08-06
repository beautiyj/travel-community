<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeTab" value="${param.activeTab}" />

<aside class="business-sidebar">
    <div class="business-sidebar__logo">
        <%-- 메인 헤더(common/header.jsp)와 동일한 로고 표기: common.css의 .brand .name 클래스를 그대로 재사용 --%>
        <a href="/" class="brand"><span class="name">갈래말래</span></a>
    </div>

    <div class="business-sidebar__biz">
        <div class="business-sidebar__biz-row">
            <div class="business-sidebar__biz-avatar">
                <c:choose>
                    <c:when test="${not empty bizFirstImage}">
                        <img src="${bizFirstImage}" alt="${bizName}" />
                    </c:when>
                    <c:otherwise>
                        <svg class="business-sidebar__biz-avatar-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M4 21V5a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v16M15 21V9a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v12M8 7h1M8 11h1M8 15h1" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="business-sidebar__biz-text">
                <p class="business-sidebar__biz-name">${bizName}</p>
                <p class="business-sidebar__biz-owner">${ownerName} 대표</p>
            </div>
        </div>
        <div class="business-sidebar__status">
            <c:choose>
                <c:when test="${isClosed == 1}">
                    <span class="business-status-dot business-status-dot--closed"></span>
                    <span class="business-status-label business-status-label--closed">예약 마감</span>
                </c:when>
                <c:otherwise>
                    <span class="business-status-dot business-status-dot--open"></span>
                    <span class="business-status-label business-status-label--open">예약 운영중</span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <nav class="business-sidebar__nav">
        <a href="/business/dashboard" class="business-nav-item${activeTab == 'overview' ? ' is-active' : ''}">대시보드</a>
        <a href="/business/reservations" class="business-nav-item${activeTab == 'reservations' ? ' is-active' : ''}">
            예약 관리
            <c:if test="${(pendingCount + cancelRequestCount) > 0}">
                <span class="business-badge">${pendingCount + cancelRequestCount}</span>
            </c:if>
        </a>
        <a href="/business/closure" class="business-nav-item${activeTab == 'closure' ? ' is-active' : ''}">마감 관리</a>
        <a href="/business/venue" class="business-nav-item${activeTab == 'venue' ? ' is-active' : ''}">업소 관리</a>
        <a href="/business/reviews" class="business-nav-item${activeTab == 'reviews' ? ' is-active' : ''}">후기 확인</a>
    </nav>

    <div class="business-sidebar__footer">
        <%-- 일반 이용자에게 보이는 업소 상세페이지 미리보기. 새 탭으로 열어 관리 화면 흐름을 안 끊는다.
             /place/detail?placeId= 는 아직 place(tour) 모듈 담당자가 구현 전인 가정 경로 —
             community/detail.jsp의 장소 태그 링크와 동일한 경로를 그대로 맞춰 썼다. --%>
        <c:if test="${not empty placeId}">
            <a href="/place/detail?placeId=${placeId}" target="_blank" rel="noopener" class="business-nav-item">내 업소보기</a>
        </c:if>
        <form method="post" action="/auth/logout" class="business-sidebar__logout-form">
            <button type="submit" class="business-nav-item business-nav-item--danger">로그아웃</button>
        </form>
    </div>
</aside>
