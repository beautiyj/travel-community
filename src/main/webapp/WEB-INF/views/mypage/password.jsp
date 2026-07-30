<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="비밀번호 변경" /></jsp:include></head>
<body>
<main class="profile-edit-page"><section class="profile-edit">
    <a class="profile-edit__back" href="${pageContext.request.contextPath}/mypage/info">‹ 회원정보</a>
    <h1 class="profile-edit__title">비밀번호 변경</h1>
    <c:if test="${not empty error}">
        <p class="profile-edit__message profile-edit__message--error"><c:out value="${error}" /></p>
    </c:if>
    <form class="profile-edit__form" action="${pageContext.request.contextPath}/mypage/password" method="post">
        <div class="input-field"><label for="currentPassword">현재 비밀번호</label><input id="currentPassword" type="password" name="currentPassword" autocomplete="current-password" required></div>
        <div class="input-field"><label for="newPassword">새 비밀번호</label><input id="newPassword" type="password" name="newPassword" autocomplete="new-password" minlength="8" maxlength="20" placeholder="영문·숫자 포함 8~20자" required></div>
        <div class="input-field"><label for="newPasswordCheck">새 비밀번호 확인</label><input id="newPasswordCheck" type="password" name="newPasswordCheck" autocomplete="new-password" minlength="8" maxlength="20" required></div>
        <div class="profile-edit__actions"><a class="profile-edit__cancel" href="${pageContext.request.contextPath}/mypage/info">취소</a><button class="profile-edit__submit" type="submit">저장</button></div>
    </form>
</section></main>
</body>
</html>
