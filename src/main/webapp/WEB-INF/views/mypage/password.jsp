<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="비밀번호 변경" /></jsp:include></head>
<body>
<main class="profile-edit-page"><section class="profile-edit">
    <a class="profile-edit__back" href="${pageContext.request.contextPath}/mypage/info">‹ 회원정보</a>
    <h1 class="profile-edit__title">비밀번호 변경</h1>
    <form class="profile-edit__form" action="${pageContext.request.contextPath}/mypage/password" method="post">
        <input type="hidden" name="memberId" value="${member.memberId}">
        <div class="input-field"><label for="currentPassword">현재 비밀번호</label><input id="currentPassword" type="password" name="currentPassword" required></div>
        <div class="input-field"><label for="newPassword">새 비밀번호</label><input id="newPassword" type="password" name="newPassword" required></div>
        <div class="input-field"><label for="newPasswordCheck">새 비밀번호 확인</label><input id="newPasswordCheck" type="password" name="newPasswordCheck" required></div>
        <div class="profile-edit__actions"><a class="profile-edit__cancel" href="${pageContext.request.contextPath}/mypage/info">취소</a><button class="profile-edit__submit" type="submit">저장</button></div>
    </form>
</section></main>
</body>
</html>
