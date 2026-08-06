<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="cp" value="${pageContext.request.contextPath}"/>
<c:set var="isBusiness" value="${param.accountType eq 'BUSINESS'}"/>
<c:set var="isBusinessAccessBlocked"
       value="${isBusiness and (application.status eq 'PENDING'
           or application.status eq 'REJECTED'
           or member.memberRole ne 'BUSINESS')}"/>
<c:set var="businessAccessMessage" value="사업자 권한이 승인되지 않아 사업장으로 갈 수 없습니다."/>
<c:if test="${application.status eq 'PENDING'}">
    <c:set var="businessAccessMessage" value="사업자 인증이 승인 대기 중이라 사업장으로 갈 수 없습니다."/>
</c:if>
<c:if test="${application.status eq 'REJECTED'}">
    <c:set var="businessAccessMessage" value="사업자 인증이 반려되어 사업장으로 갈 수 없습니다."/>
</c:if>
<c:if test="${isBusinessAccessBlocked}">
    <link rel="stylesheet" href="${cp}/css/components/buttonComponent.css">
    <link rel="stylesheet" href="${cp}/css/components/confirmModal.css">
    <style>
        #businessRejectedModal .modal {
            position: relative;
        }

        #businessRejectedModal .modal-btn-cancel {
            display: none;
        }

        #businessRejectedModal .modal-title {
            padding-right: 32px;
        }

        #businessRejectedModal .business-rejected-modal__close {
            position: absolute;
            top: 14px;
            right: 16px;
            width: 32px;
            height: 32px;
            border: 0;
            border-radius: 50%;
            background: transparent;
            color: var(--muted-foreground);
            cursor: pointer;
            font-size: 28px;
            line-height: 1;
        }

        #businessRejectedModal .business-rejected-modal__close:hover {
            background: var(--secondary);
            color: var(--foreground);
        }
    </style>
</c:if>
<aside class="mypage-sidebar${isBusiness ? ' business-sidebar' : ''}"
       aria-label="${isBusiness ? '사업자 마이페이지 메뉴' : '마이페이지 메뉴'}">
    <a class="mypage-sidebar__brand brand" href="${cp}/">
        <span class="name">갈래말래</span>
    </a>
    <div class="mypage-sidebar__profile">
        <div class="mypage-sidebar__avatar">
            <c:choose>
                <c:when test="${not empty member.profileImgUrl}">
                    <img src="${cp}<c:out value='${member.profileImgUrl}'/>"
                         alt="회원 프로필 사진"
                         onerror="this.hidden=true; this.nextElementSibling.hidden=false;">
                </c:when>
                <c:when test="${empty member.profileImgUrl}">
                	<img src="${cp}/images/icons/default-avatar.png" alt="회원 프로필 사진">
                </c:when>
                <c:otherwise>
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg>
                </c:otherwise>
            </c:choose>
        </div>
        <strong><c:out value="${empty member.name ? (isBusiness ? '사업자 회원' : '회원') : member.name}"/></strong>
    </div>
    <nav class="mypage-sidebar__menu">
        <c:choose>
            <c:when test="${isBusiness}">
                <a class="${param.active eq 'info' ? 'is-active' : ''}" href="${cp}/mypage/business-info">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg>
                    <span>기본 정보</span>
                </a>
                <a class="${param.active eq 'approval' ? 'is-active' : ''}" href="${cp}/mypage/business-info/approval">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 12l2 2 4-4M7 3h10a2 2 0 0 1 2 2v16l-7-3-7 3V5a2 2 0 0 1 2-2Z"/></svg>
                    <span>사업자 승인 내역</span>
                </a>
                <a class="${param.active eq 'places' ? 'is-active' : ''}" href="${cp}/business/dashboard"
                   <c:if test="${isBusinessAccessBlocked}">data-business-rejected-access</c:if>>
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6M8 10h.01M16 10h.01"/></svg>
                    <span>나의 사업장 관리</span>
                </a>
            </c:when>
            <c:otherwise>
                <a class="${param.active eq 'info' ? 'is-active' : ''}" href="${cp}/mypage/info">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21a8 8 0 0 0-16 0M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/></svg>
                    <span>회원정보</span>
                </a>
                <a class="${param.active eq 'wishlist' ? 'is-active' : ''}" href="${cp}/mypage/wishlist">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m20.8 4.6-1.4-1.4a5 5 0 0 0-7.1 0L12 3.5l-.3-.3a5 5 0 0 0-7.1 7.1L12 18l7.4-7.7a5 5 0 0 0-.1-5.7Z"/></svg>
                    <span>찜목록</span>
                </a>
                <a class="${param.active eq 'reservation' ? 'is-active' : ''}" href="${cp}/mypage/reservation">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 10h18"/></svg>
                    <span>예약목록</span>
                </a>
            </c:otherwise>
        </c:choose>
        <form method="post" action="/auth/logout" class="mypage-sidebar__logout">
<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 17l5-5-5-5m5 5H3m10 8h6a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-6"/></svg>
            <button type="submit">로그아웃</button>
        </form>
    </nav>
</aside>
<c:if test="${isBusinessAccessBlocked}">
    <jsp:include page="/WEB-INF/views/common/confirmModal.jsp">
        <jsp:param name="modalId" value="businessRejectedModal"/>
        <jsp:param name="title" value="이동 불가"/>
        <jsp:param name="message" value="${businessAccessMessage}"/>
        <jsp:param name="confirmText" value="확인"/>
        <jsp:param name="cancelText" value="닫기"/>
        <jsp:param name="tone" value="primary"/>
        <jsp:param name="method" value="get"/>
        <jsp:param name="action" value="/mypage/business-info"/>
    </jsp:include>
    <script>
        (function () {
            var accessLink = document.querySelector('[data-business-rejected-access]');
            var modal = document.getElementById('businessRejectedModal');
            if (!accessLink || !modal) return;

            var dialog = modal.querySelector('.modal');
            var cancelButton = modal.querySelector('.modal-btn-cancel');
            if (cancelButton) cancelButton.remove();

            var closeButton = document.createElement('button');
            closeButton.type = 'button';
            closeButton.className = 'business-rejected-modal__close';
            closeButton.setAttribute('aria-label', '닫기');
            closeButton.textContent = '×';
            dialog.insertBefore(closeButton, dialog.firstChild);

            var closeModal = function () {
                modal.classList.remove('is-open');
                document.body.classList.remove('modal-open');
            };

            accessLink.addEventListener('click', function (event) {
                event.preventDefault();
                modal.classList.add('is-open');
                document.body.classList.add('modal-open');
                closeButton.focus();
            });

            closeButton.addEventListener('click', closeModal);

            var form = modal.querySelector('form');
            if (form) {
                form.addEventListener('submit', function (event) {
                    event.preventDefault();
                    closeModal();
                });
            }

            modal.addEventListener('click', function (event) {
                if (event.target === modal) closeModal();
            });

            document.addEventListener('keydown', function (event) {
                if (event.key === 'Escape' && modal.classList.contains('is-open')) {
                    closeModal();
                }
            });
        }());
    </script>
</c:if>
