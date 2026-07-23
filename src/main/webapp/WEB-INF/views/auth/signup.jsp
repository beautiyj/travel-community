<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>회원 유형 선택 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
</head>
<body class="auth-page">
<main class="auth-card signup-type-card">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>회원가입</h1>
        <p>가입할 회원 유형을 먼저 선택해주세요.</p>
    </header>

    <%-- 공통 소셜 로그인 컴포넌트 적용 전까지 카카오 가입 진입점만 임시로 제공한다. --%>
    <section class="social-login-section" aria-label="소셜 회원가입">
        <a class="social-login-button social-login-button--kakao"
           href="${pageContext.request.contextPath}/auth/kakao">카카오로 가입</a>
        <button class="social-login-button social-login-button--google" type="button" disabled>구글 가입 (준비 중)</button>
        <button class="social-login-button social-login-button--naver" type="button" disabled>네이버 가입 (준비 중)</button>
    </section>

    <div class="signup-type-list">
        <a class="signup-type-option" href="${pageContext.request.contextPath}/auth/signup/user">
            <span class="signup-type-icon" aria-hidden="true">✈</span>
            <span class="signup-type-content">
                <strong>일반 회원</strong>
                <span>여행 정보를 공유하고 커뮤니티를 이용합니다.</span>
            </span>
            <span class="signup-type-arrow" aria-hidden="true">›</span>
        </a>

        <a class="signup-type-option" href="${pageContext.request.contextPath}/auth/signup/business">
            <span class="signup-type-icon" aria-hidden="true">▣</span>
            <span class="signup-type-content">
                <strong>사업자 회원</strong>
                <span>사업자등록증을 첨부하여 사업자 계정으로 가입합니다.</span>
            </span>
            <span class="signup-type-arrow" aria-hidden="true">›</span>
        </a>
    </div>

    <p class="auth-switch">
        이미 계정이 있으신가요?
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </p>
</main>
</body>
</html>
