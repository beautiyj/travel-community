<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>비밀번호 변경</title>
</head>
<body>

<h2>비밀번호 변경</h2>

<form action="/mypage/password" method="post">

<input type="hidden" name="memberId" value="${member.memberId}">

현재 비밀번호 :
<input type="password" name="currentPassword"><br><br>

새 비밀번호 :
<input type="password" name="newPassword"><br><br>

새 비밀번호 확인 :
<input type="password" name="newPasswordCheck"><br><br>

<input type="submit" value="비밀번호 변경">
<input type="button" value="취소" onclick="location.href='/mypage/info'">

</form>

</body>
</html>