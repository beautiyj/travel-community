<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>아이디 찾기 결과 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
</head>
<body class="auth-page">
<main class="auth-card auth-card--small">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>아이디 찾기 결과</h1>
        <p>입력하신 정보로 조회한 결과입니다.</p>
    </header>

    <%-- 아이디 조회 로직은 모델의 loginId 속성에 조회된 아이디를 담아 전달한다. --%>
    <c:choose>
        <c:when test="${not empty loginId}">
            <div class="form-alert form-alert--success" role="status">
                <p>입력하신 정보와 일치하는 아이디입니다.</p>
                <strong><c:out value="${loginId}" /></strong>
            </div>
        </c:when>
        <c:otherwise>
            <div class="form-alert form-alert--error" role="alert">
                입력하신 정보와 일치하는 아이디가 없습니다. 이름과 이메일을 다시 확인해주세요.
            </div>
        </c:otherwise>
    </c:choose>

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/find-id">다시 찾기</a>
        <span aria-hidden="true">|</span>
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </div>
</main>
</body>
</html>
