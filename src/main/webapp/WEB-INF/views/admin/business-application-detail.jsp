<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>사업자 인증 신청 상세 - 갈래말래</title>
    <c:url var="commonCssUrl" value="/css/common.css"/>
    <c:url var="adminCssUrl" value="/css/admin/admin.css"/>
    <c:url var="listUrl" value="/admin/business-applications"/>
    <c:url var="documentUrl"
           value="/admin/business-applications/${application.businessApplicationId}/document"/>
    <c:url var="approveUrl"
           value="/admin/business-applications/${application.businessApplicationId}/approve"/>
    <c:url var="rejectUrl"
           value="/admin/business-applications/${application.businessApplicationId}/reject"/>
    <link rel="stylesheet" href="${commonCssUrl}">
    <link rel="stylesheet" href="${adminCssUrl}">
</head>
<body>
<div class="admin-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="businessApplications"/>
    </jsp:include>

    <main class="admin-main">
        <header class="admin-topbar">
            <h1 class="admin-topbar__title">사업자 인증 신청 상세</h1>
            <span class="admin-topbar__caption">ADMIN</span>
        </header>

        <div class="admin-content admin-content--detail">
            <div class="admin-detail-heading">
                <div>
                    <a class="admin-back-link" href="${listUrl}">← 신청 목록</a>
                    <div class="admin-detail-heading__title-row">
                        <h2 class="admin-page-heading__title">
                            신청 #<c:out value="${application.businessApplicationId}"/>
                        </h2>
                        <c:choose>
                            <c:when test="${application.applicationStatus eq 'PENDING'}">
                                <span class="admin-status admin-status--pending">심사 대기</span>
                            </c:when>
                            <c:when test="${application.applicationStatus eq 'APPROVED'}">
                                <span class="admin-status admin-status--approved">승인</span>
                            </c:when>
                            <c:when test="${application.applicationStatus eq 'REJECTED'}">
                                <span class="admin-status admin-status--rejected">반려</span>
                            </c:when>
                            <c:otherwise>
                                <span class="admin-status">
                                    <c:out value="${application.applicationStatus}"/>
                                </span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <p class="admin-page-heading__description">
                        신청일 <c:out value="${application.appliedAtDisplay}"/>
                    </p>
                </div>
            </div>

            <c:if test="${param.reviewed eq 'approved'}">
                <div class="admin-alert admin-alert--success" role="status">
                    사업자 인증 신청을 승인했습니다.
                </div>
            </c:if>
            <c:if test="${param.reviewed eq 'rejected'}">
                <div class="admin-alert admin-alert--success" role="status">
                    사업자 인증 신청을 반려했습니다.
                </div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <div class="admin-alert admin-alert--error" role="alert">
                    <c:out value="${errorMessage}"/>
                </div>
            </c:if>

            <section class="admin-card" aria-labelledby="member-info-title">
                <div class="admin-card__header">
                    <div>
                        <h3 id="member-info-title" class="admin-card__title">사업자 회원 정보</h3>
                        <p class="admin-card__description">신청 회원의 기본 정보를 확인합니다.</p>
                    </div>
                </div>
                <dl class="admin-info-grid">
                    <div class="admin-info-item">
                        <dt>회원 번호</dt>
                        <dd><c:out value="${application.memberId}"/></dd>
                    </div>
                    <div class="admin-info-item">
                        <dt>이름</dt>
                        <dd><c:out value="${application.name}"/></dd>
                    </div>
                    <div class="admin-info-item">
                        <dt>생년월일</dt>
                        <dd><c:out value="${application.birth}"/></dd>
                    </div>
                    <div class="admin-info-item">
                        <dt>전화번호</dt>
                        <dd><c:out value="${application.phone}"/></dd>
                    </div>
                </dl>
            </section>

            <section class="admin-card" aria-labelledby="document-title">
                <div class="admin-card__header">
                    <div>
                        <h3 id="document-title" class="admin-card__title">사업자 등록증</h3>
                        <p class="admin-card__description">등록증의 상호와 대표자 정보를 회원 정보와 대조해 주세요.</p>
                    </div>
                    <a class="admin-btn admin-btn--outline admin-btn--sm"
                       href="${documentUrl}"
                       target="_blank"
                       rel="noopener">
                        원본 보기
                    </a>
                </div>
                <div class="admin-document">
                    <img src="${documentUrl}" alt="사업자 등록증 제출 이미지">
                </div>
            </section>

            <c:choose>
                <c:when test="${application.applicationStatus eq 'PENDING'}">
                    <section class="admin-card" aria-labelledby="review-title">
                        <div class="admin-card__header">
                            <div>
                                <h3 id="review-title" class="admin-card__title">심사 결정</h3>
                                <p class="admin-card__description">
                                    승인과 반려는 처리 후 되돌릴 수 없으므로 등록증 정보를 다시 확인해 주세요.
                                </p>
                            </div>
                        </div>

                        <div class="admin-review-grid">
                            <div class="admin-review-panel admin-review-panel--approve">
                                <h4 class="admin-review-panel__title">승인</h4>
                                <p class="admin-review-panel__description">
                                    신청을 승인하면 해당 회원의 사업자 권한이 활성화됩니다.
                                </p>
                                <form class="admin-review-form"
                                      method="post"
                                      action="${approveUrl}"
                                      data-confirm-message="이 사업자 인증 신청을 승인하시겠습니까?">
                                    <button class="admin-btn admin-btn--approve admin-btn--block"
                                            type="submit"
                                            data-submitting-label="승인 처리 중">
                                        신청 승인
                                    </button>
                                </form>
                            </div>

                            <div class="admin-review-panel admin-review-panel--reject">
                                <h4 class="admin-review-panel__title">반려</h4>
                                <p class="admin-review-panel__description">
                                    사업자가 보완할 내용을 알 수 있도록 구체적인 사유를 작성해 주세요.
                                </p>
                                <form class="admin-review-form"
                                      method="post"
                                      action="${rejectUrl}"
                                      data-confirm-message="입력한 사유로 이 신청을 반려하시겠습니까?">
                                    <label class="admin-form-label" for="rejection-reason">반려 사유</label>
                                    <textarea class="admin-form-textarea"
                                              id="rejection-reason"
                                              name="reason"
                                              rows="6"
                                              maxlength="500"
                                              required
                                              aria-describedby="rejection-reason-help rejection-reason-count"><c:out value="${rejectionReasonInput}"/></textarea>
                                    <div class="admin-form-help-row">
                                        <span id="rejection-reason-help">공백을 제외한 내용을 입력해 주세요.</span>
                                        <span id="rejection-reason-count" aria-live="polite">
                                            <strong id="rejection-reason-length">0</strong>/500자
                                        </span>
                                    </div>
                                    <button class="admin-btn admin-btn--reject admin-btn--block"
                                            type="submit"
                                            data-submitting-label="반려 처리 중">
                                        신청 반려
                                    </button>
                                </form>
                            </div>
                        </div>
                    </section>
                </c:when>
                <c:otherwise>
                    <section class="admin-card" aria-labelledby="review-result-title">
                        <div class="admin-card__header">
                            <div>
                                <h3 id="review-result-title" class="admin-card__title">심사 결과</h3>
                                <p class="admin-card__description">이미 처리가 완료된 신청입니다.</p>
                            </div>
                        </div>
                        <dl class="admin-info-grid">
                            <div class="admin-info-item">
                                <dt>처리 상태</dt>
                                <dd>
                                    <c:choose>
                                        <c:when test="${application.applicationStatus eq 'APPROVED'}">승인</c:when>
                                        <c:when test="${application.applicationStatus eq 'REJECTED'}">반려</c:when>
                                        <c:otherwise>
                                            <c:out value="${application.applicationStatus}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </dd>
                            </div>
                            <div class="admin-info-item">
                                <dt>처리 일시</dt>
                                <dd><c:out value="${application.reviewedAtDisplay}"/></dd>
                            </div>
                            <c:if test="${not empty application.reviewedBy}">
                                <div class="admin-info-item">
                                    <dt>처리 관리자 번호</dt>
                                    <dd><c:out value="${application.reviewedBy}"/></dd>
                                </div>
                            </c:if>
                        </dl>

                        <c:if test="${application.applicationStatus eq 'REJECTED'}">
                            <div class="admin-rejection-result">
                                <h4 class="admin-rejection-result__title">반려 사유</h4>
                                <p class="admin-rejection-result__reason"><c:out value="${application.rejectionReason}"/></p>
                            </div>
                        </c:if>
                    </section>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<script>
    (function () {
        // 반려 사유를 입력하는 동안 현재 글자 수를 바로 보여 줍니다.
        const reasonField = document.getElementById('rejection-reason');
        const reasonLength = document.getElementById('rejection-reason-length');

        if (reasonField && reasonLength) {
            const updateReasonLength = function () {
                reasonLength.textContent = String(reasonField.value.length);
            };
            reasonField.addEventListener('input', updateReasonLength);
            updateReasonLength();
        }

        // 최종 확인 뒤 버튼을 잠가 승인·반려 요청이 중복 제출되는 것을 막습니다.
        document.querySelectorAll('.admin-review-form').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                const message = form.getAttribute('data-confirm-message');
                if (message && !window.confirm(message)) {
                    event.preventDefault();
                    return;
                }

                form.querySelectorAll('button[type="submit"]').forEach(function (button) {
                    button.disabled = true;
                    const submittingLabel = button.getAttribute('data-submitting-label');
                    if (submittingLabel) {
                        button.textContent = submittingLabel;
                    }
                });
            });
        });
    }());
</script>
</body>
</html>
