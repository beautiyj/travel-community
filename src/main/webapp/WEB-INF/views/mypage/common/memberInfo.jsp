<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="cp" value="${pageContext.request.contextPath}"/>
<c:set var="basePath" value="${businessAccount ? '/mypage/business-info' : '/mypage'}"/>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${businessAccount ? '사업자 기본 정보' : '회원정보'}</title>
    <link rel="stylesheet" href="${cp}/css/common.css">
    <link rel="stylesheet" href="${cp}/css/mypage/common.css">
    <link rel="stylesheet" href="${cp}/css/mypage/${businessAccount ? 'business' : 'user'}.css">
    <link rel="stylesheet" href="${cp}/css/mypage/modal.css">
    <c:if test="${businessAccount}">
        <link rel="stylesheet" href="${cp}/css/components/buttonComponent.css">
        <link rel="stylesheet" href="${cp}/css/components/confirmModal.css">
    </c:if>
</head>
<body class="${businessAccount ? 'business-mypage' : ''}">
<main class="mypage-page">
    <h1 class="mypage-page__title">${businessAccount ? '기본 정보' : '회원정보'}</h1>
    <c:choose>
        <c:when test="${businessAccount}">
            <c:if test="${not empty message}"><div class="biz-message"><c:out value="${message}"/></div></c:if>
            <c:if test="${not empty error}"><div class="biz-message biz-message--error"><c:out value="${error}"/></div></c:if>
        </c:when>
        <c:otherwise>
            <c:if test="${not empty message}"><p class="profile-edit__message"><c:out value="${message}"/></p></c:if>
        </c:otherwise>
    </c:choose>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp">
            <jsp:param name="accountType" value="${businessAccount ? 'BUSINESS' : 'USER'}"/>
            <jsp:param name="active" value="info"/>
        </jsp:include>
        <section class="mypage-content mypage-content--form">
            <dl class="member-info-list">
                <div><dt>아이디</dt><dd><c:out value="${member.loginId}" default="-"/></dd></div>
                <div><dt>이름</dt><dd><c:out value="${member.name}" default="-"/></dd></div>
                <div><dt>닉네임</dt><dd><c:out value="${member.nickname}" default="-"/></dd></div>
                <div><dt>이메일</dt><dd><c:out value="${member.email}" default="-"/></dd></div>
                <div><dt>연락처</dt><dd><c:out value="${member.phone}" default="-"/></dd></div>
                <div><dt>생일</dt><dd><c:out value="${member.birth}" default="-"/></dd></div>
                <div>
                    <dt>성별</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${member.gender eq 'MALE'}">남성</c:when>
                            <c:when test="${member.gender eq 'FEMALE'}">여성</c:when>
                            <c:otherwise>미설정</c:otherwise>
                        </c:choose>
                    </dd>
                </div>
                <c:if test="${businessAccount}">
                    <div>
                        <dt>사업자 승인 상태</dt>
                        <dd><jsp:include page="/WEB-INF/views/mypage/common/businessApprovalStatus.jsp"/></dd>
                    </div>
                    <c:if test="${application.status eq 'REJECTED' and not empty application.rejectionReason}">
                        <div>
                            <dt>반려 사유</dt>
                            <dd class="business-rejection-reason"><c:out value="${application.rejectionReason}"/></dd>
                        </div>
                    </c:if>
                </c:if>
                <div><dt>가입일</dt><dd><c:out value="${member.createdDate}" default="-"/></dd></div>
            </dl>
            <div class="member-info-actions">
                <a class="mypage-secondary-link" href="${cp}${basePath}/edit">수정하기</a>
                <a class="mypage-secondary-link" href="${cp}${basePath}/password">비밀번호 변경</a>
                <button type="button" class="member-withdraw-link" id="js-withdraw-open">회원탈퇴</button>
            </div>
        </section>
    </div>
</main>
<div class="cancel-modal" id="withdrawModal" hidden>
    <div class="cancel-modal__dialog"
         role="alertdialog"
         aria-modal="true"
         aria-labelledby="withdraw-title"
         aria-describedby="withdraw-description">
        <div class="cancel-modal__header">
            <div>
                <h2 id="withdraw-title">회원탈퇴</h2>
                <p>탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</p>
            </div>
            <button class="cancel-modal__close js-withdraw-close"
                    type="button" aria-label="닫기">×</button>
        </div>
        <form id="withdrawForm"
              action="${cp}${basePath}/withdraw"
              method="post"
              data-login-url="${cp}/auth/login">
            <div class="cancel-modal__body">
                <div class="withdraw-modal__notice">
                    <div class="withdraw-card__icon" aria-hidden="true">!</div>
                    <p id="withdraw-description" class="edit-confirm-modal__message">
                        탈퇴하려면 비밀번호를 입력해 주세요.
                    </p>
                </div>
                <div class="withdraw-notice">
                    <strong>탈퇴 전 꼭 확인하세요</strong>
                    <ul>
                        <c:choose>
                            <c:when test="${businessAccount}">
                                <li>사업자 계정과 연결된 서비스 이용이 중지됩니다.</li>
                                <li>사업자 승인 신청 정보와 마이페이지 정보가 삭제될 수 있습니다.</li>
                            </c:when>
                            <c:otherwise>
                                <li>예약 중인 상품은 자동 취소될 수 있습니다.</li>
                                <li>찜목록과 후기 등 모든 정보가 삭제됩니다.</li>
                            </c:otherwise>
                        </c:choose>
                        <li>탈퇴 후 동일 이메일로 재가입이 제한될 수 있습니다.</li>
                    </ul>
                </div>
                <div class="withdraw-password">
                    <label for="currentPassword">비밀번호 확인</label>
                    <input id="currentPassword" type="password" name="currentPassword"
                           placeholder="현재 비밀번호를 입력하세요" required>
                </div>
                <label class="withdraw-consent">
                    <input type="checkbox" required>
                    <span>위 내용을 확인했으며 탈퇴에 동의합니다.</span>
                </label>
            </div>
            <div class="cancel-modal__actions">
                <button class="cancel-modal__back js-withdraw-close" type="button">돌아가기</button>
                <button class="cancel-modal__submit" type="submit">탈퇴하기</button>
            </div>
        </form>
    </div>
</div>

<c:if test="${businessAccount}"><script src="${cp}/js/common.js"></script></c:if>
<script src="${cp}/js/mypage/common/withdrawModal.js"></script>
</body>
</html>
