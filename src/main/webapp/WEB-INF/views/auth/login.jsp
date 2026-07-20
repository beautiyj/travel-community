<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>로그인 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <script defer src="${pageContext.request.contextPath}/js/auth/login.js"></script>
</head>
<body class="auth-page">
<main class="auth-card auth-card--small">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>로그인</h1>
        <p>여행 이야기를 계속 만나보세요.</p>
    </header>

    <c:choose>
        <c:when test="${param.locked != null}">
            <div class="form-alert form-alert--error" role="alert">
                비밀번호 입력을 5회 연속 실패하여 로그인이 제한되었습니다. 잠금 시작 후 5분 뒤 다시 시도해주세요.
            </div>
        </c:when>
        <c:when test="${param.error != null}">
            <div class="form-alert form-alert--error" role="alert">
                아이디 또는 비밀번호가 일치하지 않습니다. 같은 아이디에서 비밀번호를 5회 연속 잘못 입력하면 5분 동안 로그인이 제한됩니다.
            </div>
        </c:when>
    </c:choose>

    <c:if test="${param.logout != null}">
        <div class="form-alert form-alert--success" role="status">
            로그아웃되었습니다.
        </div>
    </c:if>

    <form id="loginForm" action="${pageContext.request.contextPath}/auth/login" method="post" novalidate>
        <div class="form-field">
            <label for="username">아이디</label>
            <input id="username" name="username" type="text" autocomplete="username"
                   maxlength="20" placeholder="아이디를 입력하세요" required autofocus>
            <p id="usernameError" class="field-error" aria-live="polite">${usernameError}</p>
        </div>

        <div class="form-field">
            <label for="password">비밀번호</label>
            <div class="password-field">
                <input id="password" name="password" type="password" autocomplete="current-password"
                       maxlength="20" placeholder="비밀번호를 입력하세요" required>
                <button id="togglePassword" class="text-button" type="button" aria-label="비밀번호 표시">보기</button>
            </div>
            <p id="passwordError" class="field-error" aria-live="polite">${passwordError}</p>
        </div>

        <button class="primary-button" type="submit">로그인</button>
    </form>

    <div class="auth-links">
        <a href="#">아이디 찾기</a>
        <span aria-hidden="true">|</span>
        <a href="#">비밀번호 찾기</a>
    </div>

    <p class="auth-switch">
        계정이 없으신가요?
        <a href="${pageContext.request.contextPath}/auth/signup">회원가입</a>
    </p>
</main>
</body>
</html>
