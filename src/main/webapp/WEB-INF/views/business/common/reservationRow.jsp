<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%--
예약 1건 행 (대시보드 오늘 예약 미리보기 / 예약 관리 목록 공용)

사용법 (jsp:param으로 전달하는 값):
- name, phone, headcount : 예약 기본 정보 (필수)
- status      : ReservationStatus enum 이름 (PENDING/PAID/COMPLETED/CANCEL_REQUESTED/CANCELED). 분기/뱃지 색상 판단용 (필수)
- statusLabel : 화면에 보여줄 한글 문구 (${r.status.label}). 미전달 시 status 그대로 표시
- amount     : 결제 금액
- visitDate  : 방문일자 (예약 관리 목록에서 사용, 대시보드는 미전달)
- mode       : 'actionable'이면 CANCEL_REQUESTED 상태일 때 실제 취소승인/거절 폼 렌더 (예약 관리 탭).
               그 외(미전달)에는 대시보드용 미리보기 버튼만 표시 (아직 실제 동작 연결 안 됨)
- reservationId, memberId : mode가 'actionable'일 때 필수
- layout     : 'table'이면 예약 관리 목록의 표 형태(예약자/연락처/방문일/인원/금액/상태 칼럼)로 렌더링.
               그 외(미전달)에는 대시보드 미리보기용 한 줄 요약 형태로 렌더링
--%>
<c:choose>
    <c:when test="${param.status == 'PAID'}"><c:set var="statusClass" value="confirmed"/></c:when>
    <c:when test="${param.status == 'PENDING'}"><c:set var="statusClass" value="pending"/></c:when>
    <c:when test="${param.status == 'COMPLETED'}"><c:set var="statusClass" value="done"/></c:when>
    <c:otherwise><c:set var="statusClass" value="cancelled"/></c:otherwise>
</c:choose>
<c:set var="statusText" value="${not empty param.statusLabel ? param.statusLabel : param.status}"/>

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
                <c:if test="${param.status == 'CANCEL_REQUESTED'}">
                    <c:choose>
                        <c:when test="${param.mode == 'actionable'}">
                            <form method="post" action="/business/reservations/${param.reservationId}/cancel-approve" class="business-inline-form">
                                <input type="hidden" name="memberId" value="${param.memberId}" />
                                <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                                    <jsp:param name="text" value="취소 승인" />
                                    <jsp:param name="theme" value="primary" />
                                </jsp:include>
                            </form>
                            <form method="post" action="/business/reservations/${param.reservationId}/cancel-reject" class="business-inline-form">
                                <input type="hidden" name="memberId" value="${param.memberId}" />
                                <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                                    <jsp:param name="text" value="취소 거절" />
                                    <jsp:param name="theme" value="danger" />
                                </jsp:include>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <!-- 실제 취소 승인/거절 액션 연결은 예약 관리 탭 구현 시 진행 -->
                            <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                                <jsp:param name="text" value="취소 승인" />
                                <jsp:param name="theme" value="primary" />
                            </jsp:include>
                        </c:otherwise>
                    </c:choose>
                </c:if>
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
                <c:if test="${param.status == 'CANCEL_REQUESTED'}">
                    <c:choose>
                        <c:when test="${param.mode == 'actionable'}">
                            <form method="post" action="/business/reservations/${param.reservationId}/cancel-approve" class="business-inline-form">
                                <input type="hidden" name="memberId" value="${param.memberId}" />
                                <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                                    <jsp:param name="text" value="취소 승인" />
                                    <jsp:param name="theme" value="primary" />
                                </jsp:include>
                            </form>
                            <form method="post" action="/business/reservations/${param.reservationId}/cancel-reject" class="business-inline-form">
                                <input type="hidden" name="memberId" value="${param.memberId}" />
                                <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                                    <jsp:param name="text" value="취소 거절" />
                                    <jsp:param name="theme" value="danger" />
                                </jsp:include>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <!-- 실제 취소 승인/거절 액션 연결은 예약 관리 탭 구현 시 진행 -->
                            <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                                <jsp:param name="text" value="취소 승인" />
                                <jsp:param name="theme" value="primary" />
                            </jsp:include>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </div>
        </div>
    </c:otherwise>
</c:choose>
