<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>탈퇴 계정 재활성화 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/account.css?v=auth-admin-css-split-20260806-r7">
    <script defer src="${pageContext.request.contextPath}/js/auth/reactivation.js"></script>
</head>
<body class="auth-page auth-page--account-recovery">
<main class="auth-card auth-card--small auth-card--account-recovery">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <header class="auth-header">
        <h1>탈퇴 계정 재활성화</h1>
        <p>아이디를 입력하면 가입 이메일로 본인 확인을 진행합니다.</p>
    </header>

    <form id="reactivationForm" novalidate
          data-context-path="${pageContext.request.contextPath}"
          data-complete-url="${pageContext.request.contextPath}/auth/reactivation/complete">
        <div class="form-field">
            <label for="reactivationUsername">아이디</label>
            <div class="input-action-row">
                <input id="reactivationUsername" name="username" type="text" autocomplete="username"
                       maxlength="20" pattern="^[a-z0-9]{5,20}$" placeholder="아이디를 입력하세요" required autofocus>
                <button id="sendReactivationCodeButton" class="secondary-button" type="button">인증번호 발송</button>
            </div>
            <p id="reactivationUsernameError" class="field-error" aria-live="polite"></p>
            <p id="reactivationSendSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div id="reactivationVerificationField" class="form-field" hidden>
            <label for="reactivationCode">이메일 인증번호</label>
            <input id="reactivationCode" name="code" type="text" inputmode="numeric"
                   autocomplete="one-time-code" maxlength="6" pattern="[0-9]{6}"
                   placeholder="6자리 인증번호" required>
            <p id="reactivationCodeError" class="field-error" aria-live="polite"></p>
        </div>

        <button id="verifyReactivationCodeButton" class="primary-button" type="submit" disabled>본인 확인</button>
    </form>

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/find-id">아이디 찾기</a>
        <span aria-hidden="true">|</span>
        <a href="${pageContext.request.contextPath}/auth/find-password">비밀번호 찾기</a>
        <span aria-hidden="true">|</span>
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </div>
</main>
</body>
</html>
