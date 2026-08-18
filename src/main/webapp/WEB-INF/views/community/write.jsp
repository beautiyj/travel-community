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
<jsp:include page="/WEB-INF/views/common/header.jsp" />
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
         디자인/열고닫기/숨은 라디오 동기화까지 전부 dropdownSelector.js(data-radio-name="category")가 처리함.
         목록은 컨트롤러가 내려준 categoryList(PostCategory enum) 로 렌더링 - JSP 하드코딩 없음 -->
    <div class="field">
      <label class="field-label">카테고리</label>

      <div class="drop-select-container dropdown category-dropdown" id="categoryDropdown" data-radio-name="category">
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
           name/value 는 그대로 유지 (dropdownSelector.js 가 드롭다운 클릭 시 이 라디오를 체크 + change 발생시킴) -->
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

    <!-- 장소 태그: "방문자인증후기"/"일반후기" 카테고리일 때만 노출, 취소/게시하기 버튼 바로 위
         방문자인증후기: 단일 선택(#place-tag-selected, 필수) / 일반후기: 다중 선택(#place-tag-selected-list, 선택사항, 최대 5개) -->
    <div class="field" id="place-tag-field" style="display:none;">
      <label class="field-label">장소 태그</label>

      <div id="place-tag-selected" class="place-tag-selected" style="display:none;">
        <span id="place-tag-selected-name"></span>
        <button type="button" id="place-tag-remove" class="place-tag-remove">✕</button>
      </div>

      <div id="place-tag-selected-list" class="place-tag-selected-list" style="display:none;"></div>

      <!-- 일반후기 다중 태그가 5개에 도달하면 placeTag.js의 updatePlaceTagLimitUI()가 이 문구를 보여주고
           위 open-btn을 숨김. 기본은 숨김 상태 -->
      <p id="place-tag-limit-msg" class="place-tag-limit-msg" style="display:none;">장소 태그는 최대 5개까지 추가할 수 있습니다.</p>

      <button type="button" id="place-tag-open-btn" class="place-tag-open-btn">장소 검색해서 태그하기</button>
    </div>

    <!-- 버튼: 취소는 buttonComponent 를 내비게이션 용도로, 게시하기는 순수 제출 버튼으로 -->
    <div class="form-actions">
      <div class="btn-nav-wrap" data-btn-nav="${cp}/community/list">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="취소" />
          <jsp:param name="color" value="var(--card)" />
          <jsp:param name="width" value="100%" />
        </jsp:include>
      </div>

      <div class="btn-submit-wrap">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="게시하기" />
          <jsp:param name="width" value="100%" />
        </jsp:include>
      </div>
    </div>

  </form>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

<jsp:include page="placeSearchModal.jsp">
  <jsp:param name="modalId" value="placeSearchModal" />
</jsp:include>

<jsp:include page="collageModal.jsp" />
<jsp:include page="sliderModal.jsp" />

<jsp:include page="titleLengthModal.jsp" />

<!-- 방문자인증후기 장소 태그 필수 안내 (titleLengthModal.jsp를 modalId/message로 재사용) -->
<jsp:include page="titleLengthModal.jsp">
  <jsp:param name="modalId" value="placeTagRequiredModal" />
  <jsp:param name="message" value="방문자인증후기는 장소를 1곳 태그해야 게시할 수 있습니다." />
</jsp:include>

<!-- 콜라주/슬라이더 빌더 사진 개수 제한(각 4장) 안내 (titleLengthModal.jsp를 modalId/message로 재사용) -->
<jsp:include page="titleLengthModal.jsp">
  <jsp:param name="modalId" value="collageLimitModal" />
  <jsp:param name="message" value="콜라주에는 사진을 최대 4장까지 추가할 수 있습니다." />
</jsp:include>
<jsp:include page="titleLengthModal.jsp">
  <jsp:param name="modalId" value="sliderLimitModal" />
  <jsp:param name="message" value="슬라이더에는 사진을 최대 4장까지 추가할 수 있습니다." />
</jsp:include>

<!-- 본문 전체 이미지 개수(50장)/텍스트 길이(9999자) 제한 안내: message는 contentEditor.js가 상황에 맞게 채움 -->
<jsp:include page="titleLengthModal.jsp">
  <jsp:param name="modalId" value="contentLimitModal" />
  <jsp:param name="message" value="" />
</jsp:include>

<script>window.CP = "${cp}";</script>
<script src="${cp}/js/community/contentEditor/constants.js"></script>
<script src="${cp}/js/community/contentEditor/dom.js"></script>
<script src="${cp}/js/community/contentEditor/state.js"></script>
<script src="${cp}/js/community/contentEditor/range.js"></script>
<script src="${cp}/js/community/contentEditor/canvasUtils.js"></script>
<script src="${cp}/js/community/contentEditor/blockChrome.js"></script>
<script src="${cp}/js/community/contentEditor/singleImage.js"></script>
<script src="${cp}/js/community/contentEditor/collage.js"></script>
<script src="${cp}/js/community/contentEditor/slider.js"></script>
<script src="${cp}/js/community/contentEditor/serialize.js"></script>
<script src="${cp}/js/common/highlightKeyword.js"></script>
<script src="${cp}/js/community/placeTag.js"></script>
<script src="${cp}/js/community/titleValidation.js"></script>
</body>
</html>
