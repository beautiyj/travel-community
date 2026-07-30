<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />
<aside class="mypage-sidebar" aria-label="마이페이지 메뉴">
    <div class="mypage-sidebar__profile">
        <div class="mypage-sidebar__avatar">
            <c:choose>
                <c:when test="${not empty member.profileImgUrl}">
                    <img src="<c:out value='${member.profileImgUrl}'/>" alt="회원 프로필 사진">
                </c:when>
                <c:otherwise><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg></c:otherwise>
            </c:choose>
        </div>
        <strong><c:out value="${empty member.name ? '회원' : member.name}" /></strong>
        <span><c:out value="${empty member.email ? '내 계정' : member.email}" /></span>
    </div>

    <nav class="mypage-sidebar__menu">
        <a class="${param.active eq 'info' ? 'is-active' : ''}" href="${cp}/mypage/info"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg>회원정보</a>
        <a class="${param.active eq 'wishlist' ? 'is-active' : ''}" href="${cp}/mypage/wishlist"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m20.8 4.6-1.4-1.4a5 5 0 0 0-7.1 0L12 3.5l-.3-.3a5 5 0 0 0-7.1 7.1L12 18l7.4-7.7a5 5 0 0 0-.1-5.7Z"/></svg>찜목록</a>
        <a class="${param.active eq 'reservation' ? 'is-active' : ''}" href="${cp}/mypage/reservation"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 10h18"/></svg>예약목록</a>
        <a class="mypage-sidebar__withdraw" href="${cp}/mypage/withdraw"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m-9 0 1 13h10l1-13M10 11v5m4-5v5"/></svg>회원탈퇴</a>
        <a class="mypage-sidebar__logout" href="${cp}/mypage/logout"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 17l5-5-5-5m5 5H3m10 8h6a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-6"/></svg>로그아웃</a>
    </nav>
</aside>
