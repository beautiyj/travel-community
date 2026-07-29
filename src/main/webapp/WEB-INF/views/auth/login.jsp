<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>로그인 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <script defer src="${pageContext.request.contextPath}/js/auth/login.js"></script>
</head>
<body class="auth-page auth-page--login">
<main class="auth-card auth-card--small auth-card--login">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">갈래말래</a>

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

    <c:if test="${param.passwordReset != null}">
        <div class="form-alert form-alert--success" role="status">
            비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.
        </div>
    </c:if>

    <c:choose>
        <c:when test="${param.socialNotLinked != null}">
            <div class="form-alert form-alert--error" role="alert">
                아직 연동되지 않은 소셜 계정입니다. 회원가입 화면에서 소셜 가입 또는 기존 계정 연동을 진행해 주세요.
            </div>
        </c:when>
        <c:when test="${param.socialError != null}">
            <div class="form-alert form-alert--error" role="alert">
                소셜 로그인 인증에 실패했습니다. 다시 시도해 주세요.
            </div>
        </c:when>
        <c:when test="${not empty socialError or not empty kakaoError}">
            <div class="form-alert form-alert--error" role="alert">
                소셜 인증 정보가 없거나 만료되었습니다. 다시 시도해 주세요.
            </div>
        </c:when>
    </c:choose>

    <%-- 로그인 폼을 소셜 로그인 영역보다 먼저 배치해 일반 로그인 흐름을 우선한다. --%>
    <form id="loginForm" class="login-form" action="${pageContext.request.contextPath}/auth/login" method="post" novalidate>
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

    <%-- 공통 소셜 로그인 컴포넌트 적용 전까지 우선순위만 맞춰 임시로 노출한다. --%>
    <section class="social-login-section" aria-label="소셜 로그인">
        <div class="social-login-divider"><span>또는</span></div>
        <%-- 일반 카카오 로그인 공식 버튼 리소스를 사용해 심볼과 레이블 비율을 유지한다. --%>
        <a class="social-login-button social-login-button--kakao"
           href="${pageContext.request.contextPath}/auth/kakao" aria-label="카카오 로그인">
            <img class="kakao-login-standard-image"
                 src="https://k.kakaocdn.net/14/dn/btroDszwNrM/I6efHub1SN5KCJqLm1Ovx1/o.jpg"
                 alt="">
        </a>
        <%-- 기존 소셜 버튼 구조는 유지하고 Google 로고만 작은 아이콘으로 추가한다. --%>
        <a class="social-login-button social-login-button--google"
           href="${pageContext.request.contextPath}/auth/google" aria-label="Google 계정으로 로그인">
            <svg class="social-login-button__icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" aria-hidden="true">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
            </svg>
            <span>Google 계정으로 로그인</span>
        </a>
        <button class="social-login-button social-login-button--naver" type="button" disabled>네이버 로그인 (준비 중)</button>
    </section>

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/find-id">아이디 찾기</a>
        <span aria-hidden="true">|</span>
        <a href="${pageContext.request.contextPath}/auth/find-password">비밀번호 찾기</a>
    </div>

    <p class="auth-switch">
        계정이 없으신가요?
        <a href="${pageContext.request.contextPath}/auth/signup">회원가입</a>
    </p>
</main>
</body>
</html>
