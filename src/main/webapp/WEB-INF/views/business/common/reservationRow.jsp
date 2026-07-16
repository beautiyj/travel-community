<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%--
예약 1건 행 (대시보드 오늘 예약 미리보기 / 예약 관리 목록 공용)

사용법 (jsp:param으로 전달하는 값):
- name, phone, headcount, status : 예약 기본 정보 (필수)
- amount     : 결제 금액
- visitDate  : 방문일자- 예약 관리 목록에서 사용, 대시보드는 미전달)
- mode       : 'actionable'이면 대기중 상태일 때 실제 수락/거절 폼 렌더 (예약 관리 탭).
               그 외(미전달)에는 대시보드용 미리보기 버튼만 표시 (아직 실제 동작 연결 안 됨)
- reservationId, memberId : mode가 'actionable'일 때 필수
--%>
<c:choose>
    <c:when test="${param.status == '확정'}"><c:set var="statusClass" value="confirmed"/></c:when>
    <c:when test="${param.status == '대기중'}"><c:set var="statusClass" value="pending"/></c:when>
    <c:when test="${param.status == '완료'}"><c:set var="statusClass" value="done"/></c:when>
    <c:otherwise><c:set var="statusClass" value="cancelled"/></c:otherwise>
</c:choose>

<div class="business-reservation-row">
    <div>
        <span class="business-reservation-row__name">${param.name}</span>
        <span class="business-reservation-row__meta">${param.phone}<c:if test="${not empty param.visitDate}"> · ${param.visitDate}</c:if> · ${param.headcount}명</span>
    </div>
    <div class="business-reservation-row__right">
        <c:if test="${not empty param.amount}">
            <span class="business-reservation-row__price"><fmt:formatNumber value="${param.amount}" type="number" groupingUsed="true"/>원</span>
        </c:if>
        <span class="business-badge-status business-badge-status--${statusClass}">${param.status}</span>
        <c:if test="${param.status == '대기중'}">
            <c:choose>
                <c:when test="${param.mode == 'actionable'}">
                    <form method="post" action="/business/reservations/${param.reservationId}/accept" class="business-inline-form">
                        <input type="hidden" name="memberId" value="${param.memberId}" />
                        <button type="submit" class="business-btn business-btn--primary business-btn--sm">수락</button>
                    </form>
                    <form method="post" action="/business/reservations/${param.reservationId}/reject" class="business-inline-form">
                        <input type="hidden" name="memberId" value="${param.memberId}" />
                        <button type="submit" class="business-btn business-btn--danger business-btn--sm">거절</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <!-- todo: 실제 수락 액션 연결은 예약 관리 탭 구현 시 진행 -->
                    <button type="button" class="business-btn business-btn--primary business-btn--sm">수락</button>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>
