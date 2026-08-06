// community/postSearchHighlight.js
// list.jsp 전용: q로 검색된 게시글 목록에서, 제목/작성자에 매칭된 검색어를 highlightKeyword()로 굵게 표시.
// highlightKeyword.js가 먼저 로드되어 있어야 함.

document.addEventListener('DOMContentLoaded', function () {
  const table = document.querySelector('.table[data-search-keyword]');
  if (!table) return;

  const keyword = table.dataset.searchKeyword;
  if (!keyword) return;

  table.querySelectorAll('.js-highlight').forEach(function (el) {
    el.innerHTML = highlightKeyword(el.textContent, keyword);
  });
});
