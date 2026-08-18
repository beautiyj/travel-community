// community/contentEditor/serialize.js — edit.jsp 진입 시 서버 데이터를 본문 DOM으로 복원(rehydrate)하고,
// 제출 시 DOM을 다시 토큰 텍스트 + 새 이미지 파일 목록으로 직렬화(serialize)하는 로직
(function () {
  'use strict';
  const CE = window.ContentEditor;
  const dom = CE.dom;
  const state = CE.state;
  const constants = CE.constants;
  const singleImage = CE.singleImage;
  const collage = CE.collage;
  const slider = CE.slider;
  if (!dom.editorRoot) return;

  function appendText(container, str) {
    if (!str) return;
    const lines = str.split('\n');
    lines.forEach(function (line, i) {
      if (i > 0) container.appendChild(document.createElement('br'));
      if (line) container.appendChild(document.createTextNode(line));
    });
  }

  /* ---------- edit.jsp: 서버가 내려준 기존 content/이미지로 초기 DOM 복원 ---------- */

  function rehydrateFromServer() {
    const dataEl = document.getElementById('postContentData');
    if (!dataEl) return; // write.jsp: 기존 글이 없으므로 아무 것도 하지 않음

    const listEl = document.getElementById('postImageData');
    const imageUrls = listEl
      ? Array.from(listEl.querySelectorAll('li')).map(function (li) { return li.getAttribute('data-url'); })
      : [];
    state.existingImageCount = imageUrls.length;
    state.existingImageUrls = imageUrls;

    // Cloudinary로 저장된 최신 글은 절대 URL이 그대로 들어있고, 예전에 로컬 디스크에
    // 저장된 글은 파일명만 있어서 /upload/ 접두사를 붙여야 한다. 둘 다 지원한다.
    function imgUrlFor(n) {
      const url = imageUrls[n];
      if (/^https?:\/\//.test(url)) {
        return url;
      }
      const cp = window.CP || '';
      return cp + '/upload/' + url;
    }

    const rawContent = dataEl.textContent;
    let lastIndex = 0;
    let match;
    let hasToken = false;

    while ((match = constants.TOKEN_PATTERN.exec(rawContent)) !== null) {
      hasToken = true;
      appendText(dom.editorRoot, rawContent.slice(lastIndex, match.index));

      if (match[1] !== undefined) {
        singleImage.appendLockedSingle(dom.editorRoot, imgUrlFor, parseInt(match[1], 10), match[2], match[3]);
      } else if (match[4] !== undefined) {
        const indices = match[4].split(',').map(Number);
        slider.appendLockedSlider(dom.editorRoot, imgUrlFor, indices, match[5], match[6]);
      } else {
        const entries = match[9].split(',').map(function (s) {
          const parts = s.split('-').map(Number);
          return { n: parts[0], x: parts[1], y: parts[2], w: parts.length > 3 ? parts[3] : constants.DEFAULT_COLLAGE_ITEM_WIDTH };
        });
        const ratio = (match[7] !== undefined && match[8] !== undefined)
          ? { rw: Number(match[7]), rh: Number(match[8]) }
          : null;
        collage.appendLockedCollage(dom.editorRoot, imgUrlFor, entries, match[10], ratio, match[11]);
      }
      lastIndex = constants.TOKEN_PATTERN.lastIndex;
    }
    appendText(dom.editorRoot, rawContent.slice(lastIndex));

    // 토큰이 없는 과거 글: 텍스트 뒤에 기존 이미지를 순서대로(잠금 상태) 붙여서 복원
    if (!hasToken && imageUrls.length > 0) {
      for (let n = 0; n < imageUrls.length; n++) {
        singleImage.appendLockedSingle(dom.editorRoot, imgUrlFor, n, 'center');
      }
    }

    // 본문이 이미지로 끝나는 글(뒤에 텍스트가 없는 경우)은 appendChild로만 쌓아올린 터라
    // 마지막 블록 뒤에 캐럿을 앵커링할 자리가 없다 - anchorCaretAfterBlock과 동일한 이유로
    // 이어서 타이핑이 안 되므로, 마지막 자식이 이미지 블록이면 같은 방식(<br>)으로 앵커를 붙여둔다.
    // 페이지 로드 시점이라 반환된 Range는 포커스/셀렉션에 적용하지 않고 버린다.
    const lastChild = dom.editorRoot.lastChild;
    if (lastChild && lastChild.nodeType === Node.ELEMENT_NODE && lastChild.getAttribute('data-block')) {
      CE.range.anchorCaretAfterBlock(lastChild);
    }
  }

  rehydrateFromServer();

  /* ---------- 제출: DOM을 순서대로 훑어 토큰 텍스트 + 새 이미지 파일 목록을 만든다 ---------- */

  function serializeEditor() {
    let text = '';
    const newFiles = []; // 절대 인덱스 = existingImageCount + newFiles.length (순서 그대로)
    const usedExistingIndices = new Set(); // 본문에 남아있는(=삭제 안 된) 기존 이미지 인덱스

    function resolveIndex(el) {
      if (el.hasAttribute('data-existing-index')) {
        const idx = parseInt(el.getAttribute('data-existing-index'), 10);
        usedExistingIndices.add(idx);
        return idx;
      }
      const fileId = el.getAttribute('data-file-id');
      const idx = state.existingImageCount + newFiles.length;
      newFiles.push(state.fileMap.get(fileId));
      return idx;
    }

    function alignSuffix(node) {
      const align = node.getAttribute('data-align') || 'center';
      return align === 'center' ? '' : (':' + align);
    }

    // 편집 중 전체 블록 리사이즈 핸들로 조절한 폭(에디터 폭 대비 %). 없으면(리사이즈 안 함) 생략.
    function widthSuffix(node) {
      const bw = node.getAttribute('data-block-width');
      return bw ? (':' + bw) : '';
    }

    function walk(node) {
      if (node.nodeType === Node.TEXT_NODE) {
        text += node.textContent;
        return;
      }
      if (node.nodeType !== Node.ELEMENT_NODE) return;

      if (node.tagName === 'BR') {
        text += '\n';
        return;
      }

      const blockType = node.getAttribute && node.getAttribute('data-block');

      if (blockType === 'single') {
        const img = node.querySelector('img');
        const n = resolveIndex(img);
        text += '[[IMG:' + n + alignSuffix(node) + widthSuffix(node) + ']]\n';
        return;
      }

      if (blockType === 'collage') {
        const imgs = node.querySelectorAll('img[data-file-id], img[data-existing-index]');
        const entries = Array.from(imgs).map(function (img) {
          const n = resolveIndex(img);
          const x = Math.round(parseFloat(img.getAttribute('data-x') || '50'));
          const y = Math.round(parseFloat(img.getAttribute('data-y') || '50'));
          const w = Math.round(parseFloat(img.getAttribute('data-w') || String(constants.DEFAULT_COLLAGE_ITEM_WIDTH)));
          return n + '-' + x + '-' + y + '-' + w;
        });
        const rw = node.getAttribute('data-canvas-rw');
        const rh = node.getAttribute('data-canvas-rh');
        const ratioPrefix = (rw && rh) ? (rw + '-' + rh + ':') : '';
        text += '[[COLLAGE:' + ratioPrefix + entries.join(',') + alignSuffix(node) + widthSuffix(node) + ']]\n';
        return;
      }

      if (blockType === 'slider') {
        const imgs = node.querySelectorAll('img[data-file-id], img[data-existing-index]');
        const indices = Array.from(imgs).map(resolveIndex);
        text += '[[SLIDER:' + indices.join(',') + alignSuffix(node) + widthSuffix(node) + ']]\n';
        return;
      }

      const isBlockLevel = node.tagName === 'DIV' || node.tagName === 'P';
      Array.from(node.childNodes).forEach(walk);
      if (isBlockLevel) text += '\n';
    }

    Array.from(dom.editorRoot.childNodes).forEach(walk);

    return {
      text: text.replace(/\n{3,}/g, '\n\n').trim(),
      newFiles: newFiles,
      usedExistingIndices: usedExistingIndices
    };
  }

  /* ---------- 본문 전체 이미지 개수(50장) / 텍스트 길이(9999자) 제한 ---------- */

  // 텍스트 절삭 지점 이후의 텍스트 노드/<br>만 제거하고, [data-block] 이미지 블록은
  // 순회는 하되 내부로 들어가지 않고 그대로 둔다(이미지는 개수 맞춰 삭제하지 않음).
  function truncateEditorTrailingText(maxLen) {
    let count = 0;
    let cutNode = null;
    let cutOffset = -1;

    (function findCut(node) {
      if (cutNode) return;
      if (node.nodeType === Node.TEXT_NODE) {
        const len = node.textContent.length;
        if (count + len > maxLen) {
          cutNode = node;
          cutOffset = maxLen - count;
        } else {
          count += len;
        }
        return;
      }
      if (node.nodeType !== Node.ELEMENT_NODE) return;
      if (node.getAttribute && node.getAttribute('data-block')) return; // 이미지 블록은 건너뜀
      Array.from(node.childNodes).forEach(findCut);
    })(dom.editorRoot);

    if (!cutNode) return; // 이미 한도 이내
    cutNode.textContent = cutNode.textContent.slice(0, cutOffset);

    let reachedCut = false;
    (function removeAfter(node) {
      Array.from(node.childNodes).forEach(function (child) {
        if (child.nodeType === Node.ELEMENT_NODE && child.getAttribute && child.getAttribute('data-block')) {
          return; // 이미지 블록은 건너뛰고 유지
        }
        if (child === cutNode) {
          reachedCut = true;
          return;
        }
        if (reachedCut) {
          if (child.nodeType === Node.TEXT_NODE || child.tagName === 'BR') {
            child.remove();
          } else if (child.nodeType === Node.ELEMENT_NODE) {
            removeAfter(child);
          }
          return;
        }
        if (child.nodeType === Node.ELEMENT_NODE) removeAfter(child);
      });
    })(dom.editorRoot);
  }

  const contentLimitModal = dom.contentLimitModal;
  if (contentLimitModal) {
    const confirmBtn = contentLimitModal.querySelector('.modal-btn');
    confirmBtn && confirmBtn.addEventListener('click', function () {
      if (state.pendingTextOverflow) {
        truncateEditorTrailingText(constants.MAX_CONTENT_TEXT_LENGTH);
        state.pendingTextOverflow = false;
      }
    });
  }

  const form = dom.form;
  if (form) {
    form.addEventListener('submit', function (e) {
      const blockCount = dom.editorRoot.querySelectorAll('[data-block]').length;
      const textLength = (dom.editorRoot.textContent || '').length;
      const imageOver = blockCount > constants.MAX_TOTAL_IMAGE_BLOCKS;
      const textOver = textLength > constants.MAX_CONTENT_TEXT_LENGTH;

      if (imageOver || textOver) {
        e.preventDefault();
        const lines = [];
        if (imageOver) lines.push('본문에는 사진을 최대 ' + constants.MAX_TOTAL_IMAGE_BLOCKS + '장까지 추가할 수 있습니다. (콜라주, 슬라이더는 각각 1장으로 계산됩니다)');
        if (textOver) lines.push('본문은 ' + constants.MAX_CONTENT_TEXT_LENGTH + '자 이내로 작성해주세요. 확인을 누르면 초과된 내용이 자동으로 삭제됩니다.');
        state.pendingTextOverflow = textOver;
        const messageEl = contentLimitModal && contentLimitModal.querySelector('.modal-message');
        if (messageEl) messageEl.innerHTML = lines.join('<br>');
        if (window.openModal) window.openModal('contentLimitModal');
        return;
      }

      try {
        const result = serializeEditor();
        dom.contentField.value = result.text;

        const dt = new DataTransfer();
        result.newFiles.forEach(function (file) {
          if (file) dt.items.add(file);
        });
        dom.imagesInput.files = dt.files;

        // 본문 편집 중 지워진(=더 이상 토큰에 안 남은) 기존 이미지는 서버에 명시적으로 알려서
        // post_image 행과 Cloudinary 파일을 같이 정리하게 한다.
        form.querySelectorAll('input[name="removeImageUrls"]').forEach(function (el) { el.remove(); });
        state.existingImageUrls.forEach(function (url, idx) {
          if (result.usedExistingIndices.has(idx)) return;
          const removedInput = document.createElement('input');
          removedInput.type = 'hidden';
          removedInput.name = 'removeImageUrls';
          removedInput.value = url;
          form.appendChild(removedInput);
        });
      } catch (err) {
        // 직렬화 중 에러가 나도 최소한 입력한 텍스트라도 저장되게 (이미지 없이라도 게시는 되도록)
        console.error('contentEditor: 본문 직렬화 실패', err);
        if (!dom.contentField.value) {
          dom.contentField.value = (dom.editorRoot.textContent || '').trim() || ' ';
        }
      }
    });
  }
})();
