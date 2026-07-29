// common/highlightKeyword.js
// 검색 결과 목록(장소 태그 검색 모달 / 게시글 목록 검색)에서 매칭된 검색어를 <b>로 감싸 강조 표시하는 공용 헬퍼.
// text를 키워드 기준으로 먼저 split한 뒤 각 조각을 개별 escape하므로, escape와 매칭 순서가 꼬여
// 하이라이트가 깨지는 일이 없음. text 자체의 escape도 이 함수가 처리하므로 호출부에서 별도
// escapeHtml이 필요 없음.

function highlightKeyword(text, keyword) {
  const str = String(text == null ? '' : text);
  if (!keyword) return _hkEscapeHtml(str);

  const escapedKeyword = String(keyword).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const re = new RegExp('(' + escapedKeyword + ')', 'ig');

  return str.split(re).map(function (part, i) {
    return i % 2 === 1 ? '<b>' + _hkEscapeHtml(part) + '</b>' : _hkEscapeHtml(part);
  }).join('');
}

function _hkEscapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
