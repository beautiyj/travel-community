<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp">
    <jsp:param name="title" value="회원정보 수정" />
</jsp:include>
</head>
<body>

<main class="profile-edit-page">
    <section class="profile-edit">
        <a class="profile-edit__back" href="${cp}/mypage/info"
           aria-label="회원정보 화면으로 돌아가기">
            <span aria-hidden="true">‹</span> 회원정보
        </a>

        <h1 class="profile-edit__title">회원정보 수정</h1>

        <form class="profile-edit__form" action="${cp}/mypage/edit" method="post">
            <input type="hidden" name="memberId" value="<c:out value='${member.memberId}'/>">
            <input type="hidden" name="nickname" value="<c:out value='${member.nickname}'/>">

            <jsp:include page="/WEB-INF/views/mypage/components/readOnlyField.jsp">
                <jsp:param name="label" value="아이디" />
                <jsp:param name="id" value="loginId" />
                <jsp:param name="value" value="${member.loginId}" />
                <jsp:param name="helpText" value="수정이 불가능한 항목입니다" />
            </jsp:include>

            <jsp:include page="/WEB-INF/views/mypage/components/readOnlyField.jsp">
                <jsp:param name="label" value="비밀번호" />
                <jsp:param name="id" value="password" />
                <jsp:param name="value" value="••••••••" />
                <jsp:param name="helpText" value="수정이 불가능한 항목입니다" />
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/inputField.jsp">
                <jsp:param name="label" value="이름" />
                <jsp:param name="name" value="name" />
                <jsp:param name="value" value="${member.name}" />
                <jsp:param name="maxlength" value="50" />
                <jsp:param name="required" value="true" />
            </jsp:include>

            <jsp:include page="/WEB-INF/views/mypage/components/readOnlyField.jsp">
                <jsp:param name="label" value="이메일" />
                <jsp:param name="id" value="email" />
                <jsp:param name="value" value="${member.email}" />
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/inputField.jsp">
                <jsp:param name="label" value="연락처" />
                <jsp:param name="name" value="phone" />
                <jsp:param name="type" value="tel" />
                <jsp:param name="value" value="${member.phone}" />
                <jsp:param name="placeholder" value="010-1234-5678" />
                <jsp:param name="maxlength" value="20" />
                <jsp:param name="required" value="true" />
            </jsp:include>

            <div class="profile-edit__actions">
                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="취소" />
                    <jsp:param name="width" value="100%" />
                    <jsp:param name="color" value="var(--card)" />
                    <jsp:param name="size" value="var(--text-sm)" />
                    <jsp:param name="onclick" value="location.href='${cp}/mypage/info'; return false;" />
                </jsp:include>

                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="저장" />
                    <jsp:param name="width" value="100%" />
                    <jsp:param name="size" value="var(--text-sm)" />
                </jsp:include>
            </div>
        </form>
    </section>
</main>

</body>
</html>
