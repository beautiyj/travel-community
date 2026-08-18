// community/contentEditor/collage.js — 콜라주(고정 2×2 합성) 블록 삽입/서버 데이터 복원과 콜라주 빌더 모달 담당
(function () {
  'use strict';
  const CE = window.ContentEditor;
  const dom = CE.dom;
  const state = CE.state;
  const range = CE.range;
  const blockChrome = CE.blockChrome;
  const canvasUtils = CE.canvasUtils;
  const constants = CE.constants;
  if (!dom.editorRoot) return;

  /* ---------- 새 이미지 블록 생성 (콜라주: 고정 2×2 격자로 합성) ---------- */

  // 콜라주 빌더 4칸에 채워진 사진들을 고정 2×2 격자(4:3 프레임)로 합성해 하나의 이미지 File로
  // 만든다. 각 칸은 가로/세로 중 더 크게 확대해야 하는 쪽 기준으로 칸을 꽉 채우고(= CSS
  // object-fit: cover와 동일), 원본 비율을 무시하고 넘치는 부분은 칸 영역으로 클리핑해서
  // 잘라낸다. 칸 사이 여백도 없어 사진들이 서로 완전히 맞닿는다.
  // cellEls[i]는 items[i]와 같은 순서로 빌더 그리드에 렌더링된 .collage-builder-slot 이고,
  // 이미 화면에 표시되어 있어 그 안의 <img>를 그대로 drawImage 소스로 쓸 수 있다.
  function composeCollageImage(items, cellEls) {
    const canvasW = constants.COMPOSE_CANVAS_MAX_DIM;
    const canvasH = Math.round(constants.COMPOSE_CANVAS_MAX_DIM * 3 / 4);

    const canvas = document.createElement('canvas');
    canvas.width = canvasW;
    canvas.height = canvasH;
    const ctx = canvas.getContext('2d');

    const cellW = canvasW / 2;
    const cellH = canvasH / 2;

    items.forEach(function (item, i) {
      const img = cellEls[i] && cellEls[i].querySelector('img');
      if (!img) return;
      const col = i % 2;
      const row = Math.floor(i / 2);
      const cellX = col * cellW;
      const cellY = row * cellH;

      const naturalW = img.naturalWidth || 1;
      const naturalH = img.naturalHeight || 1;
      const scale = Math.max(cellW / naturalW, cellH / naturalH); // cover: 항상 칸을 꽉 채움
      const drawW = naturalW * scale;
      const drawH = naturalH * scale;
      const drawX = cellX + (cellW - drawW) / 2; // 가로 중앙 정렬
      const drawY = cellY + (cellH - drawH) / 2; // 세로 중앙 정렬

      ctx.save();
      ctx.beginPath();
      ctx.rect(cellX, cellY, cellW, cellH);
      ctx.clip();
      ctx.drawImage(img, drawX, drawY, drawW, drawH);
      ctx.restore();
    });

    return canvasUtils.canvasToImageFile(canvas, 'collage');
  }

  // 합성된 콜라주 이미지 1장을 콜라주 블록 구조(.collage-canvas 배경 + 100% 채운 .collage-item)
  // 안에 넣어 삽입한다. 합성 캔버스가 항상 고정 4:3 비율로 그려지므로(community.css의
  // .collage-canvas { aspect-ratio: 4/3 } 기본값과 동일) 이미지가 .collage-canvas를 꽉 채운다
  // (appendLockedCollage가 이미 항목 1개짜리 콜라주도 그대로 지원함).
  function insertCollageBlock(file, insertRange) {
    const id = 'f' + (state.fileIdSeq++);
    state.fileMap.set(id, file);
    const url = URL.createObjectURL(file);

    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.draggable = false;
    wrapper.className = 'content-collage-block align-center';
    wrapper.setAttribute('data-block', 'collage');
    wrapper.setAttribute('data-align', 'center');

    const canvas = document.createElement('div');
    canvas.className = 'collage-canvas';
    const cell = document.createElement('div');
    cell.className = 'collage-item';
    cell.style.left = '50%';
    cell.style.top = '50%';
    cell.style.width = '100%';
    const img = document.createElement('img');
    img.src = url;
    img.setAttribute('data-file-id', id);
    img.setAttribute('data-x', 50);
    img.setAttribute('data-y', 50);
    img.setAttribute('data-w', 100);
    img.alt = '첨부 이미지';
    img.draggable = false;
    cell.appendChild(img);
    canvas.appendChild(cell);
    wrapper.appendChild(canvas);
    blockChrome.appendBlockControls(wrapper);

    return range.insertBlockAtRange(insertRange, wrapper);
  }

  // entries: [{n, x, y, w}] — 구버전 자유배치 콜라주(항목 여러 개, x/y/w 좌표 있음) 하위 호환용.
  // 새로 만드는 콜라주는 항상 항목 1개(고정 2×2 격자로 합성된 이미지)로 저장되지만, 이전에
  // 저장된 자유배치 콜라주가 있다면 겹침/위치를 그대로(리사이즈 불가) 재현한다.
  // ratio: {rw, rh} — 삽입 당시 여백을 잘라낸 캔버스 비율(없으면 CSS 기본값인 4:3 유지)
  function appendLockedCollage(container, imgUrlFor, entries, align, ratio, bw) {
    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.className = 'content-collage-block align-' + (align || 'center');
    wrapper.setAttribute('data-block', 'collage');
    wrapper.setAttribute('data-align', align || 'center');
    if (bw) {
      wrapper.style.width = bw + '%';
      wrapper.setAttribute('data-block-width', bw);
    }
    if (ratio) {
      wrapper.setAttribute('data-canvas-rw', ratio.rw);
      wrapper.setAttribute('data-canvas-rh', ratio.rh);
    }

    const canvas = document.createElement('div');
    canvas.className = 'collage-canvas';
    if (ratio) canvas.style.aspectRatio = ratio.rw + ' / ' + ratio.rh;
    entries.forEach(function (entry) {
      const w = entry.w || constants.DEFAULT_COLLAGE_ITEM_WIDTH;
      const cell = document.createElement('div');
      cell.className = 'collage-item';
      cell.style.left = entry.x + '%';
      cell.style.top = entry.y + '%';
      cell.style.width = w + '%';
      const img = document.createElement('img');
      img.src = imgUrlFor(entry.n);
      img.setAttribute('data-existing-index', String(entry.n));
      img.setAttribute('data-x', entry.x);
      img.setAttribute('data-y', entry.y);
      img.setAttribute('data-w', w);
      img.alt = '첨부 이미지';
      img.draggable = false;
      cell.appendChild(img);
      canvas.appendChild(cell);
    });
    wrapper.appendChild(canvas);
    blockChrome.appendBlockControls(wrapper);
    container.appendChild(wrapper);
  }

  /* ---------- "콜라주" 버튼: 빌더 모달 (고정 2×2 격자에 순서대로 채워 넣음) ---------- */

  function openCollageBuilder() {
    range.saveCurrentRange();
    state.builderFiles = [];
    renderCollageGrid();
    if (window.openModal) window.openModal('collageModal');
  }

  dom.toolCollageBtn && dom.toolCollageBtn.addEventListener('click', openCollageBuilder);

  dom.collageFileInput && dom.collageFileInput.addEventListener('change', function () {
    const files = Array.from(dom.collageFileInput.files).filter(function (f) { return f.type.indexOf('image/') === 0; });
    const availableSlots = constants.MAX_BUILDER_ITEMS - state.builderFiles.length;
    files.slice(0, availableSlots).forEach(function (file) {
      state.builderFiles.push({ id: 'f' + (state.fileIdSeq++), file: file, url: URL.createObjectURL(file) });
    });
    dom.collageFileInput.value = '';
    renderCollageGrid();
    if (files.length > availableSlots && window.openModal) window.openModal('collageLimitModal');
  });

  // 콜라주: 고정 2×2 격자(4칸)에 순서대로 채워 넣는다. 빈 칸은 placeholder를 보여주고
  // 클릭하면 파일 선택이 열리며, 채워진 칸은 미리보기 + 하단 삭제 바를 보여준다.
  // 채워진 칸끼리는 드래그로 자리를 맞바꿀 수 있다.
  // 실제 크롭/합성은 완료 시 composeCollageImage가 담당하므로 여기 미리보기는
  // object-fit:cover(CSS)로만 칸에 맞춰 보여준다.
  function renderCollageGrid() {
    if (!dom.collageCanvas) return;
    dom.collageCanvas.innerHTML = '';
    for (let i = 0; i < constants.MAX_BUILDER_ITEMS; i++) {
      const item = state.builderFiles[i];
      const slot = document.createElement('div');
      slot.className = 'collage-builder-slot';
      slot.dataset.i = String(i);
      if (item) {
        slot.classList.add('is-filled');
        slot.innerHTML =
          '<img src="' + item.url + '" alt="선택한 이미지" draggable="false">' +
          '<button type="button" class="collage-builder-remove" data-i="' + i + '" title="삭제">🗑 삭제</button>';
      } else {
        slot.textContent = '+ 사진 추가';
        slot.addEventListener('click', function () {
          if (dom.collageFileInput) dom.collageFileInput.click();
        });
      }
      dom.collageCanvas.appendChild(slot);
    }
    if (dom.collageConfirmBtn) dom.collageConfirmBtn.disabled = state.builderFiles.length !== constants.MAX_BUILDER_ITEMS;
  }

  dom.collageCanvas && dom.collageCanvas.addEventListener('click', function (e) {
    const btn = e.target.closest('.collage-builder-remove');
    if (!btn) return;
    state.builderFiles.splice(Number(btn.getAttribute('data-i')), 1);
    renderCollageGrid();
  });

  // 콜라주: 채워진 칸을 다른 채워진 칸으로 드래그하면 두 사진의 자리가 서로 바뀐다.
  // 네이티브 HTML5 draggable 대신 Pointer Events로 구현해 모바일 터치에서도 동작하게 한다
  // (본문 블록 재배치 드래그와 같은 방식).
  dom.collageCanvas && dom.collageCanvas.addEventListener('pointerdown', function (e) {
    if (e.target.closest('.collage-builder-remove')) return;
    const startSlot = e.target.closest('.collage-builder-slot.is-filled');
    if (!startSlot) return;

    const sourceI = Number(startSlot.dataset.i);
    const pointerId = e.pointerId;
    const startX = e.clientX;
    const startY = e.clientY;
    let dragging = false;
    let currentTarget = null;

    function clearDropTarget() {
      if (currentTarget) currentTarget.classList.remove('is-drop-target');
      currentTarget = null;
    }

    function onMove(ev) {
      if (ev.pointerId !== pointerId) return;
      if (!dragging) {
        if (Math.abs(ev.clientX - startX) < 4 && Math.abs(ev.clientY - startY) < 4) return;
        dragging = true;
        startSlot.classList.add('is-dragging');
      }
      ev.preventDefault();
      const el = document.elementFromPoint(ev.clientX, ev.clientY);
      const hovered = el && el.closest('.collage-builder-slot.is-filled');
      const target = hovered && hovered !== startSlot ? hovered : null;
      if (target !== currentTarget) {
        clearDropTarget();
        if (target) {
          target.classList.add('is-drop-target');
          currentTarget = target;
        }
      }
    }

    function onUp(ev) {
      if (ev.pointerId !== pointerId) return;
      startSlot.releasePointerCapture(pointerId);
      startSlot.removeEventListener('pointermove', onMove);
      startSlot.removeEventListener('pointerup', onUp);
      startSlot.removeEventListener('pointercancel', onUp);
      startSlot.classList.remove('is-dragging');
      const dropTarget = currentTarget;
      clearDropTarget();
      if (!dragging || !dropTarget) return;

      const targetI = Number(dropTarget.dataset.i);
      const tmp = state.builderFiles[sourceI];
      state.builderFiles[sourceI] = state.builderFiles[targetI];
      state.builderFiles[targetI] = tmp;
      renderCollageGrid();
    }

    startSlot.setPointerCapture(pointerId);
    startSlot.addEventListener('pointermove', onMove);
    startSlot.addEventListener('pointerup', onUp);
    startSlot.addEventListener('pointercancel', onUp);
  });

  // 완료 시 고정 2×2 격자의 4칸을 그대로 캔버스에 합성해 하나의 이미지로 만든 뒤,
  // 일반 사진 한 장과 동일하게 현재 캐럿 위치에 삽입한다.
  dom.collageConfirmBtn && dom.collageConfirmBtn.addEventListener('click', function () {
    if (state.builderFiles.length !== constants.MAX_BUILDER_ITEMS || dom.collageConfirmBtn.disabled) return;
    const confirmRange = state.savedRange ? state.savedRange.cloneRange() : range.endOfEditorRange();
    range.restoreRange(confirmRange);
    const cellEls = Array.from(dom.collageCanvas.querySelectorAll('.collage-builder-slot'));

    dom.collageConfirmBtn.disabled = true;
    composeCollageImage(state.builderFiles, cellEls).then(function (file) {
      range.restoreRange(confirmRange);
      insertCollageBlock(file, confirmRange);
      if (window.closeModal) window.closeModal('collageModal');
      state.builderFiles = [];
    }).catch(function (err) {
      console.error('콜라주 합성 실패', err);
    }).then(function () {
      dom.collageConfirmBtn.disabled = state.builderFiles.length !== constants.MAX_BUILDER_ITEMS;
    });
  });

  CE.collage = {
    appendLockedCollage: appendLockedCollage
  };
})();
