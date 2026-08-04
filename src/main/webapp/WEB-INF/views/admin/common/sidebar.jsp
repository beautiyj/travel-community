<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:url var="adminHomeUrl" value="/admin"/>
<c:url var="businessApplicationsUrl" value="/admin/business-applications"/>
<c:url var="logoutUrl" value="/auth/logout"/>

<%--
  이 조각은 각 관리자 화면이 activeTab 파라미터를 넘겨 현재 메뉴를 표시한다.
  사이드바 노출 자체는 인가가 아니므로 /admin/** 접근과 아래 상태 변경 요청은 서버가 ADMIN 권한을 검증해야 한다.
--%>
<aside class="admin-sidebar">
    <a class="admin-sidebar__logo" href="${adminHomeUrl}">
        <span class="admin-sidebar__logo-badge" aria-hidden="true">A</span>
        <span>갈래말래 관리자</span>
    </a>

    <%-- 로그인 성공 시 세션에 저장된 회원 DTO의 닉네임을 출력하며, 권한 판단에는 이 표시값을 사용하지 않는다. --%>
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
        <%-- 로그아웃은 세션을 변경하므로 POST를 사용한다. CSRF 보호 적용 시 서버 토큰 필드를 이 폼에 포함해야 한다. --%>
        <form method="post" action="${logoutUrl}">
            <button class="admin-sidebar__logout" type="submit">로그아웃</button>
        </form>
    </div>
</aside>
