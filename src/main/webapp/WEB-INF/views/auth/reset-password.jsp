<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>새 비밀번호 설정 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <script defer src="${pageContext.request.contextPath}/js/auth/reset-password.js"></script>
</head>
<body class="auth-page">
<main class="auth-card auth-card--small">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>새 비밀번호 설정</h1>
        <p>영문과 숫자를 포함해 8~20자로 설정해주세요.</p>
    </header>

    <c:if test="${not empty resetPasswordError}">
        <div class="form-alert form-alert--error" role="alert">${resetPasswordError}</div>
    </c:if>

    <%-- 변경 대상 회원은 클라이언트 입력값이 아니라 이메일 인증 후 서버 세션에 저장한 정보로 결정한다. --%>
    <form id="resetPasswordForm" action="${pageContext.request.contextPath}/auth/reset-password" method="post" novalidate>
        <div class="form-field">
            <label for="newPassword">새 비밀번호</label>
            <input id="newPassword" name="newPassword" type="password" autocomplete="new-password"
                   maxlength="20" placeholder="영문과 숫자를 포함한 8~20자" required autofocus>
            <p id="newPasswordError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="newPasswordConfirm">새 비밀번호 확인</label>
            <input id="newPasswordConfirm" name="newPasswordConfirm" type="password" autocomplete="new-password"
                   maxlength="20" placeholder="새 비밀번호를 다시 입력하세요" required>
            <p id="newPasswordConfirmError" class="field-error" aria-live="polite"></p>
            <p id="newPasswordConfirmSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <button class="primary-button" type="submit">비밀번호 변경</button>
    </form>
</main>
</body>
</html>
