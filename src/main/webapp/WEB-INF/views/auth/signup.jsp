<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>회원 유형 선택 | 갈래말래</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css?v=naver-20260729-7">
    <script defer src="${pageContext.request.contextPath}/js/auth/auth-history-guard.js"></script>
</head>
<%-- 로그인 이후 뒤로가기로 이 선택 화면이 복원되면 세션 상태를 다시 확인한다. --%>
<body class="auth-page auth-page--signup-type"
      data-session-status-url="${pageContext.request.contextPath}/auth/api/session-status">
<main class="signup-type-shell">
    <%-- 시안의 여행 이미지는 프로젝트에 없으므로 외부 이미지를 추가하지 않고 그라데이션으로 분위기만 구현한다. --%>
    <aside class="signup-type-visual" aria-label="갈래말래 서비스 안내">
        <div class="signup-type-visual__content">
            <span class="signup-type-visual__symbol" aria-hidden="true">✈</span>
            <h2>함께 떠나요,<br>갈래말래</h2>
            <p>나만의 여행을 발견하고<br>새로운 추억을 시작하세요.</p>
            <ul>
                <li>숙박·맛집·여행지 원스톱 예약</li>
                <li>커뮤니티로 동행·후기 공유</li>
                <li>사업자 전용 예약 관리</li>
            </ul>
        </div>
    </aside>

    <section class="signup-type-main" aria-labelledby="signup-type-title">
        <div class="signup-type-main__inner">
            <header class="signup-type-header">
                <div class="signup-type-title-row">
                    <a class="signup-type-back"
                       href="${pageContext.request.contextPath}/auth/login"
                       aria-label="로그인 화면으로 돌아가기">←</a>
                    <h1 id="signup-type-title">회원가입</h1>
                </div>

                <%-- 회원가입 흐름의 현재 위치를 시각 정보와 텍스트 정보로 함께 제공한다. --%>
                <ol class="signup-progress" aria-label="회원가입 진행 단계">
                    <li class="signup-progress__step signup-progress__step--current" aria-current="step">
                        <span class="signup-progress__number">1</span>
                        <span class="signup-progress__label">유형 선택</span>
                    </li>
                    <li class="signup-progress__step">
                        <span class="signup-progress__number">2</span>
                        <span class="signup-progress__label">회원정보 입력</span>
                    </li>
                </ol>
            </header>

            <div class="signup-type-intro">
                <h2>어떤 유형으로 가입하시나요?</h2>
                <p>유형에 따라 이용 가능한 서비스가 달라집니다.</p>
            </div>

            <%-- 소셜 인증·연동 처리에서 서버가 redirect한 결과에 따라 하나의 오류만 표시한다. --%>
            <c:choose>
                <c:when test="${param.socialAlreadyLinked != null}">
                    <div class="form-alert form-alert--error" role="alert">
                        이미 다른 소셜 계정이 연동된 회원입니다. 기존 로그인 방법을 이용해 주세요.
                    </div>
                </c:when>
                <c:when test="${param.socialLinkUnavailable != null}">
                    <div class="form-alert form-alert--error" role="alert">
                        소셜 회원가입 또는 계정 연동을 진행할 수 없습니다. 다른 가입 방법을 이용해 주세요.
                    </div>
                </c:when>
            </c:choose>

            <%-- 별도 선택용 JavaScript 없이 카드 자체를 기존 회원가입 주소로 연결해 기능을 단순하게 유지한다. --%>
            <div class="signup-type-list">
                <a class="signup-type-option signup-type-option--traveler"
                   href="${pageContext.request.contextPath}/auth/signup/user">
                    <span class="signup-type-icon" aria-hidden="true">🧳</span>
                    <span class="signup-type-content">
                        <strong>여행객</strong>
                        <span class="signup-type-description">여행지를 탐색하고 숙박·맛집을 예약하는 일반 이용자</span>
                        <span class="signup-type-features">
                            <span>여행지 탐색 · 예약 · 결제</span>
                            <span>찜 목록 · 후기 작성</span>
                            <span>커뮤니티 동행 모집</span>
                        </span>
                    </span>
                </a>

                <a class="signup-type-option signup-type-option--business"
                   href="${pageContext.request.contextPath}/auth/signup/business">
                    <span class="signup-type-icon" aria-hidden="true">🏪</span>
                    <span class="signup-type-content">
                        <strong>사업자</strong>
                        <span class="signup-type-description">업소를 등록하고 예약을 받아 운영하는 사업자</span>
                        <span class="signup-type-features">
                            <span>업소 등록 · 정보 관리</span>
                            <span>예약 수락 · 거절 · 관리</span>
                            <span>매출 현황 대시보드</span>
                        </span>
                    </span>
                </a>
            </div>

            <%-- 로그인과 회원가입 의도를 구분하는 소셜 가입 전용 주소를 사용한다. --%>
            <section class="social-login-section signup-type-social" aria-label="소셜 회원가입">
                <div class="social-login-divider">소셜 계정으로 시작하기</div>
                <a class="social-login-button social-login-button--kakao"
                   href="${pageContext.request.contextPath}/auth/kakao/signup" aria-label="카카오 계정으로 가입">
                    <span class="kakao-login-content">
                        <img class="kakao-login-symbol"
                             src="${pageContext.request.contextPath}/images/auth/kakao-login-symbol.png?v=naver-20260729-6"
                             alt="">
                        <span class="kakao-login-label">카카오 로그인</span>
                    </span>
                </a>
                <a class="social-login-button social-login-button--google"
                   href="${pageContext.request.contextPath}/auth/google/signup" aria-label="Google 계정으로 가입">
                    <svg class="social-login-button__icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" aria-hidden="true">
                        <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                        <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
                        <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
                        <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
                    </svg>
                    <span>Google 계정으로 가입</span>
                </a>
                <a class="social-login-button social-login-button--naver"
                   href="${pageContext.request.contextPath}/auth/naver/signup" aria-label="네이버로 시작하기">
                    <img class="naver-login-icon"
                         src="${pageContext.request.contextPath}/images/auth/NAVER_login_Light_KR_green_icon_H48.png?v=naver-20260729-7"
                         alt="">
                    <span>네이버로 시작하기</span>
                </a>
            </section>

            <p class="auth-switch signup-type-switch">
                이미 계정이 있으신가요?
                <a href="${pageContext.request.contextPath}/auth/login">로그인</a>
            </p>
        </div>
    </section>
</main>
</body>
</html>
