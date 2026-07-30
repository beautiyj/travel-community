<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="회원정보" /></jsp:include></head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/components/sidebar.jsp"><jsp:param name="active" value="info" /></jsp:include>
        <section class="mypage-content" aria-labelledby="member-info-title">
            <c:if test="${not empty message}">
                <p class="profile-edit__message"><c:out value="${message}" /></p>
            </c:if>
            <div class="mypage-content__header">
                <h2 id="member-info-title">회원정보</h2>
            </div>
            <dl class="member-info-list">
                <div><dt>아이디</dt><dd><c:out value="${member.loginId}" default="-" /></dd></div>
                <div><dt>이름</dt><dd><c:out value="${member.name}" default="-" /></dd></div>
                <div><dt>이메일</dt><dd><c:out value="${member.email}" default="-" /></dd></div>
                <div><dt>연락처</dt><dd><c:out value="${member.phone}" default="-" /></dd></div>
                <div><dt>가입일</dt><dd><c:out value="${member.createdDate}" default="-" /></dd></div>
            </dl>
            <div class="member-info-actions">
                <a class="mypage-secondary-link" href="${pageContext.request.contextPath}/mypage/edit">수정하기</a>
                <a class="mypage-secondary-link" href="${pageContext.request.contextPath}/mypage/password">비밀번호 변경</a>
            </div>
        </section>
    </div>
</main>
</body>
</html>
