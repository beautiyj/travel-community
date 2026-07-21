<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>비밀번호 찾기 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <script defer src="${pageContext.request.contextPath}/js/auth/find-password.js"></script>
</head>
<body class="auth-page">
<main class="auth-card auth-card--small">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>비밀번호 찾기</h1>
        <p>아이디와 가입 이메일로 본인 확인을 진행합니다.</p>
    </header>

    <c:if test="${param.error eq 'verification'}">
        <div class="form-alert form-alert--error" role="alert">
            이메일 인증이 만료되었거나 이미 사용되었습니다. 다시 인증해주세요.
        </div>
    </c:if>

    <%-- 인증 성공 여부는 브라우저 값이 아니라 서버 세션과 DB 인증 이력으로 확인한다. --%>
    <form id="findPasswordForm" novalidate
          data-context-path="${pageContext.request.contextPath}"
          data-reset-password-url="${pageContext.request.contextPath}/auth/reset-password">
        <div class="form-field">
            <label for="findPasswordUsername">아이디</label>
            <input id="findPasswordUsername" name="username" type="text" autocomplete="username"
                   maxlength="20" pattern="[A-Za-z0-9]{5,20}" placeholder="아이디를 입력하세요" required autofocus>
            <p id="findPasswordUsernameError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="findPasswordEmail">이메일</label>
            <div class="input-action-row">
                <input id="findPasswordEmail" name="email" type="email" autocomplete="email"
                       maxlength="100" placeholder="example@email.com" required>
                <button id="sendPasswordCodeButton" class="secondary-button" type="button">인증번호 발송</button>
            </div>
            <p id="findPasswordEmailError" class="field-error" aria-live="polite"></p>
            <p id="findPasswordEmailSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div id="passwordVerificationField" class="form-field" hidden>
            <label for="findPasswordCode">이메일 인증번호</label>
            <input id="findPasswordCode" name="code" type="text" inputmode="numeric"
                   autocomplete="one-time-code" maxlength="6" pattern="[0-9]{6}"
                   placeholder="6자리 인증번호" required>
            <p id="findPasswordCodeError" class="field-error" aria-live="polite"></p>
        </div>

        <button id="verifyPasswordCodeButton" class="primary-button" type="submit" disabled>본인 확인</button>
    </form>

    <p class="auth-switch">
        로그인 화면으로 돌아가시겠어요?
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </p>
</main>
</body>
</html>
