<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>예약 관리</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mypage/common.css">
    <link rel="stylesheet" href="/css/components/tagButton.css">
    <link rel="stylesheet" href="/css/mypage/user.css">
    <link rel="stylesheet" href="/css/mypage/modal.css?v=reservation-detail-v3">
    <style>
        /* IDE가 이전 정적 CSS를 유지해도 예약 상세 UI는 현재 JSP 기준으로 표시한다. */
        .detail-modal__dialog { max-width: 560px; border-radius: 20px; }
        .detail-modal__header { align-items: center; padding: 30px 32px 14px; border-bottom: 0; }
        .detail-modal__header h2 { font-size: 24px; }
        .detail-modal__body { gap: 18px; padding: 16px 32px 24px; }
        .detail-modal__summary { padding: 8px 16px; }
        .detail-modal__summary p { padding: 9px 0; border-bottom: 1px solid var(--border); }
        .detail-modal__summary p:last-child { border-bottom: 0; }
        .detail-modal__summary strong { max-width: 260px; overflow-wrap: anywhere; text-align: right; }
        .detail-modal__section { display: grid; overflow: hidden; border: 1px solid var(--border); border-radius: 12px; background: var(--card); }
        .detail-modal__section h3 { margin: 0; padding: 12px 16px; border-bottom: 1px solid var(--border); background: var(--secondary); font-size: var(--text-sm); font-weight: var(--font-weight-bold); }
        .detail-modal__processing { padding: 5px 16px; }
        .detail-modal__processing p { display: grid; grid-template-columns: 76px minmax(0, 1fr); align-items: center; gap: 12px; padding: 8px 0; font-size: var(--text-sm); }
        .detail-modal__processing p + p { border-top: 1px solid var(--border); }
        .detail-modal__processing p > span:first-child { color: var(--muted-foreground); }
        .detail-modal__processing strong { min-width: 0; text-align: left; }
        .detail-modal__status { display: inline-flex; align-items: center; min-height: 26px; padding: 2px 9px; border-radius: 999px; font-size: var(--text-xs); font-weight: var(--font-weight-bold); }
        .detail-modal__status--paid, .detail-modal__status--pending { background: #fef3c7; color: #92400e; }
        .detail-modal__status--requested { background: #ffedd5; color: #9a3412; }
        .detail-modal__status--confirmed { background: #dbeafe; color: #1d4ed8; }
        .detail-modal__status--completed { background: #dcfce7; color: #166534; }
        .detail-modal__status--cancelled, .detail-modal__status--rejected { background: #fee2e2; color: #b91c1c; }
        .detail-modal__status--expired { background: #e5e7eb; color: #4b5563; }
        .detail-modal__reason, .detail-modal__empty { padding: 14px 16px; background: var(--card); }
        .detail-modal__reason + .detail-modal__reason, .detail-modal__reason + .detail-modal__empty { border-top: 1px solid var(--border); }
        .detail-modal__reason-label { margin: 0 0 6px; color: var(--muted-foreground); font-size: 13px; font-weight: 700; }
        .detail-modal__reason-text, .detail-modal__empty { margin: 0; color: var(--foreground); font-size: 15px; line-height: 1.6; overflow-wrap: anywhere; white-space: pre-line; }
        .detail-modal__empty { color: var(--muted-foreground); }
        .detail-modal__actions { grid-template-columns: 1fr; padding: 0 32px 30px; }
    </style>
</head>
<body>
<main class="mypage-page mypage-reservation-page">
    <h1 class="mypage-page__title mypage-reservation-topbar">
        <span>예약 관리</span>
        <span class="mypage-reservation-topbar__date" aria-label="오늘 날짜">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 10h18"/></svg>
            <c:out value="${todayLabel}" />
        </span>
    </h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/common/sidebar.jsp"><jsp:param name="accountType" value="USER"/><jsp:param name="active" value="reservation"/></jsp:include>
        <section class="mypage-content mypage-reservation-content" aria-labelledby="reservation-title">
            <h2 id="reservation-title" class="sr-only">내 예약 관리</h2>

            <c:if test="${not empty message}">
                <p class="profile-edit__message"><c:out value="${message}" /></p>
            </c:if>
            <c:if test="${not empty error}">
                <p class="profile-edit__message profile-edit__message--error"><c:out value="${error}" /></p>
            </c:if>

            <div class="mypage-reservation-filter-row">
                <div class="mypage-reservation-tabs" aria-label="예약 상태 필터">
                    <c:forEach var="tab" items="${reservationTabs}">
                        <c:url value="/mypage/reservation" var="filterUrl">
                            <c:if test="${not empty tab.value}"><c:param name="status" value="${tab.value}" /></c:if>
                        </c:url>
                        <a href="${filterUrl}"
                           class="mypage-reservation-tab${(empty tab.value and empty statusFilter) or tab.value eq statusFilter ? ' is-active' : ''}">
                            <c:choose>
                                <c:when test="${tab.value eq 'CONFIRMED'}">예약 확정</c:when>
                                <c:when test="${tab.value eq 'COMPLETED'}">이용 완료</c:when>
                                <c:otherwise><c:out value="${tab.label}" /></c:otherwise>
                            </c:choose>
                            <c:if test="${not empty tab.value}"><span><c:out value="${tab.count}" /></span></c:if>
                        </a>
                    </c:forEach>
                </div>
                <span class="mypage-reservation-total">총 <c:out value="${reservationTotalCount}" />건</span>
            </div>

            <div class="mypage-reservation-card">
                <c:choose>
                    <c:when test="${empty reservationList}">
                        <div class="mypage-empty-state"><strong>해당 상태의 예약 내역이 없습니다</strong><a href="${pageContext.request.contextPath}/">여행지 탐색하기</a></div>
                    </c:when>
                    <c:otherwise>
                        <div class="mypage-reservation-table" role="table" aria-label="내 예약 목록">
                            <div class="mypage-reservation-table__row mypage-reservation-table__row--head" role="row">
                                <div role="columnheader">장소</div>
                                <div role="columnheader">예약자</div>
                                <div role="columnheader">연락처</div>
                                <div role="columnheader">방문일</div>
                                <div role="columnheader">인원</div>
                                <div role="columnheader">금액</div>
                                <div role="columnheader">상세보기</div>
                                <div role="columnheader">상태 / 처리</div>
                            </div>
                            <c:forEach var="reservation" items="${reservationList}">
                                <div class="mypage-reservation-table__row" role="row">
                                    <div class="mypage-reservation-place" role="cell"><c:out value="${reservation.placeName}" default="장소 정보 없음" /></div>
                                    <div role="cell"><c:out value="${reservation.visitorName}" default="-" /></div>
                                    <div role="cell"><c:out value="${reservation.phone}" default="-" /></div>
                                    <div role="cell"><c:out value="${reservation.visitDate}" default="-" /></div>
                                    <div role="cell"><c:out value="${reservation.headcount}" default="0" />명</div>
                                    <div role="cell">
                                        <c:choose>
                                            <c:when test="${not empty reservation.amount}"><fmt:formatNumber value="${reservation.amount}" pattern="#,###" />원</c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div role="cell">
                                        <button type="button" class="js-detail-open"
                                                data-place-name="<c:out value='${reservation.placeName}' default='장소 정보 없음'/>"
                                                data-visitor-name="<c:out value='${reservation.visitorName}' default='-'/>"
                                                data-phone="<c:out value='${reservation.phone}' default='-'/>"
                                                data-visit-date="<c:out value='${reservation.visitDate}' default='-'/>"
                                                data-headcount="<c:out value='${reservation.headcount}' default='0'/>"
                                                data-amount="<c:out value='${reservation.amount}' default='0'/>"
                                                data-status="<c:out value='${reservation.status}'/>"
                                                data-cancel-reason="<c:out value='${reservation.cancelReason}'/>"
                                                data-cancel-reject-reason="<c:out value='${reservation.cancelRejectReason}'/>"
                                                data-reject-reason="<c:out value='${reservation.rejectReason}'/>"
                                                style="background:none;border: 1px solid var(--border);border-radius:10px;color:#000;cursor:pointer;padding:8px 10px;font-size:inherit;">상세보기</button>
                                    </div>
                                    <div class="mypage-reservation-actions" role="cell">
                                        <jsp:include page="/WEB-INF/views/mypage/common/reservationStatusBadge.jsp">
                                            <jsp:param name="status" value="${reservation.status}"/>
                                        </jsp:include>
                                        <c:if test="${reservation.status eq 'PAID'}">
                                            <button class="mypage-outline-danger js-cancel-now-open" type="button"
                                                    data-reservation-id="<c:out value='${reservation.reservationId}'/>"
                                                    data-place-name="<c:out value='${reservation.placeName}' default='장소 정보 없음'/>"
                                                    data-visit-date="<c:out value='${reservation.visitDate}'/>"
                                                    data-headcount="<c:out value='${reservation.headcount}'/>"
                                                    data-amount="<c:out value='${reservation.amount}' default='0'/>">취소하기</button>
                                        </c:if>
                                        <c:if test="${reservation.status eq 'CONFIRMED'}">
                                            <button class="mypage-outline-danger js-cancel-open" type="button"
                                                    data-reservation-id="<c:out value='${reservation.reservationId}'/>"
                                                    data-place-name="<c:out value='${reservation.placeName}' default='장소 정보 없음'/>"
                                                    data-visit-date="<c:out value='${reservation.visitDate}'/>"
                                                    data-headcount="<c:out value='${reservation.headcount}'/>"
                                                    data-amount="<c:out value='${reservation.amount}' default='0'/>">취소 요청</button>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </div>
</main>

<div class="cancel-modal" id="cancelModal" hidden>
    <div class="cancel-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-modal-title">
        <div class="cancel-modal__header">
            <div><h2 id="cancel-modal-title">예약 취소 요청</h2><p id="cancelPlaceName">장소</p></div>
            <button class="cancel-modal__close js-cancel-close" type="button" aria-label="닫기">×</button>
        </div>
        <form action="#" method="post" id="cancelRequestForm"
              data-endpoint-prefix="${pageContext.request.contextPath}/reservations/"
              data-success-url="${pageContext.request.contextPath}/mypage/reservation?status=CANCEL_REQUESTED">
            <input type="hidden" id="cancelReservationId" name="reservationId">
            <div class="cancel-modal__body">
                <div class="cancel-modal__summary">
                    <p><span>예약 일정</span><strong id="cancelVisitDate">-</strong></p>
                    <p><span>인원</span><strong id="cancelHeadcount">-</strong></p>
                    <p><span>결제 금액</span><strong class="cancel-modal__price" id="cancelAmount">-</strong></p>
                    <p><span>예상 환불액</span><strong class="cancel-modal__price" id="cancelRefundAmount">-</strong></p>
                </div>
                <div class="cancel-modal__field">
                    <label for="cancelReason">취소 사유</label>
                    <select id="cancelReason" name="reasonOption" required>
                        <option value="" selected disabled>취소 사유를 선택해 주세요</option>
                        <option value="일정이 변경되었습니다">일정이 변경되었습니다</option>
                        <option value="개인 사정으로 여행이 어렵게 되었습니다">개인 사정으로 여행이 어렵게 되었습니다</option>
                        <option value="다른 장소로 변경하고 싶습니다">다른 장소로 변경하고 싶습니다</option>
                        <option value="동행자가 참여하지 못하게 되었습니다">동행자가 참여하지 못하게 되었습니다</option>
                        <option value="CUSTOM">직접 입력</option>
                    </select>
                </div>
                <div class="cancel-modal__field" id="cancelCustomReason" hidden>
                    <label for="cancelReasonText">취소 사유 직접 입력</label>
                    <textarea id="cancelReasonText" name="customReason" rows="3" maxlength="500" placeholder="취소 사유를 직접 입력해 주세요"></textarea>
                </div>
                <p class="profile-edit__message profile-edit__message--error" id="cancelRequestError" hidden></p>
                <p class="cancel-modal__notice">취소 요청 후 업체 측 검토가 완료되면 결과를 안내드립니다. 환불은 정책에 따라 처리됩니다.</p>
            </div>
            <div class="cancel-modal__actions">
                <button class="cancel-modal__back js-cancel-close" type="button">돌아가기</button>
                <button class="cancel-modal__submit" type="submit">취소 요청 제출</button>
            </div>
        </form>
    </div>
</div>

<div class="cancel-modal" id="cancelNowModal" hidden>
    <div class="cancel-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-now-modal-title">
        <div class="cancel-modal__header">
            <div><h2 id="cancel-now-modal-title">예약 취소하기</h2><p id="cancelNowPlaceName">장소</p></div>
            <button class="cancel-modal__close js-cancel-now-close" type="button" aria-label="닫기">×</button>
        </div>
        <form action="#" method="post" id="cancelNowForm"
              data-endpoint-prefix="${pageContext.request.contextPath}/reservations/"
              data-success-url="${pageContext.request.contextPath}/mypage/reservation?status=CANCELED">
            <input type="hidden" id="cancelNowReservationId" name="reservationId">
            <div class="cancel-modal__body">
                <div class="cancel-modal__summary">
                    <p><span>예약 일정</span><strong id="cancelNowVisitDate">-</strong></p>
                    <p><span>인원</span><strong id="cancelNowHeadcount">-</strong></p>
                    <p><span>결제 금액</span><strong class="cancel-modal__price" id="cancelNowAmount">-</strong></p>
                </div>
                <p class="profile-edit__message profile-edit__message--error" id="cancelNowError" hidden></p>
                <p class="cancel-modal__notice">아직 업체가 확정하지 않은 예약이라, 취소 즉시 결제하신 금액을 전액 환불합니다.</p>
            </div>
            <div class="cancel-modal__actions">
                <button class="cancel-modal__back js-cancel-now-close" type="button">돌아가기</button>
                <button class="cancel-modal__submit" type="submit">취소하기</button>
            </div>
        </form>
    </div>
</div>

<div class="cancel-modal" id="detailModal" hidden>
    <div class="cancel-modal__dialog detail-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="detail-modal-title">
        <div class="cancel-modal__header detail-modal__header">
            <div><h2 id="detail-modal-title">예약 상세</h2><p id="detailPlaceName">장소</p></div>
            <button class="cancel-modal__close js-detail-close" type="button" aria-label="닫기">×</button>
        </div>
        <div class="cancel-modal__body detail-modal__body">
            <section class="cancel-modal__summary detail-modal__summary" aria-label="예약 기본 정보">
                <p><span>장소</span><strong id="detailPlace">-</strong></p>
                <p><span>예약자</span><strong id="detailVisitorName">-</strong></p>
                <p><span>연락처</span><strong id="detailPhone">-</strong></p>
                <p><span>방문일</span><strong id="detailVisitDate">-</strong></p>
                <p><span>인원</span><strong id="detailHeadcount">-</strong></p>
                <p><span>금액</span><strong class="cancel-modal__price" id="detailAmount">-</strong></p>
            </section>
            <section class="detail-modal__section detail-modal__section--processing" aria-label="예약 상태와 처리">
                <h3>상태 / 처리</h3>
                <div class="detail-modal__processing">
                    <p><span>상태</span><strong><span class="detail-modal__status" id="detailStatus">-</span></strong></p>
                    <p><span>처리</span><strong id="detailProcess">-</strong></p>
                </div>
            </section>
            <section class="detail-modal__section detail-modal__section--reason" aria-label="사유">
                <h3>사유</h3>
                <div class="detail-modal__reason" id="detailCancelReasonBlock" hidden>
                    <p class="detail-modal__reason-label">내가 남긴 취소 사유</p>
                    <p class="detail-modal__reason-text" id="detailCancelReason">-</p>
                </div>
                <div class="detail-modal__reason" id="detailCancelRejectReasonBlock" hidden>
                    <p class="detail-modal__reason-label">업체가 취소 요청을 거절한 사유</p>
                    <p class="detail-modal__reason-text" id="detailCancelRejectReason">-</p>
                </div>
                <div class="detail-modal__reason" id="detailRejectReasonBlock" hidden>
                    <p class="detail-modal__reason-label">업체가 예약을 거절한 사유</p>
                    <p class="detail-modal__reason-text" id="detailRejectReason">-</p>
                </div>
                <p class="detail-modal__empty" id="detailNoReason">등록된 사유가 없습니다.</p>
            </section>
        </div>
        <div class="cancel-modal__actions detail-modal__actions">
            <button class="cancel-modal__back js-detail-close" type="button">닫기</button>
        </div>
    </div>
</div>
<script>
(() => {
    const modal = document.getElementById('cancelModal');
    const form = document.getElementById('cancelRequestForm');
    const reasonSelect = document.getElementById('cancelReason');
    const customReasonField = document.getElementById('cancelCustomReason');
    const customReasonText = document.getElementById('cancelReasonText');
    const requestError = document.getElementById('cancelRequestError');
    const submitButton = form.querySelector('.cancel-modal__submit');
    const close = () => { modal.hidden = true; };
    const formatAmount = (amount) => Number(amount || 0).toLocaleString('ko-KR') + '원';
    document.querySelectorAll('.js-cancel-open').forEach((button) => button.addEventListener('click', () => {
        document.getElementById('cancelReservationId').value = button.dataset.reservationId;
        document.getElementById('cancelPlaceName').textContent = button.dataset.placeName;
        document.getElementById('cancelVisitDate').textContent = button.dataset.visitDate;
        document.getElementById('cancelHeadcount').textContent = button.dataset.headcount + '명';
        document.getElementById('cancelAmount').textContent = formatAmount(button.dataset.amount);
        reasonSelect.value = '';
        customReasonField.hidden = true;
        customReasonText.required = false;
        customReasonText.value = '';
        requestError.hidden = true;
        requestError.textContent = '';
        modal.hidden = false;

        const cancelRefundAmount = document.getElementById('cancelRefundAmount');
        cancelRefundAmount.textContent = '조회 중...';
        fetch(form.dataset.endpointPrefix + encodeURIComponent(button.dataset.reservationId) + '/refund-preview')
            .then((res) => res.json())
            .then((data) => { cancelRefundAmount.textContent = formatAmount(data.refundAmount); })
            .catch(() => { cancelRefundAmount.textContent = '-'; });
    }));
    document.querySelectorAll('.js-cancel-close').forEach((button) => button.addEventListener('click', close));
    reasonSelect.addEventListener('change', (event) => {
        const isCustom = event.target.value === 'CUSTOM';
        customReasonField.hidden = !isCustom;
        customReasonText.required = isCustom;
        if (isCustom) customReasonText.focus();
    });
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const reservationId = document.getElementById('cancelReservationId').value;
        const reason = reasonSelect.value === 'CUSTOM'
            ? customReasonText.value.trim()
            : reasonSelect.value;

        if (!reservationId || !reason) return;

        requestError.hidden = true;
        submitButton.disabled = true;

        try {
            const response = await fetch(
                form.dataset.endpointPrefix + encodeURIComponent(reservationId) + '/cancel-request',
                {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                    body: new URLSearchParams({reason: reason}).toString()
                }
            );

            if (!response.ok) throw new Error('cancel request failed');
            window.location.assign(form.dataset.successUrl);
        } catch (error) {
            requestError.textContent = '취소 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.';
            requestError.hidden = false;
            submitButton.disabled = false;
        }
    });
    modal.addEventListener('click', (event) => { if (event.target === modal) close(); });
})();

(() => {
    const modal = document.getElementById('cancelNowModal');
    const form = document.getElementById('cancelNowForm');
    const errorBox = document.getElementById('cancelNowError');
    const submitButton = form.querySelector('.cancel-modal__submit');
    const close = () => { modal.hidden = true; };
    const formatAmount = (amount) => Number(amount || 0).toLocaleString('ko-KR') + '원';

    document.querySelectorAll('.js-cancel-now-open').forEach((button) => button.addEventListener('click', () => {
        document.getElementById('cancelNowReservationId').value = button.dataset.reservationId;
        document.getElementById('cancelNowPlaceName').textContent = button.dataset.placeName;
        document.getElementById('cancelNowVisitDate').textContent = button.dataset.visitDate;
        document.getElementById('cancelNowHeadcount').textContent = button.dataset.headcount + '명';
        document.getElementById('cancelNowAmount').textContent = formatAmount(button.dataset.amount);
        errorBox.hidden = true;
        errorBox.textContent = '';
        modal.hidden = false;
    }));

    document.querySelectorAll('.js-cancel-now-close').forEach((button) => button.addEventListener('click', close));

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const reservationId = document.getElementById('cancelNowReservationId').value;
        if (!reservationId) return;

        errorBox.hidden = true;
        submitButton.disabled = true;

        try {
            const response = await fetch(
                form.dataset.endpointPrefix + encodeURIComponent(reservationId) + '/cancel-request',
                { method: 'POST' }
            );
            if (!response.ok) throw new Error('cancel failed');
            window.location.assign(form.dataset.successUrl);
        } catch (error) {
            errorBox.textContent = '취소 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.';
            errorBox.hidden = false;
            submitButton.disabled = false;
        }
    });

    modal.addEventListener('click', (event) => { if (event.target === modal) close(); });
})();

(() => {
    const modal = document.getElementById('detailModal');
    const statusElement = document.getElementById('detailStatus');
    const processElement = document.getElementById('detailProcess');
    const cancelReasonBlock = document.getElementById('detailCancelReasonBlock');
    const cancelRejectReasonBlock = document.getElementById('detailCancelRejectReasonBlock');
    const rejectReasonBlock = document.getElementById('detailRejectReasonBlock');
    const noReason = document.getElementById('detailNoReason');

    const hasText = (value) => Boolean(value && value.trim());
    const formatAmount = (amount) => Number(amount || 0).toLocaleString('ko-KR') + '원';

    const getDetailState = ({status, cancelReason, cancelRejectReason, rejectReason}) => {
        const isCancelled = status === 'CANCELED' || status === 'CANCELLED';
        const hasCancelReason = hasText(cancelReason);
        const hasCancelRejectReason = hasText(cancelRejectReason);
        const hasRejectReason = hasText(rejectReason);

        if (isCancelled && hasCancelReason && !hasCancelRejectReason && !hasRejectReason) {
            return {label: '예약 취소', process: '예약 취소 처리 완료', type: 'cancelled'};
        }
        if (isCancelled && hasRejectReason && !hasCancelReason && !hasCancelRejectReason) {
            return {label: '예약 거절', process: '업체 예약 거절 처리 완료', type: 'rejected'};
        }
        if (status === 'CONFIRMED' && hasCancelReason && hasCancelRejectReason) {
            return {label: '예약 확정', process: '취소 요청 거절 · 예약 확정 유지', type: 'confirmed'};
        }

        const states = {
            PAID: {label: '결제완료', process: '업체 확인 대기', type: 'paid'},
            CANCEL_REQUESTED: {label: '취소요청', process: '업체의 취소 요청 확인 대기', type: 'requested'},
            CONFIRMED: {label: '예약 확정', process: '예약 확정', type: 'confirmed'},
            COMPLETED: {label: '이용 완료', process: '이용 완료', type: 'completed'},
            CANCELED: {label: '예약취소', process: '예약취소', type: 'cancelled'},
            CANCELLED: {label: '예약취소', process: '예약취소', type: 'cancelled'},
            EXPIRED: {label: '예약만료', process: '예약만료', type: 'expired'}
        };

        return states[status] || {label: '예약대기', process: '예약 대기', type: 'pending'};
    };

    document.querySelectorAll('.js-detail-open').forEach((button) => button.addEventListener('click', () => {
        const {dataset} = button;
        const state = getDetailState(dataset);

        document.getElementById('detailPlaceName').textContent = dataset.placeName || '장소 정보 없음';
        document.getElementById('detailPlace').textContent = dataset.placeName || '-';
        document.getElementById('detailVisitorName').textContent = dataset.visitorName || '-';
        document.getElementById('detailPhone').textContent = dataset.phone || '-';
        document.getElementById('detailVisitDate').textContent = dataset.visitDate || '-';
        document.getElementById('detailHeadcount').textContent = (dataset.headcount || '0') + '명';
        document.getElementById('detailAmount').textContent = formatAmount(dataset.amount);
        statusElement.textContent = state.label;
        statusElement.className = 'detail-modal__status detail-modal__status--' + state.type;
        processElement.textContent = state.process;

        const reasonBlocks = [
            {block: cancelReasonBlock, text: document.getElementById('detailCancelReason'), value: dataset.cancelReason},
            {block: cancelRejectReasonBlock, text: document.getElementById('detailCancelRejectReason'), value: dataset.cancelRejectReason},
            {block: rejectReasonBlock, text: document.getElementById('detailRejectReason'), value: dataset.rejectReason}
        ];
        let hasReason = false;
        reasonBlocks.forEach(({block, text, value}) => {
            const visible = hasText(value);
            block.hidden = !visible;
            if (visible) {
                text.textContent = value;
                hasReason = true;
            }
        });
        noReason.hidden = hasReason;
        modal.hidden = false;
    }));

    document.querySelectorAll('.js-detail-close').forEach((button) => button.addEventListener('click', () => { modal.hidden = true; }));
    modal.addEventListener('click', (event) => { if (event.target === modal) modal.hidden = true; });
})();
</script>
</body>
</html>
