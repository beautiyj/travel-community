<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>마이페이지</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/mypage/user.css">
</head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp"><jsp:param name="accountType" value="USER"/><jsp:param name="active" value=""/></jsp:include>
        <section class="mypage-content mypage-hub" aria-label="마이페이지 메뉴">
            <a href="${pageContext.request.contextPath}/mypage/info"><b>회원정보</b><span>내 정보를 확인하고 수정합니다</span></a>
            <a href="${pageContext.request.contextPath}/mypage/wishlist"><b>찜목록</b><span>관심 있는 여행지를 확인합니다</span></a>
            <a href="${pageContext.request.contextPath}/mypage/reservation"><b>예약목록</b><span>예약 내역과 상태를 확인합니다</span></a>
        </section>
    </div>
</main>
</body>
</html>
        
