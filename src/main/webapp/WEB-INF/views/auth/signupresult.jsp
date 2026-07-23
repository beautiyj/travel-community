<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>회원 가입 중</title>
</head>

<body>
	<!-- 배포 경로가 달라져도 현재 애플리케이션의 로그인 화면으로 이동한다. -->
	<script>
		alert("회원 가입 성공");
		location.href = "${pageContext.request.contextPath}/auth/login";
	</script>
</body>
</html>
