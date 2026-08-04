<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
예약 행의 상태별 처리 버튼 (reservationRow.jsp의 표/요약 두 레이아웃이 공용으로 include).
버튼 마크업이 레이아웃마다 똑같아서 여기로 분리했다.

파라미터:
- status        : ReservationStatus enum 이름
- actionable    : 'true'일 때만 버튼을 렌더 (동작하지 않는 버튼을 노출하지 않기 위함)
- reservationId : 처리 대상 예약 ID
- rejectModalId : 거절 사유 모달의 화면 내 고유 id (호출부가 레이아웃별로 다르게 만들어 넘긴다)
- cancelReason, cancelRequestedAt, cancelRequestModalId : 취소요청 행에서 "상세보기" 버튼으로 여는
  취소 요청 모달(cancelRequestModal.jsp - 사유 확인 + 승인/거절 처리)에 쓰인다
--%>
<c:if test="${param.actionable == 'true'}">

    <c:if test="${param.status == 'PAID'}">
        <form method="post" action="/business/reservations/${param.reservationId}/confirm" class="business-inline-form">
            <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                <jsp:param name="text" value="예약확정" />
                <jsp:param name="theme" value="primary" />
            </jsp:include>
        </form>
        <%-- 거절은 사유 입력이 필요해 폼 즉시제출 대신 모달을 띄운다 (rejectReasonModal.jsp) --%>
        <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
            <jsp:param name="text" value="예약거절" />
            <jsp:param name="theme" value="danger" />
            <jsp:param name="onclick" value="openModal('${param.rejectModalId}')" />
        </jsp:include>
        <jsp:include page="/WEB-INF/views/business/common/rejectReasonModal.jsp">
            <jsp:param name="modalId" value="${param.rejectModalId}" />
            <jsp:param name="reservationId" value="${param.reservationId}" />
        </jsp:include>
    </c:if>

    <c:if test="${param.status == 'CANCEL_REQUESTED'}">
        <%-- 승인/거절 모두 사유 확인 후 판단해야 해서, 행 버튼 3개 대신 모달 하나에 사유 확인+처리를 몰아넣는다 (cancelRequestModal.jsp) --%>
        <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
            <jsp:param name="text" value="상세보기" />
            <jsp:param name="theme" value="outline" />
            <jsp:param name="onclick" value="openModal('${param.cancelRequestModalId}'); resetCancelRequestModal('${param.cancelRequestModalId}')" />
        </jsp:include>
        <jsp:include page="/WEB-INF/views/business/common/cancelRequestModal.jsp">
            <jsp:param name="modalId" value="${param.cancelRequestModalId}" />
            <jsp:param name="reservationId" value="${param.reservationId}" />
            <jsp:param name="cancelReason" value="${param.cancelReason}" />
            <jsp:param name="cancelRequestedAt" value="${param.cancelRequestedAt}" />
        </jsp:include>
    </c:if>

</c:if>
