<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>계정 재활성화 확인 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/account.css?v=auth-admin-css-split-20260806-r7">
</head>
<body class="auth-page auth-page--account-recovery">
<main class="auth-card auth-card--small auth-card--account-recovery">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <header class="auth-header">
        <h1>계정을 재활성화할까요?</h1>
        <p>이메일 본인 확인이 완료되었습니다.</p>
    </header>

    <div class="form-alert form-alert--notice" role="status">
        재활성화 후 자동 로그인되지 않습니다. 완료 후 아이디와 비밀번호로 로그인해 주세요.
    </div>

    <form action="${pageContext.request.contextPath}/auth/reactivation/complete" method="post">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <button class="primary-button" type="submit">계정 재활성화 완료</button>
    </form>

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/login">로그인으로 돌아가기</a>
    </div>
</main>
</body>
</html>
