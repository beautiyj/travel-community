<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>비밀번호 찾기 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/account.css?v=auth-admin-css-split-20260806-r7">
    <script defer src="${pageContext.request.contextPath}/js/auth/find-password.js"></script>
</head>
<body class="auth-page auth-page--account-recovery">
<main class="auth-card auth-card--small auth-card--account-recovery">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <header class="auth-header">
        <h1>비밀번호 찾기</h1>
        <p>아이디와 가입 이메일로 본인 확인을 진행합니다.</p>
    </header>

    <%-- 재설정 화면에서 인증 세션이 만료·소비된 경우 서버가 이 쿼리 파라미터로 되돌려 보낸다. --%>
    <c:if test="${param.error eq 'verification'}">
        <div class="form-alert form-alert--error" role="alert">
            이메일 인증이 만료되었거나 이미 사용되었습니다. 다시 인증해주세요.
        </div>
    </c:if>

    <%--
      이 form에는 action이 없고 find-password.js가 제출을 가로채 발송/검증 API를 순서대로 호출한다.
      data-* 속성은 컨텍스트 경로와 성공 후 이동 주소를 JavaScript에 전달할 뿐 인증 정보가 아니다.
      인증 성공 여부와 재설정 대상은 브라우저 값이 아니라 서버 세션과 DB 인증 이력으로 확인한다.
    --%>
    <form id="findPasswordForm" novalidate
          data-context-path="${pageContext.request.contextPath}"
          data-reset-password-url="${pageContext.request.contextPath}/auth/reset-password">
        <div class="form-field">
            <label for="findPasswordUsername">아이디</label>
            <input id="findPasswordUsername" name="username" type="text" autocomplete="username"
                   maxlength="20" pattern="^[a-z0-9]{5,20}$" placeholder="아이디를 입력하세요" required autofocus>
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

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/reactivation">탈퇴 계정 재활성화</a>
        <span aria-hidden="true">|</span>
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </div>
</main>
</body>
</html>
