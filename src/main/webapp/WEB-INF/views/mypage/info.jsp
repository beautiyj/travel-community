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

회원 번호 : ${member.memberId}<br><br>

이름 : ${member.name}<br><br>

아이디 : ${member.loginId}<br><br>

닉네임 : ${member.nickname}<br><br>

회원 등급 : ${member.memberType}<br><br>

전화번호 : ${member.phone}<br><br>

성별 : ${member.gender}<br><br>

생년월일 : ${member.birth}<br><br>

프로필 이미지 : ${member.profileImgUrl}<br><br>

가입 방식 : ${member.signupType}<br><br>

회원 상태 : ${member.memberStatus}<br><br>

이메일 인증 : ${member.emailVerified}<br><br>

가입일 : ${member.createdAt}<br><br>

수정일 : ${member.updatedAt}<br><br>

<hr>

<a href="/mypage/edit">회원정보 수정</a>

</body>
</html>