<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="사업자 회원정보 수정"/></jsp:include>
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp"><jsp:param name="active" value="info"/></jsp:include>
        <section class="mypage-content mypage-content--form">
            <div class="mypage-content__header"><h2>회원정보 수정</h2></div>
            <c:if test="${not empty error}">
                <p class="profile-edit__message profile-edit__message--error">
                    <c:out value="${error}"/>
                </p>
            </c:if>
            <form class="profile-edit__form mypage-form-card"
                  action="${pageContext.request.contextPath}/mypage/business-info/edit"
                  method="post" enctype="multipart/form-data"
                  data-edit-confirm>
                <div class="profile-edit__profile-card">
                    <div class="profile-edit__profile-image">
                        <c:choose>
                            <c:when test="${not empty member.profileImgUrl}">
                                <img src="${pageContext.request.contextPath}<c:out value='${member.profileImgUrl}'/>"
                                     alt="현재 프로필 이미지">
                            </c:when>
                            <c:otherwise><span>프로필</span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="profile-edit__profile-controls">
                        <input id="businessProfileImage" name="profileImage" type="file"
                               accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp">
                        <span id="businessProfileImageName"
                              class="profile-edit__file-name"
                              aria-live="polite">선택된 파일 없음</span>
                        <label class="profile-edit__profile-button"
                               for="businessProfileImage">파일 선택</label>
                        <small>JPG, PNG, WEBP 형식, 최대 5MB</small>
                    </div>
                </div>

                <div class="input-field input-field--readonly">
                    <label for="businessLoginId">아이디</label>
                    <input id="businessLoginId" type="text"
                           value="<c:out value='${member.loginId}'/>"
                           readonly aria-readonly="true">
                    <p class="input-field__help">수정이 불가능한 항목입니다</p>
                </div>

                <div class="input-field">
                    <label for="businessName">이름</label>
                    <input id="businessName" name="name" type="text"
                           value="<c:out value='${member.name}'/>"
                           required maxlength="20">
                </div>

                <div class="input-field input-field--readonly">
                    <label for="businessEmail">이메일</label>
                    <input id="businessEmail" type="email"
                           value="<c:out value='${member.email}'/>"
                           readonly aria-readonly="true">
                </div>

                <div class="input-field">
                    <label for="businessPhone">연락처</label>
                    <input id="businessPhone" name="phone" type="tel"
                           value="<c:out value='${member.phone}'/>"
                           placeholder="010-1234-5678" maxlength="20">
                </div>

                <div class="input-field input-field--readonly">
                    <label for="businessBirth">생일</label>
                    <input id="businessBirth" type="text"
                           value="<c:out value='${member.birth}'/>"
                           readonly aria-readonly="true">
                    <p class="input-field__help">생일은 수정할 수 없습니다.</p>
                </div>

                <div class="input-field">
                    <label for="businessGender">성별</label>
                    <select id="businessGender" name="gender">
                        <option value="" ${empty member.gender ? 'selected' : ''}>선택 안 함</option>
                        <option value="MALE" ${member.gender eq 'MALE' ? 'selected' : ''}>남성</option>
                        <option value="FEMALE" ${member.gender eq 'FEMALE' ? 'selected' : ''}>여성</option>
                    </select>
                </div>

                <div class="input-field">
                    <label for="businessAddress">주소</label>
                    <input id="businessAddress" name="address" type="text"
                           value="<c:out value='${member.address}'/>"
                           maxlength="255" placeholder="주소를 입력해 주세요">
                </div>

                <div class="profile-edit__actions">
                    <a class="profile-edit__cancel"
                       href="${pageContext.request.contextPath}/mypage/business-info">취소</a>
                    <button class="profile-edit__submit" type="submit">저장</button>
                </div>
            </form>
        </section>
    </div>
</main>
<jsp:include page="/WEB-INF/views/mypage/components/editConfirmModal.jsp"/>
<script>
    document.getElementById("businessProfileImage").addEventListener("change", function () {
        document.getElementById("businessProfileImageName").textContent =
            this.files.length ? this.files[0].name : "선택된 파일 없음";
    });
</script>
</body>
</html>
