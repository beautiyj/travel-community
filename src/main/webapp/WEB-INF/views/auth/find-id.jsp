<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>아이디 찾기 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <script defer src="${pageContext.request.contextPath}/js/auth/find-id.js"></script>
</head>
<body class="auth-page">
<main class="auth-card auth-card--small">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>아이디 찾기</h1>
        <p>회원가입 시 등록한 이름과 이메일을 입력해주세요.</p>
    </header>

    <%-- 추후 아이디 조회 컨트롤러와 연결할 입력 항목만 구성한다. --%>
    <form id="findIdForm" action="${pageContext.request.contextPath}/auth/find-id" method="post" novalidate>
        <div class="form-field">
            <label for="findIdName">이름</label>
            <input id="findIdName" name="name" type="text" autocomplete="name"
                   maxlength="20" placeholder="이름을 입력하세요" required autofocus>
            <p id="findIdNameError" class="field-error" aria-live="polite">${nameError}</p>
        </div>

        <div class="form-field">
            <label for="findIdEmail">이메일</label>
            <%-- 서버의 이메일 최대 길이 정책과 동일하게 100자로 제한한다. --%>
            <input id="findIdEmail" name="email" type="email" autocomplete="email"
                   maxlength="100" placeholder="example@email.com" required>
            <p id="findIdEmailError" class="field-error" aria-live="polite">${emailError}</p>
        </div>

        <button class="primary-button" type="submit">아이디 찾기</button>
    </form>

    <p class="auth-switch">
        로그인 화면으로 돌아가시겠어요?
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </p>
</main>
</body>
</html>
