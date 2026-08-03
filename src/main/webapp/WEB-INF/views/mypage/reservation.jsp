<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="예약 관리" /></jsp:include></head>
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
        <jsp:include page="/WEB-INF/views/mypage/components/sidebar.jsp"><jsp:param name="active" value="reservation" /></jsp:include>
        <section class="mypage-content mypage-reservation-content" aria-labelledby="reservation-title">
            <h2 id="reservation-title" class="sr-only">내 예약 관리</h2>

            <div class="mypage-reservation-filter-row">
                <div class="mypage-reservation-tabs" aria-label="예약 상태 필터">
                    <c:forEach var="tab" items="${reservationTabs}">
                        <c:url value="/mypage/reservation" var="filterUrl">
                            <c:if test="${not empty tab.value}"><c:param name="status" value="${tab.value}" /></c:if>
                        </c:url>
                        <a href="${filterUrl}"
                           class="mypage-reservation-tab${(empty tab.value and empty statusFilter) or tab.value eq statusFilter ? ' is-active' : ''}">
                            <c:out value="${tab.label}" />
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
                                    <div class="mypage-reservation-actions" role="cell">
                                        <c:choose>
                                            <c:when test="${reservation.status eq 'PAID'}"><span class="mypage-status mypage-status--paid">결제완료</span></c:when>
                                            <c:when test="${reservation.status eq 'CANCEL_REQUESTED'}"><span class="mypage-status mypage-status--requested">취소요청</span></c:when>
                                            <c:when test="${reservation.status eq 'CONFIRMED'}"><span class="mypage-status mypage-status--confirmed">예약확정</span></c:when>
                                            <c:when test="${reservation.status eq 'COMPLETED'}"><span class="mypage-status mypage-status--completed">예약완료</span></c:when>
                                            <c:when test="${reservation.status eq 'CANCELED' or reservation.status eq 'CANCELLED'}"><span class="mypage-status mypage-status--cancelled">예약취소</span></c:when>
                                            <c:when test="${reservation.status eq 'EXPIRED'}"><span class="mypage-status mypage-status--expired">예약만료</span></c:when>
                                            <c:otherwise><span class="mypage-status mypage-status--pending">예약대기</span></c:otherwise>
                                        </c:choose>
                                        <c:if test="${reservation.status eq 'PAID' or reservation.status eq 'PENDING'}">
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
        <div class="cancel-modal__body">
            <div class="cancel-modal__summary">
                <p><span>예약 일정</span><strong id="cancelVisitDate">-</strong></p>
                <p><span>인원</span><strong id="cancelHeadcount">-</strong></p>
                <p><span>결제 금액</span><strong class="cancel-modal__price" id="cancelAmount">-</strong></p>
            </div>
            <div class="cancel-modal__field"><label for="cancelReason">취소 사유 선택</label><select id="cancelReason"><option>일정이 변경되었습니다</option><option>개인 사정으로 여행이 어렵게 되었습니다</option><option>다른 장소로 변경하고 싶습니다</option><option>동행자가 참여하지 못하게 되었습니다</option><option>직접 입력</option></select></div>
            <div class="cancel-modal__field" id="cancelCustomReason" hidden><label for="cancelReasonText">직접 입력</label><textarea id="cancelReasonText" rows="3" placeholder="취소 사유를 직접 입력해 주세요..."></textarea></div>
            <p class="cancel-modal__notice">취소 요청 후 업체 측 검토가 완료되면 결과를 안내드립니다. 환불은 정책에 따라 처리됩니다.</p>
        </div>
        <form action="${pageContext.request.contextPath}/mypage/reservation/cancel" method="post" class="cancel-modal__actions">
            <input type="hidden" id="cancelReservationId" name="reservationId">
            <button class="cancel-modal__back js-cancel-close" type="button">돌아가기</button>
            <button class="cancel-modal__submit" type="submit">취소 요청 제출</button>
        </form>
    </div>
</div>
<script>
(() => {
    const modal = document.getElementById('cancelModal');
    const close = () => { modal.hidden = true; };
    const formatAmount = (amount) => `${Number(amount || 0).toLocaleString('ko-KR')}원`;
    document.querySelectorAll('.js-cancel-open').forEach((button) => button.addEventListener('click', () => {
        document.getElementById('cancelReservationId').value = button.dataset.reservationId;
        document.getElementById('cancelPlaceName').textContent = button.dataset.placeName;
        document.getElementById('cancelVisitDate').textContent = button.dataset.visitDate;
        document.getElementById('cancelHeadcount').textContent = `${button.dataset.headcount}명`;
        document.getElementById('cancelAmount').textContent = formatAmount(button.dataset.amount);
        modal.hidden = false;
    }));
    document.querySelectorAll('.js-cancel-close').forEach((button) => button.addEventListener('click', close));
    document.getElementById('cancelReason').addEventListener('change', (event) => {
        document.getElementById('cancelCustomReason').hidden = event.target.value !== '직접 입력';
    });
    modal.addEventListener('click', (event) => { if (event.target === modal) close(); });
})();
</script>
</body>
</html>
