<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>기존 계정 연동 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/account.css?v=auth-admin-css-split-20260806-r7">
</head>
<body class="auth-page auth-page--social-link">
<main class="auth-card auth-card--small auth-card--social-link">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <header class="auth-header">
        <h1>기존 계정과 연결</h1>
        <p>
            같은 이메일로 가입된 계정이 있습니다.
            기존 계정을 확인하면 <c:out value="${socialProviderName}" /> 로그인을 함께 사용할 수 있습니다.
        </p>
    </header>

    <%-- 제공자명·이메일·마스킹 아이디는 소셜 콜백을 검증한 서버가 모델로 전달하며 c:out으로 출력한다. --%>
    <section class="social-account-summary social-link-account-summary"
             aria-label="연동할 기존 계정 안내">
        <dl class="social-link-account-details">
            <div>
                <dt><c:out value="${socialProviderName}" /> 인증 이메일</dt>
                <dd><c:out value="${socialEmail}" /></dd>
            </div>
            <div>
                <dt>확인된 기존 아이디</dt>
                <dd><c:out value="${maskedUsername}" /></dd>
            </div>
        </dl>
        <p>본인의 계정이 맞다면 아래에서 아이디와 비밀번호를 다시 확인해 주세요.</p>
    </section>

    <%-- 연동 실패 시 서버 오류와 입력 DTO를 다시 표시하되 비밀번호 값은 재출력하지 않는다. --%>
    <c:if test="${not empty socialLinkError}">
        <div class="form-alert form-alert--error" role="alert">
            <c:out value="${socialLinkError}" />
        </div>
    </c:if>

    <%--
      기존 자격증명과 명시적 동의를 POST하고, 서버가 기존 계정 소유권·세션의 소셜 인증·연동 가능 여부를 함께 확인한다.
      현재 마크업에는 CSRF 토큰 출력이 없으므로 CSRF 보호 적용 시 서버 토큰 필드를 함께 렌더링해야 한다.
    --%>
    <form class="social-link-form"
          action="${pageContext.request.contextPath}/auth/social/link"
          method="post"
          novalidate>
        <%-- linkNonce는 연동 흐름의 일회성과 세션 결속을 검증하는 값이며 사용자 식별자나 일반 CSRF 토큰이 아니다. --%>
        <input type="hidden" name="linkNonce"
               value="<c:out value='${socialLinkNonce}' />">

        <div class="form-field">
            <label for="username">기존 아이디</label>
            <input id="username"
                   name="username"
                   type="text"
                   maxlength="20"
                   pattern="^[a-z0-9]{5,20}$"
                   autocomplete="username"
                   value="<c:out value='${socialLinkRequest.username}' />"
                   aria-describedby="usernameHelp usernameError"
                   aria-invalid="${not empty errors.username ? 'true' : 'false'}"
                   placeholder="기존 아이디를 입력하세요"
                   required
                   autofocus>
            <p id="usernameHelp" class="field-help">
                위에 표시된 계정의 전체 아이디를 입력해 주세요.
            </p>
            <p id="usernameError" class="field-error" aria-live="polite">
                <c:out value="${errors.username}" />
            </p>
        </div>

        <div class="form-field">
            <label for="password">기존 비밀번호</label>
            <input id="password"
                   name="password"
                   type="password"
                   maxlength="20"
                   autocomplete="current-password"
                   aria-describedby="passwordError"
                   aria-invalid="${not empty errors.password ? 'true' : 'false'}"
                   placeholder="기존 비밀번호를 입력하세요"
                   required>
            <p id="passwordError" class="field-error" aria-live="polite">
                <c:out value="${errors.password}" />
            </p>
        </div>

        <div class="agreement-box">
            <label for="linkAgreed">
                <input id="linkAgreed"
                       name="linkAgreed"
                       type="checkbox"
                       value="true"
                       aria-describedby="linkAgreedError"
                       aria-invalid="${not empty errors.linkAgreed ? 'true' : 'false'}"
                       ${socialLinkRequest.linkAgreed ? 'checked' : ''}
                       required>
                현재 <c:out value="${socialProviderName}" /> 계정을 기존 계정에 연동하는 것에 동의합니다.
                <strong>(필수)</strong>
            </label>
            <p id="linkAgreedError" class="field-error" aria-live="polite">
                <c:out value="${errors.linkAgreed}" />
            </p>
        </div>

        <button class="primary-button" type="submit">연동하기</button>
    </form>

    <div class="social-link-alternative">
        <p>
            연동하지 않으면 현재 소셜 인증 정보를 폐기하고 기존 로그인 화면으로 이동합니다.
        </p>
        <%-- 취소도 서버의 임시 소셜 인증 상태를 폐기하는 상태 변경이므로 GET 링크가 아닌 POST를 사용한다. --%>
        <form action="${pageContext.request.contextPath}/auth/social/link/cancel"
              method="post">
            <input type="hidden" name="linkNonce"
                   value="<c:out value='${socialLinkNonce}' />">
            <button class="social-link-secondary-action" type="submit">로그인하기</button>
        </form>
    </div>
</main>
</body>
</html>
