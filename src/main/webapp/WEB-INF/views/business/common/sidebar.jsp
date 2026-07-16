<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeTab" value="${param.activeTab}" />

<aside class="business-sidebar">
    <div class="business-sidebar__logo">
        <span class="business-sidebar__logo-badge">TA</span> 트립어라운드
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
                <c:when test="${isClosed}">
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
        <a href="/business/dashboard?memberId=${memberId}" class="business-nav-item${activeTab == 'overview' ? ' is-active' : ''}">대시보드</a>
        <a href="/business/reservations?memberId=${memberId}" class="business-nav-item${activeTab == 'reservations' ? ' is-active' : ''}">
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
        <a href="/" class="business-nav-item">사용자 화면</a>
        <a href="/business/logout" class="business-nav-item business-nav-item--danger">로그아웃</a>
    </div>
</aside>
