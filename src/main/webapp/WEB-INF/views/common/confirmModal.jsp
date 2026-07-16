<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--
  공통 확인 모달 (재사용 조각 파일) — 삭제 / 예약 취소 / 회원 탈퇴 등 되돌릴 수 없는 동작 전 확인
  위치: /WEB-INF/views/common/confirmModal.jsp

  파라미터
    modalId     : 모달 DOM id. 한 페이지에 여러 개 둘 때 구분 (기본 confirmModal)
    action      : [필수] 컨텍스트 경로를 뺀 폼 전송 경로 (예: /community/delete)
    method      : 폼 method (기본 post)
    title       : 모달 제목 (기본 확인)
    message     : 본문 문구 (기본 이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?)
    hiddenName  : 함께 보낼 hidden 파라미터 이름. 여러 번 넘기면 여러 개 생성
    hiddenValue : hidden 값. hiddenName 과 같은 순서로 짝을 맞출 것
                  첫 번째 hidden 은 openModal(id, value) 로 값을 나중에 주입 가능
    confirmText : 확정 버튼 라벨 (기본 확인)
    cancelText  : 취소 버튼 라벨 (기본 취소)
    tone        : 확정 버튼 색 — danger(기본) | primary

  사용 예 1) 게시글 삭제 — 값이 고정
    <jsp:include page="/WEB-INF/views/common/confirmModal.jsp">
      <jsp:param name="modalId"     value="postDeleteModal" />
      <jsp:param name="action"      value="/community/delete" />
      <jsp:param name="title"       value="게시글 삭제" />
      <jsp:param name="message"     value="삭제 후 복구할 수 없습니다. 정말 삭제하시겠습니까?" />
      <jsp:param name="confirmText" value="삭제" />
      <jsp:param name="hiddenName"  value="postId" />
      <jsp:param name="hiddenValue" value="${post.postId}" />
    </jsp:include>

  사용 예 2) 예약 취소 — 목록에서 행마다 값이 다를 때 (모달 1개 재사용)
    <button type="button" onclick="openModal('reservationCancelModal', ${r.reservationId})">예약 취소</button>

    <jsp:include page="/WEB-INF/views/common/confirmModal.jsp">
      <jsp:param name="modalId"     value="reservationCancelModal" />
      <jsp:param name="action"      value="/mypage/reservation/cancel" />
      <jsp:param name="title"       value="예약 취소" />
      <jsp:param name="message"     value="취소하면 되돌릴 수 없고 환불 규정에 따라 수수료가 발생할 수 있습니다." />
      <jsp:param name="confirmText" value="예약 취소" />
      <jsp:param name="cancelText"  value="닫기" />
      <jsp:param name="hiddenName"  value="reservationId" />
    </jsp:include>

  필요 리소스: common.css → modal.css, modal.js
  ※ cp(contextPath)는 page scope 라 조각 파일에서 다시 선언
--%>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<%-- 파라미터 기본값 처리 --%>
<c:set var="mId"      value="${empty param.modalId     ? 'confirmModal' : param.modalId}" />
<c:set var="mMethod"  value="${empty param.method      ? 'post'         : param.method}" />
<c:set var="mTitle"   value="${empty param.title       ? '확인'         : param.title}" />
<c:set var="mMessage" value="${empty param.message     ? '이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?' : param.message}" />
<c:set var="mOk"      value="${empty param.confirmText ? '확인'         : param.confirmText}" />
<c:set var="mCancel"  value="${empty param.cancelText  ? '취소'         : param.cancelText}" />
<c:set var="mTone"    value="${param.tone eq 'primary' ? 'primary'      : 'danger'}" />

<%-- hidden 이름이 하나도 없으면 JS 주입용으로 id 하나만 둠 --%>
<c:set var="hNames"  value="${empty paramValues.hiddenName ? null : paramValues.hiddenName}" />
<c:set var="hCount"  value="${empty hNames ? 0 : fn:length(hNames)}" />

<c:choose>
  <c:when test="${empty param.action}">
    <%-- action 누락은 조용히 넘어가면 디버깅이 어려우므로 화면에 표시 --%>
    <!-- confirmModal(${mId}): action 파라미터가 필요합니다 -->
  </c:when>
  <c:otherwise>
    <div id="${mId}" class="modal-overlay" data-modal
         role="dialog" aria-modal="true" aria-labelledby="${mId}-title">
      <div class="modal">
        <h2 class="modal-title" id="${mId}-title"><c:out value="${mTitle}" /></h2>
        <p class="modal-message"><c:out value="${mMessage}" /></p>

        <form action="${cp}${param.action}" method="${mMethod}">
          <c:choose>
            <c:when test="${hCount eq 0}">
              <input type="hidden" name="id" value="" data-modal-value>
            </c:when>
            <c:otherwise>
              <c:forEach var="i" begin="0" end="${hCount - 1}">
                <input type="hidden"
                       name="${hNames[i]}"
                       value="${paramValues.hiddenValue[i]}"
                       <c:if test="${i eq 0}">data-modal-value</c:if>>
              </c:forEach>
            </c:otherwise>
          </c:choose>

          <div class="modal-buttons">
            <button type="button" class="btn-modal-cancel" data-modal-close>
              <c:out value="${mCancel}" />
            </button>
            <button type="submit" class="btn-modal-confirm is-${mTone}">
              <c:out value="${mOk}" />
            </button>
          </div>
        </form>
      </div>
    </div>
  </c:otherwise>
</c:choose>
