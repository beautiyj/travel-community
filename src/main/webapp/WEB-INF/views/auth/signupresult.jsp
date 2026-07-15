<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>회원 가입 중</title>
</head>
	<body>
	<!-- [수정] 회원가입 완료 후 로그인 경로에 컨텍스트 경로를 반영한다. -->
	<script>
		alert("회원 가입 성공");
		location.href="${pageContext.request.contextPath}/auth/login";
	</script>
</body>
</html>
