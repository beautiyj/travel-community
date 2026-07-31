<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp">
        <jsp:param name="title" value="사업자 승인 관리"/>
    </jsp:include>
</head>
<body class="business-mypage">
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
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
                            <dd><c:out value="${empty application.createdAt ? '-' : fn:replace(application.createdAt, 'T', ' ')}"/></dd>
                        </div>
                    </dl>
                </c:if>

                <c:choose>
                    <c:when test="${application.status eq 'PENDING'}">
                        <div class="business-approval-cancel-form">
                            <button class="business-approval-cancel-button js-business-approval-cancel-open"
                                    type="button">
                                사업자 등록 취소
                            </button>
                        </div>
                    </c:when>
                    <c:otherwise>
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
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </div>
</main>

<c:if test="${application.status eq 'PENDING'}">
    <div class="cancel-modal" id="businessApprovalCancelModal" hidden>
        <div class="cancel-modal__dialog"
             role="alertdialog"
             aria-modal="true"
             aria-labelledby="business-cancel-title"
             aria-describedby="business-cancel-description">
            <div class="cancel-modal__header">
                <div>
                    <h2 id="business-cancel-title">사업자 등록 취소</h2>
                    <p>승인 대기 중인 신청을 취소합니다.</p>
                </div>
                <button class="cancel-modal__close js-business-approval-cancel-close"
                        type="button" aria-label="닫기">×</button>
            </div>
            <div class="cancel-modal__body">
                <p id="business-cancel-description"
                   class="business-approval-cancel-confirm">
                    사업자 등록을 취소하시겠습니까?
                </p>
                <p class="cancel-modal__notice">
                    취소한 신청은 복구할 수 없으며, 사업자 승인을 받으려면
                    사업자등록증을 다시 제출해야 합니다.
                </p>
            </div>
            <form class="cancel-modal__actions"
                  action="${pageContext.request.contextPath}/mypage/business-info/approval/cancel"
                  method="post">
                <button class="cancel-modal__back js-business-approval-cancel-close"
                        type="button">돌아가기</button>
                <button class="cancel-modal__submit" type="submit">등록 취소</button>
            </form>
        </div>
    </div>
</c:if>

<script>
    const documentInput = document.getElementById("document");
    if (documentInput) {
        documentInput.addEventListener("change", function () {
            document.getElementById("document-file-name").textContent =
                this.files.length ? this.files[0].name : "선택된 파일 없음";
        });
    }

    const approvalCancelModal =
        document.getElementById("businessApprovalCancelModal");
    if (approvalCancelModal) {
        const openButton = document.querySelector(
            ".js-business-approval-cancel-open");
        const closeButtons = approvalCancelModal.querySelectorAll(
            ".js-business-approval-cancel-close");

        openButton.addEventListener("click", function () {
            approvalCancelModal.hidden = false;
        });
        closeButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                approvalCancelModal.hidden = true;
            });
        });
        approvalCancelModal.addEventListener("click", function (event) {
            if (event.target === approvalCancelModal) {
                approvalCancelModal.hidden = true;
            }
        });
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && !approvalCancelModal.hidden) {
                approvalCancelModal.hidden = true;
            }
        });
    }
</script>
</body>
</html>
