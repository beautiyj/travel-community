<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/components/pageHead.jsp"><jsp:param name="title" value="예약목록" /></jsp:include></head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/components/sidebar.jsp"><jsp:param name="active" value="reservation" /></jsp:include>
        <section class="mypage-content" aria-labelledby="reservation-title">
            <div class="mypage-content__header"><h2 id="reservation-title">예약목록 <em>(${empty reservationList ? 0 : reservationList.size()})</em></h2></div>
            <c:choose>
                <c:when test="${empty reservationList}">
                    <div class="mypage-empty-state"><strong>아직 예약 내역이 없습니다</strong><a href="${pageContext.request.contextPath}/">여행지 탐색하기</a></div>
                </c:when>
                <c:otherwise>
                    <div class="reservation-list">
                        <c:forEach var="reservation" items="${reservationList}">
                            <article class="reservation-item">
                                <div class="reservation-item__body">
                                    <h3>장소 #<c:out value="${reservation.placeId}" /></h3>
                                    <p>방문자 <c:out value="${reservation.visitorName}" default="-" /></p>
                                    <ul><li>방문일 <c:out value="${reservation.visitDate}" default="-" /></li><li><c:out value="${reservation.headcount}" default="0" />명</li><li><c:out value="${reservation.phone}" default="-" /></li></ul>
                                    <c:choose>
                                        <c:when test="${reservation.status eq 'PAID'}"><span class="mypage-status mypage-status--paid">결제완료</span></c:when>
                                        <c:when test="${reservation.status eq 'PENDING'}"><span class="mypage-status mypage-status--pending">예약대기</span></c:when>
                                        <c:when test="${reservation.status eq 'CANCELLED'}"><span class="mypage-status mypage-status--cancelled">취소됨</span></c:when>
                                        <c:otherwise><span class="mypage-status"><c:out value="${reservation.status}" default="상태 확인 중" /></span></c:otherwise>
                                    </c:choose>
                                </div>
                                <c:if test="${reservation.status eq 'PAID' or reservation.status eq 'PENDING'}">
                                    <button class="mypage-outline-danger js-cancel-open" type="button"
                                            data-reservation-id="<c:out value='${reservation.reservationId}'/>"
                                            data-place-id="<c:out value='${reservation.placeId}'/>"
                                            data-visit-date="<c:out value='${reservation.visitDate}'/>"
                                            data-headcount="<c:out value='${reservation.headcount}'/>">취소 요청</button>
                                </c:if>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
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
                <p><span>결제 금액</span><strong class="cancel-modal__price">-</strong></p>
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
    document.querySelectorAll('.js-cancel-open').forEach((button) => button.addEventListener('click', () => {
        document.getElementById('cancelReservationId').value = button.dataset.reservationId;
        document.getElementById('cancelPlaceName').textContent = `장소 #${button.dataset.placeId}`;
        document.getElementById('cancelVisitDate').textContent = button.dataset.visitDate;
        document.getElementById('cancelHeadcount').textContent = `${button.dataset.headcount}명`;
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
