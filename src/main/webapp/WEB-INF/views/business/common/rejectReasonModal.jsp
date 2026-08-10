<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
예약 거절 사유 입력 모달 (business 도메인 전용).
예약 목록에서 PAID 행마다 하나씩 렌더링되므로 modalId/action에 reservationId가 박혀 들어간다.

필수 파라미터:
- modalId       : 화면 내 고유 식별자
- reservationId : 거절할 예약 ID
--%>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div id="${param.modalId}" class="modal-overlay" data-modal
     role="dialog" aria-modal="true" aria-labelledby="${param.modalId}-title">
    <div class="modal">
        <h2 class="modal-title" id="${param.modalId}-title">예약 거절</h2>
        <p class="modal-message">거절 사유를 선택해 주세요. 결제 금액은 즉시 환불됩니다.</p>

        <form action="${cp}/business/reservations/${param.reservationId}/reject" method="post" data-reject-form>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <label class="business-modal-label" for="${param.modalId}-select">거절 사유</label>
            <select id="${param.modalId}-select" class="business-modal-select" data-reject-select>
                <option value="">사유를 선택하세요</option>
                <option value="예약 가능 인원 초과">예약 가능 인원 초과</option>
                <option value="휴무/예약 마감일">휴무/예약 마감일</option>
                <option value="연락처 확인 불가">연락처 확인 불가</option>
                <option value="기타">기타</option>
            </select>
            <%-- "기타" 선택 시에만 노출되는 직접입력. reject_reason 컬럼 길이(100자)에 맞춰 maxlength 제한 --%>
            <textarea class="business-modal-textarea" data-reject-etc maxlength="100"
                      placeholder="사유를 직접 입력해 주세요" style="display:none;"></textarea>
            <input type="hidden" name="reason" data-reject-reason>

            <div class="modal-buttons">
                <div class="modal-btn modal-btn-cancel" data-modal-close>
                    <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                        <jsp:param name="text" value="취소" />
                        <jsp:param name="color" value="var(--card)" />
                        <jsp:param name="size" value="var(--text-sm)" />
                    </jsp:include>
                </div>
                <div class="modal-btn modal-btn-confirm">
                    <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                        <jsp:param name="text" value="거절하기" />
                        <jsp:param name="color" value="var(--destructive)" />
                        <jsp:param name="size" value="var(--text-sm)" />
                    </jsp:include>
                </div>
            </div>
        </form>
    </div>
</div>
