<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>소셜 회원가입 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/signup-form.css?v=auth-admin-css-split-20260806-r9">
    <script defer src="${pageContext.request.contextPath}/js/auth/auth-history-guard.js"></script>
    <%-- 로컬 가입과 같은 입력 검증 규칙을 재사용하고, 로컬 폼 전용 signup.js는 불러오지 않는다. --%>
    <script defer src="${pageContext.request.contextPath}/js/auth/signup-validation.js"></script>
    <script defer src="${pageContext.request.contextPath}/js/auth/social-signup.js"></script>
</head>
<%-- 로그인 이후 뒤로가기로 소셜 가입 폼이 복원되면 현재 세션 상태를 다시 확인한다. --%>
<body class="auth-page auth-page--social-signup"
      data-session-status-url="${pageContext.request.contextPath}/auth/api/session-status">
<main class="auth-card auth-card--social-signup">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <header class="auth-header">
        <h1>소셜 회원가입</h1>
        <p>사이트에서 사용할 회원 정보를 입력해 주세요.</p>
    </header>

    <%-- 제공자별 가입 화면을 나누지 않고, 서버 세션에서 확인한 소셜 계정 정보만 표시한다. --%>
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

    <%-- 서버 검증 실패 시 안전하게 가공된 공통 오류와 입력 DTO의 값을 다시 렌더링한다. --%>
    <c:if test="${not empty socialSignupError}">
        <div class="form-alert form-alert--error" role="alert">
            <c:out value="${socialSignupError}" />
        </div>
    </c:if>

    <%--
      소셜 프로필만으로 가입을 확정하지 않고 필수 서비스 정보를 POST한다.
      현재 마크업에는 CSRF 토큰 출력이 없으므로 CSRF 보호 적용 시 서버 토큰 필드를 함께 렌더링해야 한다.
    --%>
    <form id="socialSignupForm"
          action="${pageContext.request.contextPath}/auth/social/signup"
          method="post"
          novalidate>
        <%--
          nonce는 소셜 인증을 시작한 같은 세션의 가입 요청인지 확인하고 재사용을 막기 위한 값이다.
          일반적인 CSRF 토큰을 자동으로 대체하는 값으로 간주하지 않는다.
        --%>
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

        <%-- 생년월일은 로컬 가입과 같은 필수 항목이며 서버 오류가 나면 입력값을 유지한다. --%>
        <div class="form-field">
            <label for="birth">생년월일</label>
            <input id="birth" name="birth" type="date"
                   value="<c:out value='${socialSignupRequest.birth}' />"
                   aria-describedby="birthError"
                   autocomplete="bday"
                   required>
            <p id="birthError" class="field-error" aria-live="polite"><c:out value="${errors.birth}" /></p>
        </div>

        <%-- 소셜 회원도 서비스 연락에 사용할 전화번호를 직접 입력하고 서버 검증 오류를 표시한다. --%>
        <div class="form-field">
            <label for="phone">전화번호</label>
            <input id="phone" name="phone" type="tel" maxlength="13"
                   value="<c:out value='${socialSignupRequest.phone}' />"
                   aria-describedby="phoneError"
                   autocomplete="tel"
                   placeholder="010-1234-5678"
                   required>
            <p id="phoneError" class="field-error" aria-live="polite"><c:out value="${errors.phone}" /></p>
        </div>

        <%-- 성별은 세 항목 중 하나를 반드시 고르며 서버 검증 실패 시 선택값을 다시 표시한다. --%>
        <fieldset class="form-field" aria-describedby="genderError">
            <legend>성별</legend>
            <div class="choice-row">
                <label>
                    <input type="radio" name="gender" value="MALE"
                           ${socialSignupRequest.gender == 'MALE' ? 'checked' : ''} required> 남성
                </label>
                <label>
                    <input type="radio" name="gender" value="FEMALE"
                           ${socialSignupRequest.gender == 'FEMALE' ? 'checked' : ''} required> 여성
                </label>
                <label>
                    <input type="radio" name="gender" value="NONE"
                           ${socialSignupRequest.gender == 'NONE' ? 'checked' : ''} required> 선택 안함
                </label>
            </div>
            <p id="genderError" class="field-error" aria-live="polite"><c:out value="${errors.gender}" /></p>
        </fieldset>

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
