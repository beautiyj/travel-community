<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>아이디 찾기 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/account.css?v=auth-admin-css-split-20260806-r7">
    <script defer src="${pageContext.request.contextPath}/js/auth/find-id.js"></script>
</head>
<body class="auth-page auth-page--account-recovery">
<main class="auth-card auth-card--small auth-card--account-recovery">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <header class="auth-header">
        <h1>아이디 찾기</h1>
        <p>회원가입 시 등록한 이름과 이메일을 입력해주세요.</p>
    </header>

    <%--
      이름과 이메일을 현재 아이디 조회 컨트롤러에 POST로 전달한다.
      novalidate는 브라우저 기본 문구 대신 find-id.js의 안내를 쓰기 위한 것이며 서버 검증을 생략한다는 뜻이 아니다.
      조회 결과가 계정 존재 여부를 노출할 수 있으므로 서버는 응답 정책과 요청 남용 방지를 책임진다.
    --%>
    <form id="findIdForm" action="${pageContext.request.contextPath}/auth/find-id" method="post" novalidate>
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <div class="form-field">
            <label for="findIdName">이름</label>
            <input id="findIdName" name="name" type="text" autocomplete="name"
                   maxlength="20" placeholder="이름을 입력하세요" required autofocus>
            <%-- 서버가 다시 렌더링한 오류 메시지를 표시한다. 화면 출력 전 안전한 메시지만 모델에 담는 것을 전제로 한다. --%>
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

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/reactivation">탈퇴 계정 재활성화</a>
        <span aria-hidden="true">|</span>
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </div>
</main>
</body>
</html>
