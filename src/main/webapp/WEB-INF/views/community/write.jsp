<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>새 글쓰기</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community.css">
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div class="write-container">

  <a href="${cp}/community/list" class="back-link">&lt; 목록으로</a>
  <h1 class="page-title">새 글쓰기</h1>

  <form action="${cp}/community/write" method="post" enctype="multipart/form-data">

    <!-- 카테고리: value 는 PostCategory enum 의 value 와 동일해야 함 -->
    <div class="field">
      <label class="field-label">카테고리</label>
      <div class="category-group">
        <input type="radio" name="category" id="cat-general" value="일반" checked>
        <label for="cat-general" class="category-card">
          <div class="cat-name">일반</div>
          <div class="cat-desc">자유로운 여행 이야기</div>
        </label>

        <input type="radio" name="category" id="cat-companion" value="모집">
        <label for="cat-companion" class="category-card">
          <div class="cat-name">모집(동행)</div>
          <div class="cat-desc">동행자를 구하는 글</div>
        </label>

        <input type="radio" name="category" id="cat-general-review" value="일반후기">
        <label for="cat-general-review" class="category-card">
          <div class="cat-name">일반후기</div>
          <div class="cat-desc">다녀온 여행 후기</div>
        </label>

        <input type="radio" name="category" id="cat-verified-review" value="방문자인증후기">
        <label for="cat-verified-review" class="category-card">
          <div class="cat-name">방문자인증후기</div>
          <div class="cat-desc">방문 인증 후 남기는 후기</div>
        </label>
      </div>
    </div>

    <!-- 제목 -->
    <div class="field">
      <label class="field-label" for="title">제목</label>
      <input type="text" id="title" name="title" class="text-input"
             placeholder="제목을 입력하세요" required>
    </div>

    <!-- 내용 -->
    <div class="field">
      <label class="field-label" for="content">내용</label>
      <textarea id="content" name="content" class="text-area" rows="12"
                placeholder="여행 경험을 자세히 공유해주세요.." required></textarea>
    </div>

    <!-- 이미지 -->
    <div class="field">
      <label class="field-label">이미지</label>
      <label for="images" class="image-upload-box">
        클릭해서 이미지를 추가하세요 (여러 장 선택 가능)
      </label>
      <input type="file" id="images" name="images" accept="image/*" multiple hidden>
      <div id="preview"></div>
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

<script src="${cp}/js/common.js"></script>
<script src="${cp}/js/community/imageUpload.js"></script>
</body>
</html>
