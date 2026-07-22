/* ============================================================
   postDetail.js — 게시글 상세(detail.jsp) 전용
   - 답글 입력창 열고/닫기 (댓글마다 있는 "답글 달기" 클릭 시 토글)
   - 모달 열기/닫기(openModal/closeModal), 배너·갤러리 슬라이더는 common.js 에 있음
   ============================================================ */

function toggleReply(commentId) {
  const form = document.getElementById('reply-form-' + commentId);
  if (form) {
    form.style.display = (form.style.display === 'none') ? 'block' : 'none';
  }
}
