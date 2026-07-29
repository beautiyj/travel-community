<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>게시글 수정</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/placeSearchModal.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/community.css">
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div class="write-container">

  <!-- 수정 취소 시 원래 게시글로 돌아감 -->
  <a href="${cp}/community/detail?postId=${post.postId}" class="back-link">&lt; 게시글로</a>
  <h1 class="page-title">게시글 수정</h1>

  <form action="${cp}/community/update" method="post" enctype="multipart/form-data">

    <!-- 어떤 글을 수정하는지 서버에 전달 -->
    <input type="hidden" name="postId" value="${post.postId}">

    <!-- 카테고리: 기존 값과 일치하는 항목에 checked (value 는 PostCategory enum 의 value 와 동일) -->
    <div class="field">
      <label class="field-label">카테고리</label>
      <div class="category-group">
        <input type="radio" name="category" id="cat-general" value="일반"
               ${post.category == '일반' ? 'checked' : ''}>
        <label for="cat-general" class="category-card">
          <div class="cat-name">일반</div>
          <div class="cat-desc">자유로운 여행 이야기</div>
        </label>

        <input type="radio" name="category" id="cat-companion" value="모집"
               ${post.category == '모집' ? 'checked' : ''}>
        <label for="cat-companion" class="category-card">
          <div class="cat-name">모집(동행)</div>
          <div class="cat-desc">동행자를 구하는 글</div>
        </label>

        <input type="radio" name="category" id="cat-general-review" value="일반후기"
               ${post.category == '일반후기' ? 'checked' : ''}>
        <label for="cat-general-review" class="category-card">
          <div class="cat-name">일반후기</div>
          <div class="cat-desc">다녀온 여행 후기</div>
        </label>

        <input type="radio" name="category" id="cat-verified-review" value="방문자인증후기"
               ${post.category == '방문자인증후기' ? 'checked' : ''}>
        <label for="cat-verified-review" class="category-card">
          <div class="cat-name">방문자인증후기</div>
          <div class="cat-desc">방문 인증 후 남기는 후기</div>
        </label>
      </div>
    </div>

    <!-- 장소 태그: "방문자인증후기" 카테고리일 때만 노출 (placeTag.js가 카테고리 변경에 맞춰 토글)
         이미 태그된 장소가 있으면 미리 채워서 보여줌. 한 게시글에 장소 1개만 태그 가능 -->
    <div class="field" id="place-tag-field"
         style="${post.category == '방문자인증후기' ? '' : 'display:none;'}">
      <label class="field-label">장소 태그</label>
      <input type="hidden" id="placeId" name="placeId" value="${post.placeId}">

      <div id="place-tag-selected" class="place-tag-selected"
           style="${empty post.placeId ? 'display:none;' : ''}">
        <span id="place-tag-selected-name">${post.placeName}</span>
        <button type="button" id="place-tag-remove" class="place-tag-remove">✕</button>
      </div>

      <button type="button" id="place-tag-open-btn" class="place-tag-open-btn"
              style="${empty post.placeId ? '' : 'display:none;'}">장소 검색해서 태그하기</button>
    </div>

    <!-- 작성자: 읽기 전용 -->
    <div class="field">
      <label class="field-label">작성자</label>
      <input type="text" value="${post.nickname}" readonly class="text-input readonly">
    </div>

    <!-- 제목: 기존 값 채움 -->
    <div class="field">
      <label class="field-label" for="title">제목</label>
      <input type="text" id="title" name="title" class="text-input"
             value="${post.title}" placeholder="제목을 입력하세요" required>
    </div>

    <!-- 내용: 기존 값 채움 -->
    <div class="field">
      <label class="field-label" for="content">내용</label>
      <textarea id="content" name="content" class="text-area" rows="12"
                placeholder="여행 경험을 자세히 공유해주세요..." required>${post.content}</textarea>
    </div>

    <!-- 이미지 추가 (새로 올릴 이미지) -->
    <div class="field">
      <label class="field-label">이미지</label>
      <label for="images" class="image-upload-box">
        클릭해서 이미지를 추가하세요 (여러 장 선택 가능)
      </label>
      <input type="file" id="images" name="images" accept="image/*" multiple hidden>
      <div id="preview"></div>
    </div>

    <!-- 버튼: 취소는 buttonComponent 를 내비게이션 용도로, 수정 완료는 순수 제출 버튼으로 -->
    <div class="form-actions">
      <div class="btn-nav-wrap" data-btn-nav="${cp}/community/detail?postId=${post.postId}">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text"  value="취소" />
          <jsp:param name="color" value="var(--card)" />
        </jsp:include>
      </div>

      <div class="btn-submit-wrap">
        <jsp:include page="../common/buttonComponent.jsp">
          <jsp:param name="text" value="수정 완료" />
        </jsp:include>
      </div>
    </div>

  </form>
</div>

<jsp:include page="placeSearchModal.jsp">
  <jsp:param name="modalId" value="placeSearchModal" />
</jsp:include>

<script>window.CP = "${cp}";</script>
<script src="${cp}/js/common.js"></script>
<script src="${cp}/js/community/imageUpload.js"></script>
<script src="${cp}/js/community/placeTag.js"></script>
</body>
</html>
