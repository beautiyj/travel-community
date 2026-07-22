<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>회원 정보 수정</title>
</head>
<body>

<h2>회원 정보 수정</h2>

<form action="/mypage/edit" method="post">

<input type="hidden" name="memberId" value="${member.memberId}">

이름 : <input type="text" name="name" value="${member.name}"><br><br>

닉네임 : <input type="text" name="nickname" value="${member.nickname}"><br><br>

전화번호 : <input type="text" name="phone" value="${member.phone}"><br><br>

<input type="submit" value="수정하기">
<input type="button" value="취소" onclick="location.href='/mypage/info'">

</form>

</body>
</html>