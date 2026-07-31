<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="사업자 기본 정보"/></jsp:include>
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <c:if test="${not empty message}"><div class="biz-message"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="biz-message biz-message--error"><c:out value="${error}"/></div></c:if>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="info"/></jsp:include>
        <section class="mypage-content" aria-labelledby="business-info-title">
            <div class="mypage-content__header"><h2 id="business-info-title">기본 정보</h2></div>
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
                <div><dt>주소</dt><dd><c:out value="${member.address}" default="-"/></dd></div>
                <div>
                    <dt>사업자 승인 상태</dt>
                    <dd>${application.status eq 'APPROVED' ? '사업자' : '대기'}</dd>
                </div>
                <div><dt>가입일</dt><dd><c:out value="${member.createdDate}" default="-"/></dd></div>
            </dl>
            <div class="member-info-actions">
                <a class="mypage-secondary-link"
                   href="${pageContext.request.contextPath}/mypage/business-info/edit">수정하기</a>
                <a class="mypage-secondary-link"
                   href="${pageContext.request.contextPath}/mypage/business-info/password">비밀번호 변경</a>
            </div>
        </section>
    </div>
</main>
</body>
</html>
