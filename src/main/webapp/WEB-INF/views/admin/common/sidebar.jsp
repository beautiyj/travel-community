<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:url var="adminHomeUrl" value="/admin"/>
<c:url var="businessApplicationsUrl" value="/admin/business-applications"/>
<c:url var="logoutUrl" value="/auth/logout"/>

<aside class="admin-sidebar">
    <a class="admin-sidebar__logo" href="${adminHomeUrl}">
        <span class="admin-sidebar__logo-badge" aria-hidden="true">A</span>
        <span>갈래말래 관리자</span>
    </a>

    <div class="admin-sidebar__profile">
        <p class="admin-sidebar__profile-label">관리자</p>
        <p class="admin-sidebar__profile-name">
            <c:out value="${sessionScope.loginMember.nickname}"/> 님
        </p>
    </div>

    <nav class="admin-sidebar__nav" aria-label="관리자 메뉴">
        <a class="admin-nav-item${param.activeTab eq 'dashboard' ? ' is-active' : ''}"
           href="${adminHomeUrl}"
           aria-current="${param.activeTab eq 'dashboard' ? 'page' : 'false'}">
            대시보드
        </a>
        <a class="admin-nav-item${param.activeTab eq 'businessApplications' ? ' is-active' : ''}"
           href="${businessApplicationsUrl}"
           aria-current="${param.activeTab eq 'businessApplications' ? 'page' : 'false'}">
            사업자 인증 관리
        </a>
    </nav>

    <div class="admin-sidebar__footer">
        <form method="post" action="${logoutUrl}">
            <button class="admin-sidebar__logout" type="submit">로그아웃</button>
        </form>
    </div>
</aside>
