<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="cp" value="${pageContext.request.contextPath}"/>
<c:set var="basePath" value="${businessAccount ? '/mypage/business-info' : '/mypage'}"/>
<c:set var="infoPath" value="${businessAccount ? basePath : '/mypage/info'}"/>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${businessAccount ? '사업자 ' : ''}비밀번호 변경</title>
    <link rel="stylesheet" href="${cp}/css/common.css">
    <link rel="stylesheet" href="${cp}/css/mypage/common.css">
    <link rel="stylesheet" href="${cp}/css/mypage/${businessAccount ? 'business' : 'user'}.css">
</head>
<body class="${businessAccount ? 'business-mypage' : ''}">
<main class="mypage-page">
    <h1 class="mypage-page__title">비밀번호 변경</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp">
            <jsp:param name="accountType" value="${businessAccount ? 'BUSINESS' : 'USER'}"/>
            <jsp:param name="active" value="info"/>
        </jsp:include>
        <section class="mypage-content mypage-content--form">
            <c:if test="${not empty error}">
                <p class="profile-edit__message profile-edit__message--error"><c:out value="${error}"/></p>
            </c:if>
            <form class="profile-edit__form mypage-form-card" action="${cp}${basePath}/password" method="post">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="input-field"><label for="currentPassword">현재 비밀번호</label><input id="currentPassword" type="password" name="currentPassword" autocomplete="current-password" required></div>
                <div class="input-field"><label for="newPassword">새 비밀번호</label><input id="newPassword" type="password" name="newPassword" autocomplete="new-password" minlength="8" maxlength="20" placeholder="영문·숫자 포함 8~20자" required></div>
                <div class="input-field"><label for="newPasswordCheck">새 비밀번호 확인</label><input id="newPasswordCheck" type="password" name="newPasswordCheck" autocomplete="new-password" minlength="8" maxlength="20" required></div>
                <div class="profile-edit__actions">
                    <a class="profile-edit__cancel" href="${cp}${infoPath}">취소</a>
                    <button class="profile-edit__submit" type="submit">저장</button>
                </div>
            </form>
        </section>
    </div>
</main>
</body>
</html>
