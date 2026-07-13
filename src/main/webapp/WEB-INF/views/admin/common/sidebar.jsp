<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeTab" value="${param.activeTab}" />

<aside class="admin-sidebar">
    <div class="admin-sidebar__logo">
        <span class="admin-sidebar__logo-badge">TA</span> 트립어라운드
    </div>

    <div class="admin-sidebar__biz">
        <p class="admin-sidebar__biz-name">${bizName}</p>
        <p class="admin-sidebar__biz-owner">${ownerName} 대표</p>
        <div class="admin-sidebar__status">
            <c:choose>
                <c:when test="${isClosed}">
                    <span class="admin-status-dot admin-status-dot--closed"></span>
                    <span class="admin-status-label admin-status-label--closed">예약 마감</span>
                </c:when>
                <c:otherwise>
                    <span class="admin-status-dot admin-status-dot--open"></span>
                    <span class="admin-status-label admin-status-label--open">예약 운영중</span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <nav class="admin-sidebar__nav">
        <a href="/admin/dashboard?memberId=${memberId}" class="admin-nav-item${activeTab == 'overview' ? ' is-active' : ''}">대시보드</a>
        <a href="/admin/reservations" class="admin-nav-item${activeTab == 'reservations' ? ' is-active' : ''}">
            예약 관리
            <c:if test="${(pendingCount + cancelRequestCount) > 0}">
                <span class="admin-badge">${pendingCount + cancelRequestCount}</span>
            </c:if>
        </a>
        <a href="/admin/closure" class="admin-nav-item${activeTab == 'closure' ? ' is-active' : ''}">마감 관리</a>
        <a href="/admin/venue" class="admin-nav-item${activeTab == 'venue' ? ' is-active' : ''}">업소 관리</a>
        <a href="/admin/reviews" class="admin-nav-item${activeTab == 'reviews' ? ' is-active' : ''}">후기 확인</a>
    </nav>

    <div class="admin-sidebar__footer">
        <a href="/" class="admin-nav-item">사용자 화면</a>
        <a href="/admin/logout" class="admin-nav-item admin-nav-item--danger">로그아웃</a>
    </div>
</aside>
