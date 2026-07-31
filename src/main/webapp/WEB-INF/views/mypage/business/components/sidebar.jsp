<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="cp" value="${pageContext.request.contextPath}" />
<aside class="mypage-sidebar business-sidebar" aria-label="사업자 마이페이지 메뉴">
    <a class="mypage-sidebar__brand" href="${cp}/">
        <span class="mypage-sidebar__brand-badge">TA</span>
        <span>갈래말래</span>
    </a>
    <div class="mypage-sidebar__profile">
        <div class="mypage-sidebar__avatar">
            <c:choose>
                <c:when test="${not empty member.profileImgUrl}">
                    <img src="${cp}<c:out value='${member.profileImgUrl}'/>" alt="프로필 이미지">
                </c:when>
                <c:otherwise>
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg>
                </c:otherwise>
            </c:choose>
        </div>
        <strong><c:out value="${empty member.name ? '사업자 회원' : member.name}"/></strong>
        <span class="business-profile-type">사업자 회원</span>
    </div>
    <nav class="mypage-sidebar__menu">
        <a class="${param.active eq 'info' ? 'is-active' : ''}" href="${cp}/mypage/business-info">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg>
            <span>기본 정보</span>
        </a>
        <a class="${param.active eq 'approval' ? 'is-active' : ''}" href="${cp}/mypage/business-info/approval">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 12l2 2 4-4M7 3h10a2 2 0 0 1 2 2v16l-7-3-7 3V5a2 2 0 0 1 2-2Z"/></svg>
            <span>사업자 승인 내역</span>
        </a>
        <a class="${param.active eq 'places' ? 'is-active' : ''}" href="${cp}/business/dashboard">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6M8 10h.01M16 10h.01"/></svg>
            <span>나의 사업장 관리</span>
        </a>
        <a class="${param.active eq 'withdraw' ? 'is-active' : ''}" href="${cp}/mypage/business-info/withdraw">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m-9 0 1 13h10l1-13M10 11v5m4-5v5"/></svg>
            <span>회원 탈퇴</span>
        </a>
        <a class="mypage-sidebar__logout" href="${cp}/mypage/logout">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 17l5-5-5-5m5 5H3m10 8h6a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-6"/></svg>
            <span>로그아웃</span>
        </a>
    </nav>
</aside>
