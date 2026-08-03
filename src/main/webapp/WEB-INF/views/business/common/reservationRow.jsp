<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%--
예약 1건 행 (대시보드 오늘 예약 / 예약 관리 목록 공용)

사용법 (jsp:param으로 전달하는 값):
- name, phone, headcount : 예약 기본 정보 (필수)
- status      : ReservationStatus enum 이름 (PENDING/PAID/CONFIRMED/COMPLETED/CANCEL_REQUESTED/CANCELED). 분기/뱃지 색상 판단용 (필수)
- statusLabel : 화면에 보여줄 한글 문구 (${r.status.label}). 미전달 시 status 그대로 표시
- amount     : 결제 금액
- visitDate  : 방문일자 (예약 관리 목록에서 사용, 대시보드는 미전달)
- mode       : 'actionable'이면 상태별 처리 버튼을 렌더한다.
               PAID -> 예약확정/예약거절, CANCEL_REQUESTED -> 취소 승인/취소 거절.
               미전달 시 버튼 없이 상태 뱃지만 보여준다(동작하지 않는 버튼을 노출하지 않기 위함).
- reservationId : mode가 'actionable'일 때 필수 (memberId는 서버가 세션에서 파생)
- layout     : 'table'이면 예약 관리 목록의 표 형태(예약자/연락처/방문일/인원/금액/상태 칼럼)로 렌더링.
               그 외(미전달)에는 대시보드용 한 줄 요약 형태로 렌더링

※ 처리 버튼은 어느 화면에서 눌러도 처리 후 예약 관리 목록(/business/reservations)으로 이동한다.
   실패 안내 배너가 그 화면에 있어서, 결과를 항상 같은 자리에서 확인할 수 있게 맞춘 것이다.
--%>
<c:choose>
    <c:when test="${param.status == 'CONFIRMED'}"><c:set var="statusClass" value="confirmed"/></c:when>
    <c:when test="${param.status == 'PAID' or param.status == 'PENDING'}"><c:set var="statusClass" value="pending"/></c:when>
    <c:when test="${param.status == 'COMPLETED'}"><c:set var="statusClass" value="done"/></c:when>
    <c:otherwise><c:set var="statusClass" value="cancelled"/></c:otherwise>
</c:choose>
<c:set var="statusText" value="${not empty param.statusLabel ? param.statusLabel : param.status}"/>
<c:set var="actionable" value="${param.mode == 'actionable'}"/>

<%-- 모달 id는 한 화면에 표/요약 두 레이아웃이 함께 놓여도 겹치지 않도록 레이아웃별 접두어를 붙인다 --%>
<c:set var="rejectModalId" value="reject-modal-${param.layout == 'table' ? 'table' : 'row'}-${param.reservationId}"/>

<c:choose>
    <c:when test="${param.layout == 'table'}">
        <div class="business-reservation-table__row">
            <div class="business-reservation-table__cell business-reservation-table__cell--name">${param.name}</div>
            <div class="business-reservation-table__cell">${param.phone}</div>
            <div class="business-reservation-table__cell">${param.visitDate}</div>
            <div class="business-reservation-table__cell">${param.headcount}명</div>
            <div class="business-reservation-table__cell business-reservation-table__cell--amount">
                <c:if test="${not empty param.amount}"><fmt:formatNumber value="${param.amount}" type="number" groupingUsed="true"/>원</c:if>
            </div>
            <div class="business-reservation-table__cell business-reservation-table__cell--action">
                <span class="business-badge-status business-badge-status--${statusClass}">${statusText}</span>
                <jsp:include page="reservationRowActions.jsp">
                    <jsp:param name="status" value="${param.status}" />
                    <jsp:param name="actionable" value="${actionable}" />
                    <jsp:param name="reservationId" value="${param.reservationId}" />
                    <jsp:param name="rejectModalId" value="${rejectModalId}" />
                </jsp:include>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="business-reservation-row">
            <div>
                <span class="business-reservation-row__name">${param.name}</span>
                <span class="business-reservation-row__meta">${param.phone}<c:if test="${not empty param.visitDate}"> · ${param.visitDate}</c:if> · ${param.headcount}명</span>
            </div>
            <div class="business-reservation-row__right">
                <c:if test="${not empty param.amount}">
                    <span class="business-reservation-row__price"><fmt:formatNumber value="${param.amount}" type="number" groupingUsed="true"/>원</span>
                </c:if>
                <span class="business-badge-status business-badge-status--${statusClass}">${statusText}</span>
                <jsp:include page="reservationRowActions.jsp">
                    <jsp:param name="status" value="${param.status}" />
                    <jsp:param name="actionable" value="${actionable}" />
                    <jsp:param name="reservationId" value="${param.reservationId}" />
                    <jsp:param name="rejectModalId" value="${rejectModalId}" />
                </jsp:include>
            </div>
        </div>
    </c:otherwise>
</c:choose>
