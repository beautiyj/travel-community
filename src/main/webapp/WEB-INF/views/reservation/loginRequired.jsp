<%@ page contentType="text/html;charset=UTF-8" %>
<%-- 비로그인 상태로 예약 폼 진입 시 안내. 진짜 HTTP 리다이렉트로는 alert을 띄울 수 없어서
     이 빈 화면을 한 번 거쳐 스크립트로 alert 후 로그인 페이지로 보낸다. --%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>로그인 필요</title>
</head>
<body>
<script>
    alert('${message}');
    location.href = '${pageContext.request.contextPath}/auth/login';
</script>
</body>
</html>
