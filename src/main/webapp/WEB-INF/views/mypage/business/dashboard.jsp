<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp">
        <jsp:param name="title" value="사업자 승인 관리"/>
    </jsp:include>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/mypage/business.css">
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">사업자 마이페이지</h1>
    <c:if test="${not empty message}">
        <div class="biz-message"><c:out value="${message}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="biz-message biz-message--error"><c:out value="${error}"/></div>
    </c:if>

    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/business/components/sidebar.jsp">
            <jsp:param name="active" value="approval"/>
        </jsp:include>

        <section class="mypage-content" aria-labelledby="approval-title">
            <div class="mypage-content__header">
                <h2 id="approval-title">사업자 승인 관리</h2>
                <span class="business-approval-badge ${application.status eq 'APPROVED' ? 'is-approved' : ''}">
                    <c:choose>
                        <c:when test="${empty application}">미신청</c:when>
                        <c:when test="${application.status eq 'PENDING'}">승인 대기</c:when>
                        <c:when test="${application.status eq 'APPROVED'}">승인 완료</c:when>
                        <c:when test="${application.status eq 'REJECTED'}">승인 반려</c:when>
                        <c:otherwise><c:out value="${application.status}"/></c:otherwise>
                    </c:choose>
                </span>
            </div>

            <div class="business-approval-card">
                <h3>사업자등록증 제출</h3>
                <p>
                    사업자등록증을 첨부하면 관리자 확인 전까지 승인 대기 상태로 저장됩니다.
                    JPG, PNG 또는 PDF 파일을 최대 5MB까지 제출할 수 있습니다.
                </p>

                <c:if test="${not empty application}">
                    <dl class="business-approval-summary">
                        <div>
                            <dt>현재 상태</dt>
                            <dd>
                                <c:choose>
                                    <c:when test="${application.status eq 'PENDING'}">승인 대기</c:when>
                                    <c:when test="${application.status eq 'APPROVED'}">승인 완료</c:when>
                                    <c:when test="${application.status eq 'REJECTED'}">승인 반려</c:when>
                                    <c:otherwise><c:out value="${application.status}"/></c:otherwise>
                                </c:choose>
                            </dd>
                        </div>
                        <div>
                            <dt>최근 제출일</dt>
                            <dd><c:out value="${application.createdAt}" default="-"/></dd>
                        </div>
                    </dl>
                </c:if>

                <form class="business-approval-form"
                      action="${pageContext.request.contextPath}/mypage/business-info/approval"
                      method="post" enctype="multipart/form-data">
                    <label for="document">사업자등록증 파일</label>
                    <div class="business-file-input">
                        <span id="document-file-name"
                              class="business-file-input__name">선택된 파일 없음</span>
                        <label class="business-file-input__button"
                               for="document">파일 선택</label>
                        <input id="document" name="document" type="file" required
                               accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf">
                    </div>
                    <small class="business-file-input__help">
                        JPG, JPEG, PNG, PDF 형식, 최대 5MB
                    </small>
                    <button class="mypage-primary-link" type="submit">
                        <c:out value="${empty application ? '승인 신청' : '사업자등록증 다시 제출'}"/>
                    </button>
                </form>
            </div>
        </section>
    </div>
</main>
<script>
    document.getElementById("document").addEventListener("change", function () {
        document.getElementById("document-file-name").textContent =
            this.files.length ? this.files[0].name : "선택된 파일 없음";
    });
</script>
</body>
</html>
