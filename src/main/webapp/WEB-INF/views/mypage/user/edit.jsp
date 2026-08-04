<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<jsp:include page="/WEB-INF/views/mypage/common/pageHead.jsp">
    <jsp:param name="title" value="회원정보 수정" />
</jsp:include>
</head>
<body>

<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/user/components/sidebar.jsp">
            <jsp:param name="active" value="info" />
        </jsp:include>
        <section class="mypage-content mypage-content--form" aria-labelledby="profile-edit-title">
        <div class="mypage-content__header">
            <h2 id="profile-edit-title">회원정보 수정</h2>
        </div>

        <c:if test="${not empty error}">
            <p class="profile-edit__message profile-edit__message--error">
                <c:out value="${error}" />
            </p>
        </c:if>

        <form class="profile-edit__form mypage-form-card" action="${cp}/mypage/edit"
              method="post" enctype="multipart/form-data"
              data-edit-confirm>
            <input type="hidden" name="memberId" value="<c:out value='${member.memberId}'/>">
            <input type="hidden" name="nickname" value="<c:out value='${member.nickname}'/>">

            <div class="profile-edit__profile-card">
                <div class="profile-edit__profile-image">
                    <c:choose>
                        <c:when test="${not empty member.profileImgUrl}">
                            <img src="${cp}<c:out value='${member.profileImgUrl}'/>"
                                 alt="현재 프로필 이미지">
                        </c:when>
                        <c:otherwise><span>프로필</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="profile-edit__profile-controls">
                    <input id="profileImage" name="profileImage" type="file"
                           accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp">
                    <span id="profileImageName"
                          class="profile-edit__file-name"
                          aria-live="polite">선택된 파일 없음</span>
                    <label class="profile-edit__profile-button"
                           for="profileImage">파일 선택</label>
                    <small>JPG, PNG, WEBP 형식, 최대 5MB</small>
                </div>
            </div>

            <jsp:include page="/WEB-INF/views/mypage/common/readOnlyField.jsp">
                <jsp:param name="label" value="아이디" />
                <jsp:param name="id" value="loginId" />
                <jsp:param name="value" value="${member.loginId}" />
                <jsp:param name="helpText" value="수정이 불가능한 항목입니다" />
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/inputField.jsp">
                <jsp:param name="label" value="이름" />
                <jsp:param name="name" value="name" />
                <jsp:param name="value" value="${member.name}" />
                <jsp:param name="maxlength" value="50" />
                <jsp:param name="required" value="true" />
            </jsp:include>

            <jsp:include page="/WEB-INF/views/mypage/common/readOnlyField.jsp">
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

            <jsp:include page="/WEB-INF/views/mypage/common/readOnlyField.jsp">
                <jsp:param name="label" value="생일" />
                <jsp:param name="id" value="birth" />
                <jsp:param name="value" value="${member.birth}" />
                <jsp:param name="helpText" value="생일은 수정할 수 없습니다." />
            </jsp:include>

            <div class="input-field">
                <label for="gender">성별</label>
                <select id="gender" name="gender">
                    <option value="" ${empty member.gender ? 'selected' : ''}>선택 안 함</option>
                    <option value="MALE" ${member.gender eq 'MALE' ? 'selected' : ''}>남성</option>
                    <option value="FEMALE" ${member.gender eq 'FEMALE' ? 'selected' : ''}>여성</option>
                </select>
            </div>

            <jsp:include page="/WEB-INF/views/common/inputField.jsp">
                <jsp:param name="label" value="주소" />
                <jsp:param name="name" value="address" />
                <jsp:param name="value" value="${member.address}" />
                <jsp:param name="placeholder" value="주소를 입력해 주세요" />
                <jsp:param name="maxlength" value="255" />
            </jsp:include>

            <div class="profile-edit__actions">
                <a class="profile-edit__cancel" href="${cp}/mypage/info">취소</a>
                <button class="profile-edit__submit" type="submit">저장</button>
            </div>
        </form>
        </section>
    </div>
</main>

<jsp:include page="/WEB-INF/views/mypage/common/editConfirmModal.jsp"/>

<script>
    document.getElementById("profileImage").addEventListener("change", function () {
        document.getElementById("profileImageName").textContent =
            this.files.length ? this.files[0].name : "선택된 파일 없음";
    });
</script>
</body>
</html>
