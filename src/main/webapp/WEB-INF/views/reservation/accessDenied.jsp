<%@ page contentType="text/html;charset=UTF-8" %>
<%-- 사업자 계정이 예약 폼(GET /reservations/new)·예약 생성(POST /reservations)에 진입 시 안내.
     loginRequired.jsp와 동일하게 진짜 HTTP 리다이렉트로는 alert을 띄울 수 없어서
     이 빈 화면을 한 번 거쳐 스크립트로 alert 후 홈으로 보낸다. --%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>이용 제한</title>
</head>
<body>
<script>
    alert('${message}');
    location.href = '${pageContext.request.contextPath}/';
</script>
</body>
</html>
