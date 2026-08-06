// community/contentEditor/singleImage.js — 단일 이미지 블록 삽입/서버 데이터 복원 및 "사진" 툴바 버튼 담당
(function () {
  'use strict';
  const CE = window.ContentEditor;
  const dom = CE.dom;
  const state = CE.state;
  const range = CE.range;
  const blockChrome = CE.blockChrome;
  if (!dom.editorRoot) return;

  /* ---------- 새 이미지 블록 생성 (사진 1장) ---------- */

  function insertSingleImage(file, insertRange) {
    const id = 'f' + (state.fileIdSeq++);
    state.fileMap.set(id, file);
    const url = URL.createObjectURL(file);

    const wrapper = document.createElement('div');
    wrapper.className = 'content-img-block align-center';
    wrapper.contentEditable = 'false';
    wrapper.draggable = false; // 커스텀 드래그(mousedown 기반)만 쓰고 네이티브 드래그는 끔
    wrapper.setAttribute('data-block', 'single');
    wrapper.setAttribute('data-align', 'center');
    wrapper.setAttribute('data-file-id', id);
    wrapper.innerHTML =
      '<img src="' + url + '" data-file-id="' + id + '" alt="첨부 이미지" draggable="false">' +
      '<button type="button" class="block-remove" title="삭제">🗑 삭제</button>';

    return range.insertBlockAtRange(insertRange, wrapper);
  }

  // 기존(이미 저장된) 이미지도 새로 추가한 이미지와 동일하게 삭제/드래그 재배치/리사이즈 가능.
  // 삭제해도 서버의 post_image 행 자체가 지워지는 건 아니고, 그냥 본문 토큰에서 참조가 빠질 뿐이라
  // 별도 삭제 API 없이도 안전하다(참조 안 되는 이미지 행이 남는 것뿐).
  function appendLockedSingle(container, imgUrlFor, n, align, bw) {
    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.className = 'content-img-block align-' + (align || 'center');
    wrapper.setAttribute('data-block', 'single');
    wrapper.setAttribute('data-align', align || 'center');
    wrapper.setAttribute('data-existing-index', String(n));
    if (bw) {
      wrapper.style.width = bw + '%';
      wrapper.setAttribute('data-block-width', bw);
    }
    const img = document.createElement('img');
    img.src = imgUrlFor(n);
    img.setAttribute('data-existing-index', String(n));
    img.alt = '첨부 이미지';
    img.draggable = false;
    wrapper.appendChild(img);
    blockChrome.appendBlockControls(wrapper);
    container.appendChild(wrapper);
  }

  /* ---------- "사진" 버튼: 즉시 커서 위치에 순서대로 삽입 ---------- */

  dom.toolPhotoBtn && dom.toolPhotoBtn.addEventListener('click', function () {
    range.saveCurrentRange();
    dom.photoInput.value = '';
    dom.photoInput.click();
  });

  dom.photoInput && dom.photoInput.addEventListener('change', function () {
    const files = Array.from(dom.photoInput.files).filter(function (f) { return f.type.indexOf('image/') === 0; });
    let insertRange = state.savedRange ? state.savedRange.cloneRange() : range.endOfEditorRange();
    range.restoreRange(insertRange);
    files.forEach(function (file) {
      insertRange = insertSingleImage(file, insertRange);
    });
    dom.photoInput.value = '';
  });

  CE.singleImage = {
    insertSingleImage: insertSingleImage,
    appendLockedSingle: appendLockedSingle
  };
})();
