<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="cp" value="${pageContext.request.contextPath}" />
<aside class="mypage-sidebar business-sidebar" aria-label="사업자 마이페이지 메뉴">
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
        <span><c:out value="${member.email}" default="사업자 계정"/></span>
        <span class="business-profile-type">사업자 회원</span>
    </div>
    <nav class="mypage-sidebar__menu">
        <a class="${param.active eq 'info' ? 'is-active' : ''}" href="${cp}/mypage/business-info">기본 정보</a>
        <a class="${param.active eq 'approval' ? 'is-active' : ''}" href="${cp}/mypage/business-info/approval">사업자 승인 관리</a>
        <a class="${param.active eq 'places' ? 'is-active' : ''}" href="${cp}/mypage/business-info/places">내 사업장 목록</a>
        <a href="${cp}/mypage/business-info/withdraw">회원 탈퇴</a>
        <a class="mypage-sidebar__logout" href="${cp}/mypage/logout">로그아웃</a>
    </nav>
</aside>
