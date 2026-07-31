<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="사업자 회원정보 수정"/></jsp:include>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/mypage/business.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">사업자 마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="edit"/></jsp:include>
        <section class="mypage-content">
            <div class="mypage-content__header"><h2>회원정보 수정</h2></div>
            <form class="business-member-form" action="${pageContext.request.contextPath}/business/mypage/edit" method="post">
                <label>아이디<input value="<c:out value='${member.loginId}'/>" readonly></label>
                <label>이름<input name="name" value="<c:out value='${member.name}'/>" required maxlength="20"></label>
                <label>이메일<input value="<c:out value='${member.email}'/>" readonly></label>
                <label>연락처<input name="phone" value="<c:out value='${member.phone}'/>" maxlength="20"></label>
                <div class="biz-actions"><button class="biz-btn" type="submit">저장</button><a class="biz-btn biz-btn--muted" href="${pageContext.request.contextPath}/business/mypage/info">취소</a></div>
            </form>
        </section>
    </div>
</main>
</body>
</html>
