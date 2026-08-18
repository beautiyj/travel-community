<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>마이페이지</title>
</head>
<body>

    <h1>마이페이지</h1>

    <ul>
        <li>
            <a href="${pageContext.request.contextPath}/mypage/info">
                회원 정보
            </a>
        </li>

        <li>
            <a href="${pageContext.request.contextPath}/mypage/reservation">
                예약 조회
            </a>
        </li>

        <li>
            <a href="${pageContext.request.contextPath}/mypage/wishlist">
                찜 목록
            </a>
        </li>

        <li>
            <a href="${pageContext.request.contextPath}/mypage/withdraw">
                회원 탈퇴
            </a>
        </li>

        <li>
            <a href="${pageContext.request.contextPath}/mypage/logout">
                로그아웃
            </a>
        </li>
    </ul>

</body>
</html>