// 게시글 제목 글자수 검증 (write.jsp / edit.jsp 공용)
// - post.title 컬럼이 VARCHAR(50)이라 51자 이상 입력 후 제출하면 SQL 에러가 나므로
//   제출 전에 길이를 확인해서 초과 시 안내 모달(titleLengthModal)을 띄우고 제출을 막음
// - preventDefault만 하고 title 값은 건드리지 않으므로 모달을 닫아도 작성 중이던 내용은 그대로 남음
document.addEventListener('DOMContentLoaded', function () {
  const TITLE_MAX_LENGTH = 50;

  const titleInput = document.getElementById('title');
  if (!titleInput) return;

  const form = titleInput.closest('form');
  if (!form) return;

  form.addEventListener('submit', function (e) {
    if (titleInput.value.length > TITLE_MAX_LENGTH) {
      e.preventDefault();

      if (typeof openModal === 'function') {
        openModal('titleLengthModal');
      }
    }
  });
});
