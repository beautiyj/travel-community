<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>소셜 회원가입 | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <%-- [수정] 로컬 가입과 같은 닉네임 형식 규칙만 재사용하고, 로컬 폼 전용 signup.js는 불러오지 않는다. --%>
    <script defer src="${pageContext.request.contextPath}/js/auth/signup-validation.js"></script>
    <script defer src="${pageContext.request.contextPath}/js/auth/social-signup.js"></script>
</head>
<body class="auth-page">
<main class="auth-card auth-card--small">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>소셜 회원가입</h1>
        <p>사이트에서 사용할 이름과 닉네임을 입력해 주세요.</p>
    </header>

    <%-- [수정] 제공자별 가입 화면을 나누지 않고, 서버 세션에서 확인한 소셜 계정 정보만 표시한다. --%>
    <section class="social-account-summary"
             aria-label="<c:out value='${socialProviderName}' /> 계정 정보">
        <c:if test="${not empty socialProfileImageUrl}">
            <img class="social-profile-image"
                 src="<c:out value='${socialProfileImageUrl}' />"
                 alt="<c:out value='${socialProviderName}' /> 프로필 이미지">
        </c:if>
        <div>
            <strong><c:out value="${socialProviderName}" /> 계정</strong>
            <p><c:out value="${socialEmail}" /></p>
        </div>
    </section>

    <c:if test="${not empty socialSignupError}">
        <div class="form-alert form-alert--error" role="alert">
            <c:out value="${socialSignupError}" />
        </div>
    </c:if>

    <form id="socialSignupForm"
          action="${pageContext.request.contextPath}/auth/social/signup"
          method="post"
          novalidate>
        <%-- 소셜 인증을 시작한 같은 세션의 가입 요청인지 서버에서 확인한다. --%>
        <input type="hidden" name="signupNonce"
               value="<c:out value='${socialSignupNonce}' />">

        <div class="form-field">
            <label for="name">이름</label>
            <input id="name" name="name" type="text" maxlength="20"
                   value="<c:out value='${socialSignupRequest.name}' />"
                   aria-describedby="nameError"
                   placeholder="공백 없이 2~20자로 입력하세요" required autofocus>
            <p id="nameError" class="field-error" aria-live="polite"><c:out value="${errors.name}" /></p>
        </div>

        <div class="form-field">
            <label for="nickname">사이트 닉네임</label>
            <div class="input-action-row">
                <input id="nickname" name="nickname" type="text" maxlength="10"
                       value="<c:out value='${socialSignupRequest.nickname}' />"
                       placeholder="공백 없이 2~10자로 입력하세요"
                       aria-describedby="nicknameHelp nicknameError nicknameSuccess"
                       required>
                <button id="checkNicknameButton" class="secondary-button" type="button">중복 확인</button>
            </div>
            <p id="nicknameHelp" class="field-help">소셜 계정 닉네임과 별개로 사이트에서만 사용하는 닉네임입니다.</p>
            <p id="nicknameError" class="field-error" aria-live="polite"><c:out value="${errors.nickname}" /></p>
            <p id="nicknameSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div class="agreement-box">
            <label>
                <input id="privacyAgreed" name="privacyAgreed" type="checkbox" value="true"
                       aria-describedby="privacyAgreedError"
                       ${socialSignupRequest.privacyAgreed ? 'checked' : ''} required>
                개인정보 수집 및 이용에 동의합니다. <strong>(필수)</strong>
            </label>
            <p id="privacyAgreedError" class="field-error" aria-live="polite"><c:out value="${errors.privacyAgreed}" /></p>
        </div>

        <button class="primary-button" type="submit">
            <c:out value="${socialProviderName}" />로 가입하기
        </button>
    </form>

    <p class="auth-switch">
        이미 계정이 있으신가요?
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </p>
</main>
</body>
</html>
