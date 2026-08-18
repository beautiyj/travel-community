<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>회원 탈퇴</title>
</head>
<body>

    <h1>회원 탈퇴</h1>

    <p>회원 탈퇴 후에는 일부 정보를 복구할 수 없습니다.</p>
    <p>정말로 탈퇴하시겠습니까?</p>

    <form action="${pageContext.request.contextPath}/mypage/withdraw"
          method="post"
          onsubmit="return confirm('정말 회원 탈퇴하시겠습니까?');">

        <button type="submit">회원 탈퇴</button>

        <button type="button"
                onclick="location.href='${pageContext.request.contextPath}/mypage'">
            취소
        </button>

    </form>

</body>
</html>