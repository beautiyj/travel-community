<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%--
취소 요청 확인/처리 모달 (business 도메인 전용).
CANCEL_REQUESTED 행마다 하나씩 렌더링. 고객이 남긴 취소 사유를 보여주고,
그 자리에서 바로 취소승인/취소거절까지 처리한다 (사유 확인 -> 판단 -> 처리를 한 모달에서).

거절을 고르면 사유 입력 패널(select+기타직접입력, rejectReasonModal.jsp와 동일한 UX)로 전환된다.
이 사유는 cancel-reject로 전송되어 RESERVATION.cancel_reject_reason에 저장된다 (2026-08-06, 예약 도메인 파일 수정
 PAID 예약을 직접 거절할 때 쓰는 reject_reason과는 별개 컬럼 — 두 액션은 시점·의미가 달라 같은 컬럼을 쓰면 값이 뒤섞인다.

필수 파라미터:
- modalId          : 화면 내 고유 식별자
- reservationId    : 대상 예약 ID
- cancelReason      : 고객이 입력한 취소 사유 (RESERVATION.cancel_reason)
- cancelRequestedAt : 취소 요청 시각 (RESERVATION.cancel_requested_at)
--%>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div id="${param.modalId}" class="modal-overlay" data-modal
     role="dialog" aria-modal="true" aria-labelledby="${param.modalId}-title">
    <div class="modal business-cancel-request-modal">
        <button type="button" class="business-modal-close-x" data-modal-close aria-label="닫기">&times;</button>
        <h2 class="modal-title" id="${param.modalId}-title">취소 요청</h2>
        <c:if test="${not empty param.cancelRequestedAt}">
            <fmt:parseDate value="${param.cancelRequestedAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="parsedAt" type="both" />
            <p class="modal-message"><fmt:formatDate value="${parsedAt}" pattern="yyyy.MM.dd HH:mm" /> 요청</p>
        </c:if>
        <p class="business-modal-cancel-reason"><c:out
                value="${not empty param.cancelReason ? param.cancelReason : '고객이 별도 사유를 남기지 않았습니다.'}" /></p>

        <%-- 1단계: 승인 또는 거절 선택 --%>
        <div class="business-cancel-actions" data-cancel-actions>
            <form action="${cp}/business/reservations/${param.reservationId}/cancel-approve" method="post" class="business-inline-form">
                <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                    <jsp:param name="text" value="취소 승인" />
                    <jsp:param name="theme" value="primary" />
                </jsp:include>
            </form>
            <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
                <jsp:param name="text" value="취소 거절" />
                <jsp:param name="theme" value="danger" />
                <jsp:param name="onclick" value="toggleCancelRejectPanel(this, true)" />
            </jsp:include>
        </div>

        <%-- 2단계: 거절 선택 시 사유 입력 (평소엔 숨김) --%>
        <form action="${cp}/business/reservations/${param.reservationId}/cancel-reject" method="post"
              class="business-cancel-reject-panel" data-reject-form data-cancel-reject-panel style="display:none;">
            <label class="business-modal-label" for="${param.modalId}-select">거절 사유</label>
            <select id="${param.modalId}-select" class="business-modal-select" data-reject-select>
                <option value="">사유를 선택하세요</option>
                <option value="이미 이용 완료된 예약">이미 이용 완료된 예약</option>
                <option value="환불 정책상 취소 불가">환불 정책상 취소 불가</option>
                <option value="취소 가능 기한 경과">취소 가능 기한 경과</option>
                <option value="기타">기타</option>
            </select>
            <%-- "기타" 선택 시에만 노출되는 직접입력. reject_reason 컬럼 길이(100자)에 맞춰 maxlength 제한 --%>
            <textarea class="business-modal-textarea" data-reject-etc maxlength="100"
                      placeholder="사유를 직접 입력해 주세요" style="display:none;"></textarea>
            <input type="hidden" name="reason" data-reject-reason>

            <div class="modal-buttons">
                <button type="button" class="btn-main" style="background: var(--card); box-shadow: none;"
                        onclick="toggleCancelRejectPanel(this, false)">
                    <span class="btn-main-text" style="color: var(--foreground);">뒤로</span>
                </button>
                <button type="submit" class="btn-main" style="background: var(--destructive); box-shadow: none;">
                    <span class="btn-main-text">거절하기</span>
                </button>
            </div>
        </form>
    </div>
</div>
