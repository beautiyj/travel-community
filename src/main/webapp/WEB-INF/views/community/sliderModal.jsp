<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%-- 슬라이더 빌더 모달 (write.jsp/edit.jsp 공용, contentEditor.js 가 제어)
     - confirmModal.css 의 .modal-overlay/.modal 재사용, common.js 의 openModal/closeModal 로 열고 닫음 --%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/sliderModal.css">

<div id="sliderModal" class="modal-overlay" data-modal role="dialog" aria-modal="true" aria-labelledby="sliderModalTitle">
  <div class="modal slider-modal">
    <h2 class="modal-title" id="sliderModalTitle">슬라이더 만들기</h2>
    <p class="modal-message">사진을 2장 이상 선택하세요.</p>

    <label for="sliderFileInput" class="image-upload-box">클릭해서 사진을 선택하세요 (여러 장 가능)</label>
    <input type="file" id="sliderFileInput" accept="image/*" multiple hidden>

    <div id="sliderPreview" class="img-block-preview"></div>

    <div class="modal-buttons">
      <button type="button" class="btn-main" style="background: var(--card); border:1px solid var(--border); box-shadow:none;" data-modal-close>
        <span class="btn-main-text" style="color: var(--foreground);">취소</span>
      </button>
      <button type="button" id="sliderConfirmBtn" class="btn-main" disabled>
        <span class="btn-main-text">완료</span>
      </button>
    </div>
  </div>
</div>
