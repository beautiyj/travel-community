<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%-- 제목 글자수 초과(50자 초과) 안내 모달 (write.jsp/edit.jsp 공용, titleValidation.js가 제출 시 검사해서 띄움)
     확인 버튼은 data-modal-close로 닫히기만 함 (폼 제출/새로고침 없음 → 작성/수정 중이던 내용 유지)
     확인 버튼은 smallButton.jsp 재사용 (detail.jsp의 수정/삭제/등록 버튼과 동일한 컴포넌트) --%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/smallButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/titleLengthModal.css">

<div id="titleLengthModal" class="modal-overlay" data-modal
     role="dialog" aria-modal="true" aria-labelledby="titleLengthModal-title">
  <div class="modal">
    <h2 class="modal-title" id="titleLengthModal-title">알림</h2>
    <p class="modal-message">제목은 50자 이내로 입력해주세요.</p>
    <div class="modal-buttons">
      <div class="modal-btn" data-modal-close>
        <jsp:include page="../common/smallButton.jsp">
          <jsp:param name="text" value="확인" />
        </jsp:include>
      </div>
    </div>
  </div>
</div>
