<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="사업자 기본 정보"/></jsp:include>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/mypage/business.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">사업자 마이페이지</h1>
    <c:if test="${not empty message}"><div class="biz-message"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="biz-message biz-message--error"><c:out value="${error}"/></div></c:if>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="info"/></jsp:include>
        <section class="mypage-content" aria-labelledby="business-info-title">
            <div class="mypage-content__header"><h2 id="business-info-title">기본 정보</h2><a class="mypage-primary-link" href="${pageContext.request.contextPath}/business/mypage/edit">수정하기</a></div>
            <div class="business-profile-editor">
                <div class="business-profile-editor__image">
                    <c:choose>
                        <c:when test="${not empty member.profileImgUrl}"><img src="${pageContext.request.contextPath}<c:out value='${member.profileImgUrl}'/>" alt="프로필 이미지"></c:when>
                        <c:otherwise><span>프로필</span></c:otherwise>
                    </c:choose>
                </div>
                <form action="${pageContext.request.contextPath}/business/mypage/profile-image" method="post" enctype="multipart/form-data">
                    <input name="profileImage" type="file" required accept=".jpg,.jpeg,.png,.webp">
                    <button class="mypage-primary-link" type="submit">프로필 이미지 변경</button>
                </form>
            </div>
            <dl class="member-info-list">
                <div><dt>아이디</dt><dd><c:out value="${member.loginId}" default="-"/></dd></div>
                <div><dt>이름</dt><dd><c:out value="${member.name}" default="-"/></dd></div>
                <div><dt>닉네임</dt><dd><c:out value="${member.nickname}" default="-"/></dd></div>
                <div><dt>이메일</dt><dd><c:out value="${member.email}" default="-"/></dd></div>
                <div><dt>연락처</dt><dd><c:out value="${member.phone}" default="-"/></dd></div>
                <div><dt>회원 유형</dt><dd>사업자 회원</dd></div>
                <div><dt>사업자 권한</dt><dd><c:out value="${member.memberRole}" default="USER"/></dd></div>
                <div><dt>승인 상태</dt><dd><c:out value="${empty application ? '신청정보 없음' : application.status}"/></dd></div>
                <div><dt>가입일</dt><dd><c:out value="${member.createdAt}" default="-"/></dd></div>
            </dl>
            <c:if test="${not empty application and application.status eq 'APPROVED'}">
                <section class="business-reapproval">
                    <h3>사업자 재승인 요청</h3>
                    <p>사업자등록증이 변경된 경우 새 파일을 제출하면 승인 상태가 다시 PENDING으로 변경됩니다.</p>
                    <form action="${pageContext.request.contextPath}/business/mypage/reapproval" method="post" enctype="multipart/form-data">
                        <input name="document" type="file" required accept=".jpg,.jpeg,.png,.pdf">
                        <button class="mypage-primary-link" type="submit">재승인 요청</button>
                    </form>
                </section>
            </c:if>
        </section>
    </div>
</main>
</body>
</html>
