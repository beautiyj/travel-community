<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>회원 가입 중</title>
	<script defer src="${pageContext.request.contextPath}/js/auth/auth-history-guard.js"></script>
</head>

<%-- 가입 완료 뒤 BFCache로 이전 가입 화면이 노출되지 않도록 같은 세션 확인 URL을 전달한다. --%>
<body data-session-status-url="${pageContext.request.contextPath}/auth/api/session-status">
	<%-- 배포 경로가 달라져도 현재 애플리케이션의 로그인 화면으로 이동한다. --%>
	<script>
		window.addEventListener("DOMContentLoaded", () => {
			alert("회원 가입 성공");
			location.href = "${pageContext.request.contextPath}/auth/login";
		}, { once: true });
	</script>
</body>
</html>
