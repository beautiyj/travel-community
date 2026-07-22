/* ============================================================
   imageUpload.js — 글쓰기(write.jsp) / 수정(edit.jsp) 공용
   - 파일 선택(#images) → 미리보기(#preview)에 썸네일 추가
   - 각 썸네일의 × 버튼(.remove)으로 미리보기 목록에서 제거
   - 실제 업로드될 input.files 는 미리보기 목록과 항상 동기화(DataTransfer)
   ============================================================ */

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
