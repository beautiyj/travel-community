<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>게시글 수정</title>
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

<div class="write-container">

  <!-- 수정 취소 시 원래 게시글로 돌아감 -->
  <a href="${cp}/community/detail?postId=${post.postId}" class="back-link">&lt; 게시글로</a>

  <!-- 작성자 아바타 + 닉네임 -->
  <div class="blog-author-bar">
    <div class="blog-avatar">${fn:substring(post.nickname, 0, 1)}</div>
    <span class="blog-author-name">${post.nickname}</span>
  </div>

  <form action="${cp}/community/update" method="post" enctype="multipart/form-data">

    <!-- 어떤 글을 수정하는지 서버에 전달 -->
    <input type="hidden" name="postId" value="${post.postId}">

    <!-- 카테고리: dropdownSelector 와 동일한 마크업, 기존 값(post.category)으로 초기 선택 표시.
         디자인/열고닫기/숨은 라디오 동기화까지 전부 dropdownSelector.js(data-radio-name="category")가 처리함. -->
    <div class="field">
      <label class="field-label">카테고리</label>

      <div class="drop-select-container dropdown category-dropdown" id="categoryDropdown" data-radio-name="category">
        <button type="button" id="categoryDropdownTrigger" class="drop-select-trigger is-selected" aria-expanded="false">
          <div class="drop-select-left-box">
            <span class="drop-select-text" id="categoryDropdownLabel">${post.categoryLabel}</span>
          </div>
          <svg class="drop-select-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>

        <ul class="dropdown-menu drop-select-menu" aria-labelledby="categoryDropdownTrigger">
          <c:forEach var="cat" items="${categoryList}">
            <li>
              <button type="button" class="dropdown-item drop-menu-item ${cat.value == post.category ? 'is-active' : ''}"
                      data-value="${cat.value}" data-label="${cat.displayLabel}">
                ${cat.displayLabel}
              </button>
            </li>
          </c:forEach>
        </ul>
      </div>

      <div class="category-radio-group" aria-hidden="true">
        <c:forEach var="cat" items="${categoryList}">
          <input type="radio" name="category" value="${cat.value}" ${cat.value == post.category ? 'checked' : ''}>
        </c:forEach>
      </div>
    </div>

    <!-- 제목: 기존 값 채움. DB 컬럼(post.title)이 VARCHAR(50)이라 51자 이상이면 수정 시
         SQL 에러가 나서 제출 시 titleValidation.js가 길이를 먼저 검사함 (아래 titleLengthModal 참고) -->
    <div class="field">
      <input type="text" id="title" name="title" class="title-input-plain"
             value="${post.title}" placeholder="제목을 입력하세요" required>
    </div>

    <!-- 장소 태그 -->
    <input type="hidden" id="placeId" name="placeId" value="${post.placeId}">

    <!-- 본문: contentEditor.js 가 아래 postContentData/postImageData 를 읽어 초기 DOM을 복원함
         (기존 이미지는 잠금 상태로 원래 위치에 표시, 새로 추가한 이미지만 삭제/리사이즈/정렬 가능) -->
    <div class="field">
      <div class="editor-toolbar">
        <button type="button" id="toolPhotoBtn" class="editor-tool-btn">🖼 사진</button>
        <button type="button" id="toolCollageBtn" class="editor-tool-btn">▦ 콜라주</button>
        <button type="button" id="toolSliderBtn" class="editor-tool-btn">⇄ 슬라이더</button>
      </div>

      <div id="contentEditor" class="content-editor" contenteditable="true"
           data-placeholder="여행 경험을 자세히 공유해주세요..."></div>

      <!-- required 안 씀: display:none 이라 검증 실패 시 브라우저가 포커스를 못 줘서 그냥 조용히 제출이 막혀버림 -->
      <textarea id="content" name="content" style="display:none"></textarea>
      <input type="file" id="photoInput" accept="image/*" multiple hidden>
      <input type="file" id="images" name="images" accept="image/*" multiple hidden>

      <!-- 기존 콘텐츠/이미지 원본 데이터 (contentEditor.js 가 읽어서 초기 DOM 복원 후 이 두 요소는 그대로 화면에 안 보임)
           fn:escapeXml 로 안전하게 인코딩 → 일반 엘리먼트라 브라우저가 다시 복원해주므로 JS에서는 textContent/속성값으로 원문 그대로 읽힘 -->
      <div id="postContentData" style="display:none">${fn:escapeXml(post.content)}</div>
      <ul id="postImageData" style="display:none">
        <c:forEach var="img" items="${post.imageList}">
          <li data-url="${fn:escapeXml(img.imageUrl)}"></li>
        </c:forEach>
      </ul>
    </div>

    <!-- 장소 태그: "방문자인증후기"/"일반후기" 카테고리일 때만 노출, 취소/수정완료 버튼 바로 위
         이미 태그된 장소가 있으면 미리 채워서 보여줌
         방문자인증후기: 단일 선택(#place-tag-selected, 필수) / 일반후기: 다중 선택(#place-tag-selected-list, 선택사항, 최대 5개) -->
    <div class="field" id="place-tag-field"
         style="${(post.category == '방문자인증후기' || post.category == '일반후기') ? '' : 'display:none;'}">
      <label class="field-label">장소 태그</label>

      <div id="place-tag-selected" class="place-tag-selected"
           style="${empty post.placeId ? 'display:none;' : ''}">
        <span id="place-tag-selected-name">${post.placeName}
          <div class="tag-view type-${post.placeType}"><span class="tag-text"><c:choose>
            <c:when test="${post.placeType == 'stay'}">숙박</c:when>
            <c:when test="${post.placeType == 'food'}">맛집</c:when>
            <c:otherwise>관광지</c:otherwise>
          </c:choose></span></div>
        </span>
        <button type="button" id="place-tag-remove" class="place-tag-remove">✕</button>
      </div>

      <!-- 일반후기 다중 태그 pre-fill: placeTag.js의 addPlaceTagChip()이 만드는 것과 동일한 마크업이어야
           칩 제거(이벤트 위임)/중복 방지 로직이 새로 추가한 태그와 똑같이 동작함 -->
      <div id="place-tag-selected-list" class="place-tag-selected-list"
           style="${post.category == '일반후기' ? '' : 'display:none;'}">
        <c:forEach var="tag" items="${post.placeTags}">
          <span class="place-tag-selected" data-place-id="${tag.placeId}">
            <input type="hidden" name="placeIds" value="${tag.placeId}">
            <span>${tag.name}
              <div class="tag-view type-${tag.placeType}"><span class="tag-text"><c:choose>
                <c:when test="${tag.placeType == 'stay'}">숙박</c:when>
                <c:when test="${tag.placeType == 'food'}">맛집</c:when>
                <c:otherwise>관광지</c:otherwise>
              </c:choose></span></div>
            </span>
            <button type="button" class="place-tag-remove" data-remove-place-id="${tag.placeId}">✕</button>
          </span>
        </c:forEach>
      </div>

      <!-- 일반후기 다중 태그가 5개에 도달하면 placeTag.js의 updatePlaceTagLimitUI()가 이 문구를 보여주고
           아래 open-btn을 숨김 (이미 5개가 pre-fill된 상태로 페이지가 로드되는 경우도 JS가 처리). 기본은 숨김 상태 -->
      <p id="place-tag-limit-msg" class="place-tag-limit-msg" style="display:none;">장소 태그는 최대 5개까지 추가할 수 있습니다.</p>

      <button type="button" id="place-tag-open-btn" class="place-tag-open-btn"
              style="${empty post.placeId ? '' : 'display:none;'}">장소 검색해서 태그하기</button>
    </div>

    <!-- 버튼: 취소는 buttonComponent 를 내비게이션 용도로, 수정 완료는 순수 제출 버튼으로 -->
    <div class="form-actions">
      <div class="btn-nav-wrap" data-btn-nav="${cp}/community/detail?postId=${post.postId}">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="취소" />
          <jsp:param name="color" value="var(--card)" />
          <jsp:param name="width" value="100%" />
        </jsp:include>
      </div>

      <div class="btn-submit-wrap">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="수정 완료" />
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
