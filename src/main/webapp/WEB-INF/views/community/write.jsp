<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>새 글쓰기</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/confirmModal.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/dropdownSelector.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/placeSearchModal.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/community.css">
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />
<c:set var="loginMember" value="${sessionScope.loginMember}" />

<div class="write-container">

  <a href="${cp}/community/list" class="back-link">&lt; 목록으로</a>

  <!-- 작성자 아바타 + 닉네임 (네이버 블로그 스타일 상단 바) -->
  <div class="blog-author-bar">
    <div class="blog-avatar">${fn:substring(loginMember.nickname, 0, 1)}</div>
    <span class="blog-author-name">${loginMember.nickname}</span>
  </div>

  <form action="${cp}/community/write" method="post" enctype="multipart/form-data">

    <!-- 카테고리: dropdownSelector 와 동일한 마크업/클래스(drop-select-*, dropdown-*)를 그대로 써서
         디자인/열고닫기 동작은 dropdownSelector.js 가 처리하고, categorySelect.js 는 숨은 라디오만 동기화함.
         목록은 컨트롤러가 내려준 categoryList(PostCategory enum) 로 렌더링 - JSP 하드코딩 없음 -->
    <div class="field">
      <label class="field-label">카테고리</label>

      <div class="drop-select-container dropdown category-dropdown" id="categoryDropdown">
        <button type="button" id="categoryDropdownTrigger" class="drop-select-trigger is-selected" aria-expanded="false">
          <div class="drop-select-left-box">
            <span class="drop-select-text" id="categoryDropdownLabel">${categoryList[0].displayLabel}</span>
          </div>
          <svg class="drop-select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>

        <ul class="dropdown-menu drop-select-menu" aria-labelledby="categoryDropdownTrigger">
          <c:forEach var="cat" items="${categoryList}" varStatus="st">
            <li>
              <button type="button" class="dropdown-item drop-menu-item ${st.first ? 'is-active' : ''}"
                      data-value="${cat.value}" data-label="${cat.displayLabel}">
                ${cat.displayLabel}
              </button>
            </li>
          </c:forEach>
        </ul>
      </div>

      <!-- 실제 서버로 전송되는 값. 화면에서만 숨김 - placeTag.js 가 이 라디오들의 change 이벤트로 동작하므로
           name/value 는 그대로 유지 (categorySelect.js 가 드롭다운 클릭 시 이 라디오를 체크 + change 발생시킴) -->
      <div class="category-radio-group" aria-hidden="true">
        <c:forEach var="cat" items="${categoryList}" varStatus="st">
          <input type="radio" name="category" value="${cat.value}" ${st.first ? 'checked' : ''}>
        </c:forEach>
      </div>
    </div>

    <!-- 제목: DB 컬럼(post.title)이 VARCHAR(50)이라 51자 이상이면 등록 시 SQL 에러가 나서
         제출 시 titleValidation.js가 길이를 먼저 검사함 (아래 titleLengthModal 참고) -->
    <div class="field">
      <input type="text" id="title" name="title" class="title-input-plain"
             placeholder="제목을 입력하세요" required>
    </div>

    <!-- 장소 태그 -->
    <input type="hidden" id="placeId" name="placeId" value="">

    <!-- 본문: contenteditable 미니 에디터. 서식(굵게/기울임/인용구/구분선) 버튼은 없고
         사진/콜라주/슬라이더로 커서 위치에 이미지를 삽입하는 것만 지원 (contentEditor.js) -->
    <div class="field">
      <div class="editor-toolbar">
        <button type="button" id="toolPhotoBtn" class="editor-tool-btn">🖼 사진</button>
        <button type="button" id="toolCollageBtn" class="editor-tool-btn">▦ 콜라주</button>
        <button type="button" id="toolSliderBtn" class="editor-tool-btn">⇄ 슬라이더</button>
        <span class="editor-tool-hint">이미지는 커서 아래에 삽입됩니다</span>
      </div>

      <div id="contentEditor" class="content-editor" contenteditable="true"
           data-placeholder="여행 경험을 자세히 공유해주세요.."></div>

      <!-- 실제 제출용 필드 (contentEditor.js 가 submit 직전에 채움) -->
      <!-- required 안 씀: display:none 이라 검증 실패 시 브라우저가 포커스를 못 줘서 그냥 조용히 제출이 막혀버림 -->
      <textarea id="content" name="content" style="display:none"></textarea>

      <!-- "사진" 버튼용 임시 파일 선택 input (제출되지 않음, 매번 비움) -->
      <input type="file" id="photoInput" accept="image/*" multiple hidden>

      <!-- 실제 제출용 이미지 input (contentEditor.js 가 DataTransfer 로 채움) -->
      <input type="file" id="images" name="images" accept="image/*" multiple hidden>
    </div>

    <!-- 장소 태그: "방문자인증후기"/"일반후기" 카테고리일 때만 노출, 취소/게시하기 버튼 바로 위 -->
    <div class="field" id="place-tag-field" style="display:none;">
      <label class="field-label">장소 태그</label>

      <div id="place-tag-selected" class="place-tag-selected" style="display:none;">
        <span id="place-tag-selected-name"></span>
        <button type="button" id="place-tag-remove" class="place-tag-remove">✕</button>
      </div>

      <button type="button" id="place-tag-open-btn" class="place-tag-open-btn">장소 검색해서 태그하기</button>
    </div>

    <!-- 버튼: 취소는 buttonComponent 를 내비게이션 용도로, 게시하기는 순수 제출 버튼으로 -->
    <div class="form-actions">
      <div class="btn-nav-wrap" data-btn-nav="${cp}/community/list">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="취소" />
          <jsp:param name="color" value="var(--card)" />
        </jsp:include>
      </div>

      <div class="btn-submit-wrap">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text" value="게시하기" />
        </jsp:include>
      </div>
    </div>

  </form>
</div>

<jsp:include page="placeSearchModal.jsp">
  <jsp:param name="modalId" value="placeSearchModal" />
</jsp:include>

<!-- 콜라주/슬라이더 빌더 모달 (write.jsp/edit.jsp 공용, contentEditor.js 가 제어) -->
<div id="imgBlockModal" class="modal-overlay" data-modal role="dialog" aria-modal="true" aria-labelledby="imgBlockModalTitle">
  <div class="modal img-block-modal">
    <h2 class="modal-title" id="imgBlockModalTitle">콜라주 만들기</h2>
    <p class="modal-message">사진을 2장 이상 선택하세요.</p>

    <label for="imgBlockFileInput" class="image-upload-box">클릭해서 사진을 선택하세요 (여러 장 가능)</label>
    <input type="file" id="imgBlockFileInput" accept="image/*" multiple hidden>

    <div id="imgBlockPreview" class="img-block-preview"></div>
    <div id="imgBlockCanvas" class="collage-builder-canvas" style="display:none;"></div>

    <div class="modal-buttons">
      <button type="button" class="btn-main" style="background: var(--card); border:1px solid var(--border); box-shadow:none;" data-modal-close>
        <span class="btn-main-text" style="color: var(--foreground);">취소</span>
      </button>
      <button type="button" id="imgBlockConfirmBtn" class="btn-main" disabled>
        <span class="btn-main-text">완료</span>
      </button>
    </div>
  </div>
</div>

<!-- 제목 글자수 초과(50자 초과) 안내 모달 - titleValidation.js가 제출 시 검사해서 띄움
     확인 버튼은 data-modal-close로 닫히기만 함 (폼 제출/새로고침 없음 → 작성 중이던 내용 유지) -->
<div id="titleLengthModal" class="modal-overlay" data-modal
     role="dialog" aria-modal="true" aria-labelledby="titleLengthModal-title">
  <div class="modal">
    <h2 class="modal-title" id="titleLengthModal-title">알림</h2>
    <p class="modal-message">제목은 50자 이내로 입력해주세요.</p>
    <div class="modal-buttons">
      <div class="modal-btn modal-btn-cancel" data-modal-close>
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="확인" />
          <jsp:param name="color" value="var(--card)" />
          <jsp:param name="size"  value="var(--text-sm)" />
        </jsp:include>
      </div>
    </div>
  </div>
</div>

<script>window.CP = "${cp}";</script>
<script src="${cp}/js/common.js"></script>
<script src="${cp}/js/dropdownSelector.js"></script>
<script src="${cp}/js/community/categorySelect.js"></script>
<script src="${cp}/js/community/contentEditor.js"></script>
<script src="${cp}/js/common/highlightKeyword.js"></script>
<script src="${cp}/js/community/placeTag.js"></script>
<script src="${cp}/js/community/titleValidation.js"></script>
</body>
</html>
