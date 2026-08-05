<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>사업자 기본 정보</title>
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/mypage/business.css">
    <link rel="stylesheet" href="/css/components/buttonComponent.css">
    <link rel="stylesheet" href="/css/components/confirmModal.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">기본 정보</h1>
    <c:if test="${not empty message}"><div class="biz-message"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="biz-message biz-message--error"><c:out value="${error}"/></div></c:if>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="info"/></jsp:include>
        <section class="mypage-content mypage-content--form">
            <dl class="member-info-list">
                <div><dt>아이디</dt><dd><c:out value="${member.loginId}" default="-"/></dd></div>
                <div><dt>이름</dt><dd><c:out value="${member.name}" default="-"/></dd></div>
                <div><dt>이메일</dt><dd><c:out value="${member.email}" default="-"/></dd></div>
                <div><dt>연락처</dt><dd><c:out value="${member.phone}" default="-"/></dd></div>
                <div><dt>생일</dt><dd><c:out value="${member.birth}" default="-"/></dd></div>
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
                <div>
                    <dt>사업자 승인 상태</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${application.status eq 'PENDING'}">승인 대기</c:when>
                            <c:when test="${application.status eq 'APPROVED'}">승인 완료</c:when>
                            <c:when test="${application.status eq 'REJECTED'}">승인 반려</c:when>
                            <c:otherwise><c:out value="${application.status}"/></c:otherwise>
                        </c:choose>
                    </dd>
                </div>
                <c:if test="${application.status eq 'REJECTED' and not empty application.rejectionReason}">
                    <div>
                        <dt>반려 사유</dt>
                        <dd><c:out value="${application.rejectionReason}"/></dd>
                    </div>
                </c:if>
                <div><dt>가입일</dt><dd><c:out value="${member.createdDate}" default="-"/></dd></div>
            </dl>
            <div class="member-info-actions">
                <a class="mypage-secondary-link"
                   href="${pageContext.request.contextPath}/mypage/business-info/edit">수정하기</a>
                <a class="mypage-secondary-link"
                   href="${pageContext.request.contextPath}/mypage/business-info/password">비밀번호 변경</a>
                <a class="member-withdraw-link"
                   href="${pageContext.request.contextPath}/mypage/business-info/withdraw">회원탈퇴</a>
            </div>
        </section>
    </div>
</main>
<script src="/js/common.js"></script>
</body>
</html>
