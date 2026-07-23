// 댓글 작성 권한 없음(commentDenied) 안내 모달
// - URL 쿼리스트링에 commentDenied=true가 있으면 페이지 로드 시 자동으로 모달 오픈
// - "닫기"(data-modal-close) 클릭 시 페이지 이동 없이 닫히므로,
//   새로고침해도 모달이 또 뜨지 않도록 URL에서 commentDenied 파라미터를 제거
document.addEventListener('DOMContentLoaded', function () {
  const params = new URLSearchParams(window.location.search);

  if (params.get('commentDenied') !== 'true') {
    return;
  }

  if (typeof openModal === 'function') {
    openModal('commentDeniedModal');
  }

  const modal = document.getElementById('commentDeniedModal');
  if (!modal) {
    return;
  }

  modal.querySelectorAll('[data-modal-close]').forEach(function (closeEl) {
    closeEl.addEventListener('click', function () {
      params.delete('commentDenied');

      const cleanQuery = params.toString();
      const cleanUrl = window.location.pathname + (cleanQuery ? '?' + cleanQuery : '');

      history.replaceState(null, '', cleanUrl);
    });
  });
});