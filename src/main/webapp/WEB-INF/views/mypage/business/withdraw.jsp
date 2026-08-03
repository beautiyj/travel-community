<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="사업자 회원 탈퇴"/></jsp:include>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/mypage/business.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">사업자 마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"/>
        <section class="mypage-content">
            <div class="mypage-content__header"><h2>회원 탈퇴</h2></div>
            <div class="biz-card"><p>탈퇴하면 사업자 회원 계정과 연결된 서비스 이용이 중지됩니다.</p><form class="biz-actions" action="${pageContext.request.contextPath}/business/mypage/withdraw" method="post" onsubmit="return confirm('정말 탈퇴하시겠습니까?');"><button class="biz-btn" type="submit">탈퇴하기</button><a class="biz-btn biz-btn--muted" href="${pageContext.request.contextPath}/business/mypage/info">취소</a></form></div>
        </section>
    </div>
</main>
</body>
</html>
