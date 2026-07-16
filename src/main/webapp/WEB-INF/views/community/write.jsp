<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>새 글쓰기</title>
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div class="write-container">

  <a href="${cp}/community/list" class="back-link">&lt; 목록으로</a>
  <h1 class="page-title">새 글쓰기</h1>

  <form action="${cp}/community/write" method="post" enctype="multipart/form-data">

    <!-- 카테고리 -->
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
          <div class="cat-name">모집 (동행)</div>
          <div class="cat-desc">동행자를 구하는 글</div>
        </label>

        <input type="radio" name="category" id="cat-review" value="후기">
        <label for="cat-review" class="category-card">
          <div class="cat-name">후기</div>
          <div class="cat-desc">다녀온 여행 후기</div>
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

    <!-- 버튼 -->
    <div class="btn-group">
      <a href="${cp}/community/list" class="btn btn-cancel">취소</a>
      <button type="submit" class="btn btn-submit">게시하기</button>
    </div>

  </form>
</div>

<script>
  const input = document.getElementById('images');
  const preview = document.getElementById('preview');
  let files = [];

  input.addEventListener('change', () => {
    for (const f of input.files) {
      if (f.type.startsWith('image/')) files.push(f);
    }
    render();
  });

  function render() {
    preview.innerHTML = '';
    files.forEach((f, i) => {
      const url = URL.createObjectURL(f);
      const div = document.createElement('div');
      div.className = 'thumb';
      div.innerHTML = '<img src="' + url + '" width="110" height="110">' +
                      '<button type="button" class="remove" data-i="' + i + '">&times;</button>';
      preview.appendChild(div);
    });
    const dt = new DataTransfer();
    files.forEach(f => dt.items.add(f));
    input.files = dt.files;
  }

  preview.addEventListener('click', (e) => {
    if (e.target.classList.contains('remove')) {
      files.splice(Number(e.target.dataset.i), 1);
      render();
    }
  });
</script>
</body>
</html>