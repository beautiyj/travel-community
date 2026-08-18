<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <script src="${pageContext.request.contextPath}/js/csrf.js"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${businessMember ? '사업자 회원가입' : '일반 회원가입'} | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=auth-admin-css-split-20260806-r7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/signup-form.css?v=auth-admin-css-split-20260806-r9">
    <script defer src="${pageContext.request.contextPath}/js/auth/auth-history-guard.js"></script>
    <%-- 회원가입 검증 모듈을 먼저 로드한 뒤 화면·API 로직을 실행한다. --%>
    <script defer src="${pageContext.request.contextPath}/js/auth/signup-validation.js"></script>
    <script defer src="${pageContext.request.contextPath}/js/auth/signup.js"></script>
</head>
<body class="auth-page auth-page--signup-form">
<main class="auth-card auth-card--signup-form">
    <p class="auth-brand brand"><span class="name">갈래말래</span></p>

    <%-- 계정정보와 상세정보를 같은 화면에서 입력하므로 실제 가입 흐름을 2단계로 표시한다. --%>
    <ol class="signup-progress signup-progress--form" aria-label="회원가입 진행 단계">
        <li class="signup-progress__step signup-progress__step--complete"
            aria-label="1단계 유형 선택 완료">
            <span class="signup-progress__number">1</span>
            <span class="signup-progress__label">유형 선택</span>
        </li>
        <li class="signup-progress__step signup-progress__step--current" aria-current="step">
            <span class="signup-progress__number">2</span>
            <span class="signup-progress__label">회원정보 입력</span>
        </li>
    </ol>

    <header class="auth-header">
        <h1>${businessMember ? '사업자 회원가입' : '일반 회원가입'}</h1>
        <p>
            <c:choose>
                <c:when test="${businessMember}">사업자 회원가입 후 관리자 승인을 받으면 업소와 예약을 관리할 수 있습니다.</c:when>
                <c:otherwise>나만의 여행 기록을 시작해보세요.</c:otherwise>
            </c:choose>
        </p>
    </header>

    <%--
      multipart POST는 회원 정보와 사업자등록증 파일을 함께 전달한다.
      JavaScript 검증·중복 확인·이메일 인증 상태는 조작 가능하므로 서버가 모두 재검증하고,
      아이디·닉네임·이메일의 최종 유일성은 DB 제약조건으로도 보장해야 한다.
      현재 마크업에는 CSRF 토큰 출력이 없으므로 CSRF 보호 적용 시 서버 토큰 필드를 함께 렌더링해야 한다.
    --%>
    <form id="signupForm" method="post" enctype="multipart/form-data" novalidate
          action="${pageContext.request.contextPath}/auth/membersignup">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <%-- memberType은 화면 분기용 서버 모델을 다시 전송하지만 hidden 값이므로 서버에서 허용 범위를 반드시 검증한다. --%>
        <input type="hidden" name="memberType" value="${memberType}">

        <div class="selected-member-type">
            <span>선택한 회원 유형</span>
            <strong>${businessMember ? '사업자 회원' : '일반 회원'}</strong>
            <a href="${pageContext.request.contextPath}/auth/signup">변경</a>
        </div>

        <div class="form-field">
            <label for="name">이름</label>
            <%-- DB member.name VARCHAR(20)과 동일하게 브라우저 입력 길이를 제한한다. --%>
            <input id="name" name="name" type="text" maxlength="20"
                   autocomplete="name" placeholder="실명을 입력하세요" required>
            <p id="nameError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="login_id">아이디</label>
            <div class="input-action-row">
                <input id="login_id" name="loginId" type="text" maxlength="20" pattern="^[a-z0-9]{5,20}$"
                       autocomplete="username" placeholder="영문 또는 숫자 5~20자" required>
                <button id="checkUsernameButton" class="secondary-button" type="button">중복 확인</button>
            </div>
            <p id="usernameError" class="field-error" aria-live="polite"></p>
            <p id="usernameSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="password">비밀번호</label>
            <%-- 서버와 같은 비밀번호 길이 제한을 적용한다. --%>
            <input id="password" name="password" type="password" maxlength="20"
                   autocomplete="new-password" placeholder="영문과 숫자를 포함한 8자 이상 20자 이하" required>
            <p id="passwordError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="passwordConfirm">비밀번호 확인</label>
            <input id="passwordConfirm" name="passwordConfirm" type="password" maxlength="20"
                   autocomplete="new-password" placeholder="비밀번호를 다시 입력하세요" required>
            <p id="passwordConfirmError" class="field-error" aria-live="polite"></p>
            <p id="passwordConfirmSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="email">이메일</label>
            <div class="input-action-row">
                <input id="email" name="email" type="email" maxlength="100"
                       autocomplete="email" placeholder="example@naver.com" required>
                <button id="sendEmailCodeButton" class="secondary-button" type="button">인증번호 발송</button>
            </div>
            <p id="emailError" class="field-error" aria-live="polite"></p>
            <p id="emailSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div id="emailVerificationField" class="form-field" hidden>
            <label for="verificationCode">이메일 인증번호</label>
            <div class="input-action-row">
                <input id="verificationCode" type="text"
                       inputmode="numeric" maxlength="6" placeholder="6자리 인증번호">
                <button id="verifyEmailCodeButton" class="secondary-button" type="button">인증 확인</button>
            </div>
            <p id="verificationCodeError" class="field-error" aria-live="polite"></p>
        </div>

        <%-- businessMember가 true일 때만 파일 입력을 렌더링한다. 파일 형식·크기·내용과 저장 권한은 서버가 재검증한다. --%>
        <c:if test="${businessMember}">
            <div class="form-field">
                <label for="businessRegistrationFile">사업자등록증</label>
                <input id="businessRegistrationFile" name="businessRegistrationFile" type="file"
                       accept=".jpg,.jpeg,.png,image/jpeg,image/png" required hidden
                       aria-describedby="businessRegistrationFileHelp businessRegistrationFileError">
                <div class="input-action-row">
                    <p id="businessRegistrationFileInfo" class="business-registration-file-info"
                       role="status" aria-live="polite" aria-atomic="true">선택된 파일 없음</p>
                    <button id="businessRegistrationFileButton" class="secondary-button" type="button"
                            aria-label="사업자등록증 파일 선택"
                            aria-describedby="businessRegistrationFileHelp businessRegistrationFileError">파일 선택</button>
                </div>
                <p id="businessRegistrationFileHelp" class="field-help">JPG, JPEG, PNG 형식, 최대 5MB</p>
                <p id="businessRegistrationFileError" class="field-error" aria-live="polite"></p>
            </div>
        </c:if>

        <div class="form-field">
            <label for="nickname">닉네임</label>
            <div class="input-action-row">
                <%-- 서버의 닉네임 정책과 같은 최대 길이를 브라우저에도 적용한다. --%>
                <input id="nickname" name="nickname" type="text" maxlength="10"
                       placeholder="2~10자로 입력하세요" required>
                <button id="checkNicknameButton" class="secondary-button" type="button">중복 확인</button>
            </div>
            <p id="nicknameError" class="field-error" aria-live="polite"></p>
            <p id="nicknameSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="birth">생년월일</label>
            <input id="birth" name="birth" type="date" required>
            <p id="birthError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="phone">전화번호</label>
            <input id="phone" name="phone" type="tel" maxlength="13"
                   autocomplete="tel" placeholder="010-1234-5678" required>
            <p id="phoneError" class="field-error" aria-live="polite"></p>
        </div>

        <fieldset class="form-field" aria-describedby="genderError">
            <legend>성별</legend>
            <div class="choice-row">
                <label><input type="radio" name="gender" value="MALE" required> 남성</label>
                <label><input type="radio" name="gender" value="FEMALE" required> 여성</label>
                <label><input type="radio" name="gender" value="NONE" required> 선택 안함</label>
            </div>
            <p id="genderError" class="field-error" aria-live="polite"></p>
        </fieldset>

        <div class="agreement-box">
            <label>
                <input id="privacyAgreed" name="privacyAgreed" type="checkbox" value="true" required>
                개인정보 수집 및 이용에 동의합니다. <strong>(필수)</strong>
            </label>
            <p id="privacyAgreedError" class="field-error" aria-live="polite"></p>
        </div>

        <div id="signupMessage" class="form-alert" role="status" hidden></div>
        <button class="primary-button" type="submit">회원가입</button>
    </form>

    <p class="auth-switch">
        이미 계정이 있으신가요?
        <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
    </p>
</main>
</body>
</html>
