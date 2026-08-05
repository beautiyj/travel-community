<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>사업자 회원 탈퇴</title>
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/mypage/business.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">회원탈퇴</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp">
            <jsp:param name="active" value="withdraw"/>
        </jsp:include>
        <section class="mypage-content mypage-content--form">
            <div class="withdraw-card">
        <div class="withdraw-card__icon" aria-hidden="true">!</div>
        <p>탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</p>

        <c:if test="${not empty error}">
            <div class="profile-edit__message profile-edit__message--error">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <div class="withdraw-notice">
            <strong>탈퇴 전 꼭 확인하세요</strong>
            <ul>
                <li>사업자 계정과 연결된 서비스 이용이 중지됩니다.</li>
                <li>사업자 승인 신청 정보와 마이페이지 정보가 삭제될 수 있습니다.</li>
                <li>탈퇴 후 동일 이메일로 재가입이 제한될 수 있습니다.</li>
            </ul>
        </div>

        <form action="${pageContext.request.contextPath}/mypage/business-info/withdraw"
              method="post"
              onsubmit="return confirm('정말 회원 탈퇴하시겠습니까?');">
            <div class="withdraw-password">
                <label for="currentPassword">비밀번호 확인</label>
                <input id="currentPassword" type="password"
                       name="currentPassword"
                       placeholder="현재 비밀번호를 입력하세요" required>
            </div>
            <label class="withdraw-consent">
                <input type="checkbox" required>
                <span>위 내용을 확인했으며 탈퇴에 동의합니다.</span>
            </label>
            <div class="withdraw-card__actions">
                <a href="${pageContext.request.contextPath}/mypage/business-info">취소</a>
                <button type="submit">탈퇴하기</button>
            </div>
        </form>
            </div>
        </section>
    </div>
</main>
</body>
</html>
