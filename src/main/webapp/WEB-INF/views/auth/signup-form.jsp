<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${businessMember ? '사업자 회원가입' : '일반 회원가입'} | Travel Community</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
    <script defer src="${pageContext.request.contextPath}/js/auth/signup.js"></script>
</head>
<body class="auth-page">
<main class="auth-card">
    <a class="auth-brand" href="${pageContext.request.contextPath}/">Travel Community</a>

    <header class="auth-header">
        <h1>${businessMember ? '사업자 회원가입' : '일반 회원가입'}</h1>
        <p>
            <c:choose>
                <c:when test="${businessMember}">사업자 정보를 확인할 수 있도록 등록증을 함께 첨부해주세요.</c:when>
                <c:otherwise>나만의 여행 기록을 시작해보세요.</c:otherwise>
            </c:choose>
        </p>
    </header>

    <%-- [수정] 회원가입 POST 경로에 컨텍스트 경로를 반영한다. --%>
    <form id="signupForm" method="post" enctype="multipart/form-data" novalidate
          action="${pageContext.request.contextPath}/auth/membersignup">
        <input type="hidden" name="memberType" value="${memberType}">

        <div class="selected-member-type">
            <span>선택한 회원 유형</span>
            <strong>${businessMember ? '사업자 회원' : '일반 회원'}</strong>
            <a href="${pageContext.request.contextPath}/auth/signup">변경</a>
        </div>

        <div class="form-field">
            <label for="name">이름</label>
            <input id="name" name="name" type="text" maxlength="50"
                   autocomplete="name" placeholder="실명을 입력하세요" required>
            <p id="nameError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="login_id">아이디</label>
            <div class="input-action-row">
                <input id="login_id" name="loginId" type="text" maxlength="20"
                       autocomplete="username" placeholder="영문 또는 숫자 5~20자" required>
                <button id="checkUsernameButton" class="secondary-button" type="button">중복 확인</button>
            </div>
            <p id="usernameError" class="field-error" aria-live="polite"></p>
            <%-- [수정] 아이디 사용 가능 안내를 오류 태그와 분리한다. --%>
            <p id="usernameSuccess" class="field-success" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="password">비밀번호</label>
            <input id="password" name="password" type="password" maxlength="64"
                   autocomplete="new-password" placeholder="영문과 숫자를 포함한 8자 이상" required>
            <p id="passwordError" class="field-error" aria-live="polite"></p>
        </div>

        <div class="form-field">
            <label for="passwordConfirm">비밀번호 확인</label>
            <input id="passwordConfirm" name="passwordConfirm" type="password" maxlength="64"
                   autocomplete="new-password" placeholder="비밀번호를 다시 입력하세요" required>
            <p id="passwordConfirmError" class="field-error" aria-live="polite"></p>
            <%-- [수정] 비밀번호 일치 성공 안내를 오류 태그와 분리한다. --%>
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
            <%-- [수정] 이메일 인증 안내를 오류 태그와 분리한다. --%>
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

        <%-- TODO: 사업자 승인 기능 추가 시 사업자등록증 업로드 기능 구현 예정
        <c:if test="${businessMember}">
            <div id="businessField" class="form-field">
                <label for="businessRegistrationFile">사업자등록증</label>
                <input id="businessRegistrationFile" name="businessRegistrationFile" type="file"
                       accept=".jpg,.jpeg,.png,.pdf" required>
                <p class="field-help">JPG, PNG 또는 PDF 파일을 첨부해주세요.</p>
                <p id="businessRegistrationFileError" class="field-error" aria-live="polite"></p>
            </div>
        </c:if>
        --%>

        <div class="form-field">
            <label for="nickname">닉네임</label>
            <div class="input-action-row">
                <input id="nickname" name="nickname" type="text" maxlength="20"
                       placeholder="2~20자로 입력하세요" required>
                <button id="checkNicknameButton" class="secondary-button" type="button">중복 확인</button>
            </div>
            <p id="nicknameError" class="field-error" aria-live="polite"></p>
            <%-- [수정] 닉네임 사용 가능 안내를 오류 태그와 분리한다. --%>
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

        <fieldset class="form-field">
            <legend>성별 <span class="optional-label">선택</span></legend>
            <div class="choice-row">
                <label><input type="radio" name="gender" value="MALE" checked> 남성</label>
                <label><input type="radio" name="gender" value="FEMALE"> 여성</label>
            </div>
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
