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
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="info"/></jsp:include>
        <section class="mypage-content">
            <a class="profile-edit__back"
               href="${pageContext.request.contextPath}/mypage/business-info"
               aria-label="기본 정보 화면으로 돌아가기">
                <span aria-hidden="true">‹</span> 회원정보
            </a>
            <div class="mypage-content__header"><h2>회원정보 수정</h2></div>
            <c:if test="${not empty error}">
                <div class="biz-message biz-message--error"><c:out value="${error}"/></div>
            </c:if>
            <form class="business-member-form"
                  action="${pageContext.request.contextPath}/mypage/business-info/edit"
                  method="post" enctype="multipart/form-data">
                <div class="business-profile-editor">
                    <div class="business-profile-editor__image">
                        <c:choose>
                            <c:when test="${not empty member.profileImgUrl}">
                                <img src="${pageContext.request.contextPath}<c:out value='${member.profileImgUrl}'/>"
                                     alt="프로필 이미지">
                            </c:when>
                            <c:otherwise><span>프로필</span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="business-profile-editor__controls">
                        <input name="profileImage" type="file"
                               accept=".jpg,.jpeg,.png,.webp">
                        <button class="mypage-primary-link" type="submit">
                            프로필 이미지 변경
                        </button>
                    </div>
                </div>
                <label>아이디<input value="<c:out value='${member.loginId}'/>" readonly></label>
                <label>이름<input name="name" value="<c:out value='${member.name}'/>" required maxlength="20"></label>
                <label>이메일<input value="<c:out value='${member.email}'/>" readonly></label>
                <label>연락처<input name="phone" value="<c:out value='${member.phone}'/>" maxlength="20"></label>
                <div class="biz-actions"><button class="biz-btn" type="submit">저장</button><a class="biz-btn biz-btn--muted" href="${pageContext.request.contextPath}/mypage/business-info">취소</a></div>
            </form>
        </section>
    </div>
</main>
</body>
</html>
