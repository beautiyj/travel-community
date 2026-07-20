<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>회원 정보</title>
</head>
<body>

<h2>회원 정보</h2>

이름 : ${member.name}<br><br>

아이디 : ${member.loginId}<br><br>

닉네임 : ${member.nickname}<br><br>

전화번호 : ${member.phone}<br><br>

성별 : ${member.gender}<br><br>

생년월일 : ${member.birth}<br><br>

가입 방식 : ${member.signupType}<br><br>

가입일 : ${member.createdAt}<br><br>

<hr>

<hr>

<a href="${pageContext.request.contextPath}/mypage/edit">회원정보 수정</a>

<br><br>

<a href="${pageContext.request.contextPath}/mypage/password">비밀번호 변경</a>
</body>
</html>