<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 제출 전 검증 안내 모달 (write.jsp/edit.jsp 공용) - 확인 버튼은 data-modal-close로 닫히기만 함
     (폼 제출/새로고침 없음 → 작성/수정 중이던 내용 유지). 확인 버튼은 smallButton.jsp 재사용
     (detail.jsp의 수정/삭제/등록 버튼과 동일한 컴포넌트)
     modalId/message 파라미터로 다른 검증(예: 장소 태그 필수)에도 재사용 가능 - 기본값은
     원래 용도였던 제목 글자수 초과(50자 초과) 안내라서, 파라미터 없이 쓰던 기존 호출부는 그대로 동작함 --%>
<c:set var="mId"      value="${empty param.modalId  ? 'titleLengthModal' : param.modalId}" />
<c:set var="mMessage" value="${empty param.message  ? '제목은 50자 이내로 입력해주세요.' : param.message}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/smallButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/titleLengthModal.css">

<div id="${mId}" class="modal-overlay" data-modal
     role="dialog" aria-modal="true" aria-labelledby="${mId}-title">
  <div class="modal">
    <h2 class="modal-title" id="${mId}-title">알림</h2>
    <p class="modal-message">${mMessage}</p>
    <div class="modal-buttons">
      <div class="modal-btn" data-modal-close>
        <jsp:include page="../common/smallButton.jsp">
          <jsp:param name="text" value="확인" />
        </jsp:include>
      </div>
    </div>
  </div>
</div>
