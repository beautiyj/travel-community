<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>회원정보</title>
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/mypage/user.css">
</head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">회원정보</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/user/components/sidebar.jsp"><jsp:param name="active" value="info" /></jsp:include>
        <section class="mypage-content mypage-content--form">
            <c:if test="${not empty message}">
                <p class="profile-edit__message"><c:out value="${message}" /></p>
            </c:if>
            <dl class="member-info-list">
                <div><dt>아이디</dt><dd><c:out value="${member.loginId}" default="-" /></dd></div>
                <div><dt>이름</dt><dd><c:out value="${member.name}" default="-" /></dd></div>
                <div><dt>이메일</dt><dd><c:out value="${member.email}" default="-" /></dd></div>
                <div><dt>연락처</dt><dd><c:out value="${member.phone}" default="-" /></dd></div>
                <div><dt>생일</dt><dd><c:out value="${member.birth}" default="-" /></dd></div>
                <div>
                    <dt>성별</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${member.gender eq 'MALE'}">남성</c:when>
                            <c:when test="${member.gender eq 'FEMALE'}">여성</c:when>
                            <c:otherwise>미설정</c:otherwise>
                        </c:choose>
                    </dd>
                </div>
                <div><dt>가입일</dt><dd><c:out value="${member.createdDate}" default="-" /></dd></div>
            </dl>
            <div class="member-info-actions">
                <a class="mypage-secondary-link" href="${pageContext.request.contextPath}/mypage/edit">수정하기</a>
                <a class="mypage-secondary-link" href="${pageContext.request.contextPath}/mypage/password">비밀번호 변경</a>
                <a class="member-withdraw-link" href="${pageContext.request.contextPath}/mypage/withdraw">회원탈퇴</a>
            </div>
        </section>
    </div>
</main>
</body>
</html>
