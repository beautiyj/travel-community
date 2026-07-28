/* ============================================================
   contentEditor.js — write.jsp / edit.jsp 공용 본문 미니 에디터
   - contenteditable 안에 커서 위치로 이미지(사진/콜라주/슬라이더)를 삽입한다.
     삽입 후 강제 줄바꿈을 넣지 않으므로, 삽입한 자리 바로 옆에서 이어 쓸 수 있다.
   - 단일 이미지/콜라주/슬라이더 블록 모두: 편집 중 리사이즈(저장 안 함) +
     블록을 직접 드래그해서 본문 안 다른 위치로 재배치 가능(재배치한 가로 위치 기준으로
     좌/가운데/우 정렬이 자동으로 정해지고, 그 정렬만 저장됨). 슬라이더는 리사이즈 핸들로
     폭뿐 아니라 높이도 조절 가능.
   - 콜라주: 빌더 모달 안 고정 캔버스에 사진들을 자유롭게(겹침 허용) 드래그로 배치 →
     완료 시 그 배치(x/y, %) 그대로 하나의 블록으로 삽입
   - 슬라이더: 사진들을 나란히 배치한 가로 스트립 + 하단 커스텀 슬라이드바로 옆으로 스크롤
   - 제출(submit) 시 DOM을 순서대로 훑어 content 텍스트에
     [[IMG:n]] / [[COLLAGE:n-x-y,...:align]] / [[SLIDER:n1,n2:align]] 토큰을 심고,
     새로 추가된 파일들을 같은 순서로 #images(file input)에 채워 넣는다.
     (서버 ImageService.saveImages() 가 기존 이미지 개수부터 sort_order를 이어붙이는 규칙과
      반드시 같은 순서를 지켜야 하므로, 인덱스 채번은 "기존(잠금) 이미지 → 새 이미지" 순서를 그대로 따른다)
   ============================================================ */
(function () {
  'use strict';

  const editorRoot = document.getElementById('contentEditor');
  if (!editorRoot) return; // 이 JS를 쓰지 않는 페이지에서는 아무 것도 하지 않음

  const contentField = document.getElementById('content');
  const imagesInput = document.getElementById('images');
  const photoInput = document.getElementById('photoInput');

  const toolPhotoBtn = document.getElementById('toolPhotoBtn');
  const toolCollageBtn = document.getElementById('toolCollageBtn');
  const toolSliderBtn = document.getElementById('toolSliderBtn');

  const imgBlockModal = document.getElementById('imgBlockModal');
  const imgBlockModalTitle = document.getElementById('imgBlockModalTitle');
  const imgBlockFileInput = document.getElementById('imgBlockFileInput');
  const imgBlockPreview = document.getElementById('imgBlockPreview');
  const imgBlockCanvas = document.getElementById('imgBlockCanvas');
  const imgBlockConfirmBtn = document.getElementById('imgBlockConfirmBtn');

  const fileMap = new Map();   // data-file-id -> File (새로 추가된 이미지만)
  let fileIdSeq = 0;
  let existingImageCount = 0;  // edit.jsp: 이미 저장된(잠금) 이미지 개수, write.jsp: 0
  let savedRange = null;       // 툴바 클릭 직전까지의 캐럿 위치
  let builderMode = null;      // 'collage' | 'slider'
  let builderFiles = [];       // [{id, file, url, x?, y?}] — 빌더 모달에서 고른 이미지들

  /* ---------- 공통 유틸 ---------- */

  function endOfEditorRange() {
    const range = document.createRange();
    range.selectNodeContents(editorRoot);
    range.collapse(false);
    return range;
  }

  function saveCurrentRange() {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0 && editorRoot.contains(sel.anchorNode)) {
      savedRange = sel.getRangeAt(0).cloneRange();
    }
  }

  function restoreRange(range) {
    editorRoot.focus();
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
  }

  function getRangeFromPoint(x, y) {
    if (document.caretRangeFromPoint) {
      return document.caretRangeFromPoint(x, y);
    }
    if (document.caretPositionFromPoint) {
      const pos = document.caretPositionFromPoint(x, y);
      if (!pos) return null;
      const range = document.createRange();
      range.setStart(pos.offsetNode, pos.offset);
      range.collapse(true);
      return range;
    }
    return null;
  }

  // getRangeFromPoint 는 문서 전체 기준이라, 마우스를 에디터 바깥(툴바/버튼 등)에 놓고 떼면
  // 엉뚱한 곳(다른 폼 요소, 심지어 페이지의 전혀 다른 영역)에 결과가 꽂힐 수 있다.
  // 좌표를 에디터 경계 안으로 clamp한 뒤 계산하고, 그래도 에디터 밖이면 폐기(에디터 끝으로 대체)한다.
  function getRangeFromPointInEditor(x, y) {
    const rect = editorRoot.getBoundingClientRect();
    const clampedX = Math.min(Math.max(x, rect.left + 1), rect.right - 1);
    const clampedY = Math.min(Math.max(y, rect.top + 1), rect.bottom - 1);
    const range = getRangeFromPoint(clampedX, clampedY);
    if (range && editorRoot.contains(range.startContainer)) return range;
    return endOfEditorRange();
  }

  // 블록을 range 위치에 그대로 끼워 넣는다. 강제 줄바꿈을 추가하지 않으므로
  // 삽입한 자리 바로 옆(또는 정렬에 따라 옆/위아래)에서 바로 이어 쓸 수 있다.
  function insertBlockAtRange(range, blockEl) {
    range.deleteContents();
    range.insertNode(blockEl);

    range.setStartAfter(blockEl);
    range.collapse(true);

    restoreRange(range);
    savedRange = range.cloneRange();
    return range;
  }

  // 콜라주/슬라이더 블록 공용: 삭제(×) + 리사이즈 핸들
  function appendBlockControls(wrapper) {
    const removeBtn = document.createElement('span');
    removeBtn.className = 'block-remove';
    removeBtn.title = '삭제';
    removeBtn.textContent = '×';
    wrapper.appendChild(removeBtn);

    const resizeHandle = document.createElement('span');
    resizeHandle.className = 'block-resize-handle';
    resizeHandle.title = '드래그해서 크기 조절';
    wrapper.appendChild(resizeHandle);
  }

  /* ---------- 슬라이더: 사진을 나란히 배치한 스트립 + 하단 커스텀 슬라이드바 ---------- */

  function sliderMarkup() {
    return (
      '<div class="slider-viewport"><div class="slider-track"></div></div>' +
      '<div class="slider-scrollbar-track"><div class="slider-scrollbar-thumb"></div></div>'
    );
  }

  // 이미지들이 로드된 뒤(원본 비율 그대로, 높이만 고정) 스트립 폭을 계산해서
  // 뷰포트보다 넓으면 하단 슬라이드바를 보여주고 드래그로 옆으로 스크롤할 수 있게 한다.
  // 리사이즈 핸들로 블록 폭/높이가 바뀌는 경우를 대비해 recompute를 wrapper에 걸어둔다.
  function initSliderStrip(wrapper) {
    const viewport = wrapper.querySelector('.slider-viewport');
    const track = wrapper.querySelector('.slider-track');
    const barTrack = wrapper.querySelector('.slider-scrollbar-track');
    const thumb = wrapper.querySelector('.slider-scrollbar-thumb');
    if (!viewport || !track || !barTrack || !thumb) return;

    let maxOffset = 0;
    let offset = 0;

    function setOffset(px) {
      offset = Math.min(maxOffset, Math.max(0, px));
      track.style.transform = 'translateX(-' + offset + 'px)';
      const thumbRatio = parseFloat(thumb.style.width) || 0;
      const leftPercent = maxOffset > 0 ? (offset / maxOffset) * (100 - thumbRatio) : 0;
      thumb.style.left = leftPercent + '%';
    }

    function recompute() {
      const trackWidth = track.scrollWidth;
      const viewportWidth = viewport.clientWidth;
      maxOffset = Math.max(0, trackWidth - viewportWidth);

      if (maxOffset <= 0) {
        barTrack.style.display = 'none';
        track.style.transform = 'translateX(0)';
        return;
      }
      barTrack.style.display = '';
      const thumbRatio = Math.max(8, (viewportWidth / trackWidth) * 100);
      thumb.style.width = thumbRatio + '%';
      setOffset(offset);
    }

    wrapper._sliderRecompute = recompute; // 리사이즈 핸들에서 폭/높이 바뀔 때 다시 호출

    thumb.addEventListener('mousedown', function (e) {
      e.preventDefault();
      const barRect = barTrack.getBoundingClientRect();

      function onMove(ev) {
        const ratio = (ev.clientX - barRect.left) / barRect.width;
        setOffset(ratio * maxOffset);
      }
      function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });

    const imgs = Array.from(track.querySelectorAll('img'));
    let remaining = imgs.length;
    if (!remaining) return;

    function loaded() {
      remaining--;
      if (remaining === 0) recompute();
    }
    imgs.forEach(function (img) {
      if (img.complete && img.naturalWidth) {
        loaded();
      } else {
        img.addEventListener('load', loaded, { once: true });
        img.addEventListener('error', loaded, { once: true });
      }
    });

    if (window.ResizeObserver) {
      const ro = new ResizeObserver(function () { recompute(); });
      ro.observe(viewport);
      ro.observe(track);
    }
  }

  /* ---------- 새 이미지 블록 생성 (사진 1장) ---------- */

  function insertSingleImage(file, range) {
    const id = 'f' + (fileIdSeq++);
    fileMap.set(id, file);
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
      '<span class="block-remove" title="삭제">×</span>' +
      '<span class="block-resize-handle" title="드래그해서 크기 조절"></span>';

    return insertBlockAtRange(range, wrapper);
  }

  /* ---------- 새 이미지 블록 생성 (콜라주: 자유 배치, 겹침 허용) ---------- */

  function insertCollageBlock(items, range) {
    items.forEach(function (item) { fileMap.set(item.id, item.file); });

    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.draggable = false;
    wrapper.className = 'content-collage-block align-center';
    wrapper.setAttribute('data-block', 'collage');
    wrapper.setAttribute('data-align', 'center');

    const canvas = document.createElement('div');
    canvas.className = 'collage-canvas';
    items.forEach(function (item) {
      const cell = document.createElement('div');
      cell.className = 'collage-item';
      cell.style.left = item.x + '%';
      cell.style.top = item.y + '%';
      const img = document.createElement('img');
      img.src = item.url;
      img.setAttribute('data-file-id', item.id);
      img.setAttribute('data-x', item.x);
      img.setAttribute('data-y', item.y);
      img.alt = '첨부 이미지';
      img.draggable = false;
      cell.appendChild(img);
      canvas.appendChild(cell);
    });
    wrapper.appendChild(canvas);
    appendBlockControls(wrapper);

    return insertBlockAtRange(range, wrapper);
  }

  /* ---------- 새 이미지 블록 생성 (슬라이더: 나란히 + 슬라이드바) ---------- */

  function insertSliderBlock(items, range) {
    items.forEach(function (item) { fileMap.set(item.id, item.file); });

    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.draggable = false;
    wrapper.className = 'content-slider-block align-center';
    wrapper.setAttribute('data-block', 'slider');
    wrapper.setAttribute('data-align', 'center');
    wrapper.innerHTML = sliderMarkup();

    const track = wrapper.querySelector('.slider-track');
    items.forEach(function (item) {
      const slideItem = document.createElement('div');
      slideItem.className = 'slider-item';
      const img = document.createElement('img');
      img.src = item.url;
      img.setAttribute('data-file-id', item.id);
      img.alt = '첨부 이미지';
      img.draggable = false;
      slideItem.appendChild(img);
      track.appendChild(slideItem);
    });
    appendBlockControls(wrapper);
    initSliderStrip(wrapper);

    return insertBlockAtRange(range, wrapper);
  }

  /* ---------- "사진" 버튼: 즉시 커서 위치에 순서대로 삽입 ---------- */

  toolPhotoBtn && toolPhotoBtn.addEventListener('click', function () {
    saveCurrentRange();
    photoInput.value = '';
    photoInput.click();
  });

  photoInput && photoInput.addEventListener('change', function () {
    const files = Array.from(photoInput.files).filter(function (f) { return f.type.indexOf('image/') === 0; });
    let range = savedRange ? savedRange.cloneRange() : endOfEditorRange();
    restoreRange(range);
    files.forEach(function (file) {
      range = insertSingleImage(file, range);
    });
    photoInput.value = '';
  });

  /* ---------- "콜라주"/"슬라이더" 버튼: 빌더 모달 ----------
     콜라주는 고정 캔버스에 자유 배치(겹침 허용), 슬라이더는 목록에서 순서만 조정 */

  function defaultCollagePosition(i) {
    const spots = [[32, 32], [68, 30], [50, 62], [74, 66], [26, 66], [50, 26], [76, 26], [24, 26], [50, 50]];
    const p = spots[i % spots.length];
    return { x: p[0], y: p[1] };
  }

  function openBuilder(mode) {
    saveCurrentRange();
    builderMode = mode;
    builderFiles = [];
    if (imgBlockModalTitle) {
      imgBlockModalTitle.textContent = mode === 'collage' ? '콜라주 만들기' : '슬라이더 만들기';
    }
    if (imgBlockCanvas) imgBlockCanvas.style.display = mode === 'collage' ? '' : 'none';
    if (imgBlockPreview) imgBlockPreview.style.display = mode === 'collage' ? 'none' : '';
    renderBuilderState();
    if (window.openModal) window.openModal('imgBlockModal');
  }

  toolCollageBtn && toolCollageBtn.addEventListener('click', function () { openBuilder('collage'); });
  toolSliderBtn && toolSliderBtn.addEventListener('click', function () { openBuilder('slider'); });

  imgBlockFileInput && imgBlockFileInput.addEventListener('change', function () {
    Array.from(imgBlockFileInput.files).forEach(function (file) {
      if (file.type.indexOf('image/') !== 0) return;
      const item = { id: 'f' + (fileIdSeq++), file: file, url: URL.createObjectURL(file) };
      if (builderMode === 'collage') {
        const pos = defaultCollagePosition(builderFiles.length);
        item.x = pos.x;
        item.y = pos.y;
      }
      builderFiles.push(item);
    });
    imgBlockFileInput.value = '';
    renderBuilderState();
  });

  function renderBuilderState() {
    if (builderMode === 'collage') renderCollageCanvas();
    else renderSliderPreview();
  }

  // 콜라주: 고정 캔버스 위에 사진을 자유롭게 드래그로 배치(겹침 허용), ×로 제거
  function renderCollageCanvas() {
    if (!imgBlockCanvas) return;
    imgBlockCanvas.innerHTML = '';
    builderFiles.forEach(function (item, i) {
      const el = document.createElement('div');
      el.className = 'collage-builder-item';
      el.style.left = item.x + '%';
      el.style.top = item.y + '%';
      el.innerHTML =
        '<img src="' + item.url + '" alt="선택한 이미지" draggable="false">' +
        '<button type="button" class="collage-builder-item-remove" data-i="' + i + '">×</button>';
      imgBlockCanvas.appendChild(el);

      el.addEventListener('mousedown', function (e) {
        if (e.target.closest('.collage-builder-item-remove')) return;
        e.preventDefault();
        const canvasRect = imgBlockCanvas.getBoundingClientRect();

        function onMove(ev) {
          let x = ((ev.clientX - canvasRect.left) / canvasRect.width) * 100;
          let y = ((ev.clientY - canvasRect.top) / canvasRect.height) * 100;
          x = Math.min(90, Math.max(10, x));
          y = Math.min(90, Math.max(10, y));
          item.x = x;
          item.y = y;
          el.style.left = x + '%';
          el.style.top = y + '%';
        }
        function onUp() {
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
        }
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      });
    });
    if (imgBlockConfirmBtn) imgBlockConfirmBtn.disabled = builderFiles.length < 2;
  }

  imgBlockCanvas && imgBlockCanvas.addEventListener('click', function (e) {
    const btn = e.target.closest('.collage-builder-item-remove');
    if (!btn) return;
    builderFiles.splice(Number(btn.getAttribute('data-i')), 1);
    renderCollageCanvas();
  });

  // 슬라이더: 목록 형태 + ‹/›로 순서(=나열 순서) 조정, ×로 제거
  function renderSliderPreview() {
    if (!imgBlockPreview) return;
    imgBlockPreview.innerHTML = '';
    builderFiles.forEach(function (item, i) {
      const div = document.createElement('div');
      div.className = 'img-block-thumb';
      div.innerHTML =
        '<img src="' + item.url + '" alt="선택한 이미지">' +
        '<div class="img-block-thumb-order">' +
          '<button type="button" class="img-block-thumb-move" data-i="' + i + '" data-dir="-1" title="앞으로"' + (i === 0 ? ' disabled' : '') + '>‹</button>' +
          '<button type="button" class="img-block-thumb-move" data-i="' + i + '" data-dir="1" title="뒤로"' + (i === builderFiles.length - 1 ? ' disabled' : '') + '>›</button>' +
        '</div>' +
        '<button type="button" class="img-block-thumb-remove" data-i="' + i + '" title="제거">×</button>';
      imgBlockPreview.appendChild(div);
    });
    if (imgBlockConfirmBtn) imgBlockConfirmBtn.disabled = builderFiles.length < 2;
  }

  imgBlockPreview && imgBlockPreview.addEventListener('click', function (e) {
    const moveBtn = e.target.closest('.img-block-thumb-move');
    if (moveBtn) {
      const i = Number(moveBtn.getAttribute('data-i'));
      const j = i + Number(moveBtn.getAttribute('data-dir'));
      if (j < 0 || j >= builderFiles.length) return;
      const tmp = builderFiles[i];
      builderFiles[i] = builderFiles[j];
      builderFiles[j] = tmp;
      renderSliderPreview();
      return;
    }

    const removeBtn = e.target.closest('.img-block-thumb-remove');
    if (removeBtn) {
      builderFiles.splice(Number(removeBtn.getAttribute('data-i')), 1);
      renderSliderPreview();
    }
  });

  imgBlockConfirmBtn && imgBlockConfirmBtn.addEventListener('click', function () {
    if (builderFiles.length < 2) return;
    const range = savedRange ? savedRange.cloneRange() : endOfEditorRange();
    restoreRange(range);
    if (builderMode === 'collage') insertCollageBlock(builderFiles, range);
    else insertSliderBlock(builderFiles, range);
    if (window.closeModal) window.closeModal('imgBlockModal');
    builderFiles = [];
  });

  /* ---------- 블록 내부 컨트롤: 삭제 (이벤트 위임) ---------- */

  editorRoot.addEventListener('click', function (e) {
    const removeBtn = e.target.closest('.block-remove');
    if (removeBtn) {
      const block = removeBtn.closest('[data-block]');
      if (block) {
        block.querySelectorAll('[data-file-id]').forEach(function (el) {
          fileMap.delete(el.getAttribute('data-file-id'));
        });
        block.remove();
      }
    }
  });

  /* ---------- 리사이즈 핸들 (단일/콜라주/슬라이더 공용, 편집 중 미리보기 용도, 저장 안 함) ----------
     가로로 끌면 블록 폭이 바뀌고, 슬라이더는 세로로 끌면 사진 높이(--slider-item-height)도 바뀐다. */

  editorRoot.addEventListener('mousedown', function (e) {
    const handle = e.target.closest('.block-resize-handle');
    if (!handle) return;
    e.preventDefault();
    e.stopPropagation();

    const block = handle.closest('[data-block]');
    if (!block) return;

    const isSlider = block.getAttribute('data-block') === 'slider';
    const startX = e.clientX;
    const startY = e.clientY;
    const startWidth = block.getBoundingClientRect().width;
    const maxWidth = editorRoot.clientWidth;
    const startHeight = isSlider
      ? (parseFloat(getComputedStyle(block).getPropertyValue('--slider-item-height')) || block.querySelector('.slider-item').getBoundingClientRect().height)
      : 0;

    function onMove(ev) {
      const newWidth = Math.min(maxWidth, Math.max(120, startWidth + (ev.clientX - startX)));
      block.style.width = newWidth + 'px';

      if (isSlider) {
        const newHeight = Math.min(600, Math.max(80, startHeight + (ev.clientY - startY)));
        block.style.setProperty('--slider-item-height', newHeight + 'px');
        if (block._sliderRecompute) block._sliderRecompute();
      }
    }
    function onUp() {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    }
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  });

  /* ---------- 블록을 직접 드래그해서 본문 안 다른 위치로 재배치 (단일/콜라주/슬라이더 공용) ----------
     별도 손잡이 없이, 블록 위에서 그대로 마우스를 누르고 끌면 옮겨진다.
     네이티브 HTML5 draggable 대신 mousedown/mousemove/mouseup 으로 직접 구현하고,
     mousedown 시점에 preventDefault로 (contenteditable=false 블록이 브라우저 기본 네이티브
     드래그를 암묵적으로 가로채지 않도록) 막아서 커스텀 로직이 항상 이벤트를 받게 한다.
     드롭한 가로 위치(에디터 폭 기준 좌/중간/우)로 정렬(왼쪽/가운데/오른쪽)이 자동으로 정해지고,
     그 정렬만 저장되며(플로트로 텍스트가 자연스럽게 감쌈), 정확한 픽셀 위치 자체는 저장하지 않는다. */

  function alignFromClientX(clientX) {
    const rect = editorRoot.getBoundingClientRect();
    const ratio = (clientX - rect.left) / rect.width;
    if (ratio < 0.33) return 'left';
    if (ratio > 0.67) return 'right';
    return 'center';
  }

  editorRoot.addEventListener('mousedown', function (e) {
    if (e.target.closest('.block-resize-handle') || e.target.closest('.block-remove')) return;
    const block = e.target.closest('[data-block]');
    if (!block) return;

    e.preventDefault();

    const startX = e.clientX;
    const startY = e.clientY;
    let dragging = false;

    function onMove(ev) {
      if (!dragging) {
        if (Math.abs(ev.clientX - startX) < 4 && Math.abs(ev.clientY - startY) < 4) return;
        dragging = true;
        block.classList.add('is-dragging');
      }
    }

    function onUp(ev) {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      block.classList.remove('is-dragging');
      if (!dragging) return; // 이동 없이 그냥 클릭한 경우는 아무 것도 하지 않음

      const range = getRangeFromPointInEditor(ev.clientX, ev.clientY);

      // 드롭 지점이 옮기려는 블록 자기 자신(또는 그 내부)이면 Range.insertNode가
      // "새 자식이 부모를 포함한다"는 HierarchyRequestError를 던진다 - 그냥 제자리에 둔다.
      if (block === range.startContainer || block.contains(range.startContainer)) {
        return;
      }

      try {
        range.insertNode(block);
      } catch (err) {
        console.error('블록 재배치 실패', err);
        return;
      }
      range.setStartAfter(block);
      range.collapse(true);
      restoreRange(range);
      savedRange = range.cloneRange();

      const align = alignFromClientX(ev.clientX);
      block.classList.remove('align-left', 'align-center', 'align-right');
      block.classList.add('align-' + align);
      block.setAttribute('data-align', align);
    }

    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  });

  /* ---------- 캐럿 위치 추적 (툴바 클릭으로 포커스가 빠지기 전까지 계속 갱신) ---------- */

  editorRoot.addEventListener('keyup', saveCurrentRange);
  editorRoot.addEventListener('mouseup', saveCurrentRange);
  editorRoot.addEventListener('input', saveCurrentRange);

  /* ---------- edit.jsp: 서버가 내려준 기존 content/이미지로 초기 DOM 복원 ---------- */

  const TOKEN_PATTERN = /\[\[IMG:(\d+)(?::(left|right))?\]\]|\[\[SLIDER:(\d+(?:,\d+)*)(?::(left|right))?\]\]|\[\[COLLAGE:(\d+-\d+-\d+(?:,\d+-\d+-\d+)*)(?::(left|right))?\]\]/g;

  function appendText(container, str) {
    if (!str) return;
    const lines = str.split('\n');
    lines.forEach(function (line, i) {
      if (i > 0) container.appendChild(document.createElement('br'));
      if (line) container.appendChild(document.createTextNode(line));
    });
  }

  // 기존(이미 저장된) 이미지도 새로 추가한 이미지와 동일하게 삭제/드래그 재배치/리사이즈 가능.
  // 삭제해도 서버의 post_image 행 자체가 지워지는 건 아니고, 그냥 본문 토큰에서 참조가 빠질 뿐이라
  // 별도 삭제 API 없이도 안전하다(참조 안 되는 이미지 행이 남는 것뿐).
  function appendLockedSingle(container, imgUrlFor, n, align) {
    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.className = 'content-img-block align-' + (align || 'center');
    wrapper.setAttribute('data-block', 'single');
    wrapper.setAttribute('data-align', align || 'center');
    wrapper.setAttribute('data-existing-index', String(n));
    const img = document.createElement('img');
    img.src = imgUrlFor(n);
    img.setAttribute('data-existing-index', String(n));
    img.alt = '첨부 이미지';
    img.draggable = false;
    wrapper.appendChild(img);
    appendBlockControls(wrapper);
    container.appendChild(wrapper);
  }

  // entries: [{n, x, y}] — 기존 콜라주. 겹침/자유배치는 그대로 재현하고, 새 콜라주처럼 편집 가능
  function appendLockedCollage(container, imgUrlFor, entries, align) {
    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.className = 'content-collage-block align-' + (align || 'center');
    wrapper.setAttribute('data-block', 'collage');
    wrapper.setAttribute('data-align', align || 'center');

    const canvas = document.createElement('div');
    canvas.className = 'collage-canvas';
    entries.forEach(function (entry) {
      const cell = document.createElement('div');
      cell.className = 'collage-item';
      cell.style.left = entry.x + '%';
      cell.style.top = entry.y + '%';
      const img = document.createElement('img');
      img.src = imgUrlFor(entry.n);
      img.setAttribute('data-existing-index', String(entry.n));
      img.setAttribute('data-x', entry.x);
      img.setAttribute('data-y', entry.y);
      img.alt = '첨부 이미지';
      img.draggable = false;
      cell.appendChild(img);
      canvas.appendChild(cell);
    });
    wrapper.appendChild(canvas);
    appendBlockControls(wrapper);
    container.appendChild(wrapper);
  }

  function appendLockedSlider(container, imgUrlFor, indices, align) {
    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.className = 'content-slider-block align-' + (align || 'center');
    wrapper.setAttribute('data-block', 'slider');
    wrapper.setAttribute('data-align', align || 'center');
    wrapper.innerHTML = sliderMarkup();

    const track = wrapper.querySelector('.slider-track');
    indices.forEach(function (n) {
      const slideItem = document.createElement('div');
      slideItem.className = 'slider-item';
      const img = document.createElement('img');
      img.src = imgUrlFor(n);
      img.setAttribute('data-existing-index', String(n));
      img.alt = '첨부 이미지';
      img.draggable = false;
      slideItem.appendChild(img);
      track.appendChild(slideItem);
    });
    appendBlockControls(wrapper);

    container.appendChild(wrapper);
    initSliderStrip(wrapper);
  }

  function rehydrateFromServer() {
    const dataEl = document.getElementById('postContentData');
    if (!dataEl) return; // write.jsp: 기존 글이 없으므로 아무 것도 하지 않음

    const listEl = document.getElementById('postImageData');
    const imageUrls = listEl
      ? Array.from(listEl.querySelectorAll('li')).map(function (li) { return li.getAttribute('data-url'); })
      : [];
    existingImageCount = imageUrls.length;

    function imgUrlFor(n) {
      const cp = window.CP || '';
      return cp + '/upload/' + imageUrls[n];
    }

    const rawContent = dataEl.textContent;
    let lastIndex = 0;
    let match;
    let hasToken = false;

    while ((match = TOKEN_PATTERN.exec(rawContent)) !== null) {
      hasToken = true;
      appendText(editorRoot, rawContent.slice(lastIndex, match.index));

      if (match[1] !== undefined) {
        appendLockedSingle(editorRoot, imgUrlFor, parseInt(match[1], 10), match[2]);
      } else if (match[3] !== undefined) {
        const indices = match[3].split(',').map(Number);
        appendLockedSlider(editorRoot, imgUrlFor, indices, match[4]);
      } else {
        const entries = match[5].split(',').map(function (s) {
          const parts = s.split('-').map(Number);
          return { n: parts[0], x: parts[1], y: parts[2] };
        });
        appendLockedCollage(editorRoot, imgUrlFor, entries, match[6]);
      }
      lastIndex = TOKEN_PATTERN.lastIndex;
    }
    appendText(editorRoot, rawContent.slice(lastIndex));

    // 토큰이 없는 과거 글: 텍스트 뒤에 기존 이미지를 순서대로(잠금 상태) 붙여서 복원
    if (!hasToken && imageUrls.length > 0) {
      for (let n = 0; n < imageUrls.length; n++) {
        appendLockedSingle(editorRoot, imgUrlFor, n, 'center');
      }
    }
  }

  rehydrateFromServer();

  /* ---------- 제출: DOM을 순서대로 훑어 토큰 텍스트 + 새 이미지 파일 목록을 만든다 ---------- */

  function serializeEditor() {
    let text = '';
    const newFiles = []; // 절대 인덱스 = existingImageCount + newFiles.length (순서 그대로)

    function resolveIndex(el) {
      if (el.hasAttribute('data-existing-index')) {
        return parseInt(el.getAttribute('data-existing-index'), 10);
      }
      const fileId = el.getAttribute('data-file-id');
      const idx = existingImageCount + newFiles.length;
      newFiles.push(fileMap.get(fileId));
      return idx;
    }

    function alignSuffix(node) {
      const align = node.getAttribute('data-align') || 'center';
      return align === 'center' ? '' : (':' + align);
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
        text += '[[IMG:' + n + alignSuffix(node) + ']]\n';
        return;
      }

      if (blockType === 'collage') {
        const imgs = node.querySelectorAll('img[data-file-id], img[data-existing-index]');
        const entries = Array.from(imgs).map(function (img) {
          const n = resolveIndex(img);
          const x = Math.round(parseFloat(img.getAttribute('data-x') || '50'));
          const y = Math.round(parseFloat(img.getAttribute('data-y') || '50'));
          return n + '-' + x + '-' + y;
        });
        text += '[[COLLAGE:' + entries.join(',') + alignSuffix(node) + ']]\n';
        return;
      }

      if (blockType === 'slider') {
        const imgs = node.querySelectorAll('img[data-file-id], img[data-existing-index]');
        const indices = Array.from(imgs).map(resolveIndex);
        text += '[[SLIDER:' + indices.join(',') + alignSuffix(node) + ']]\n';
        return;
      }

      const isBlockLevel = node.tagName === 'DIV' || node.tagName === 'P';
      Array.from(node.childNodes).forEach(walk);
      if (isBlockLevel) text += '\n';
    }

    Array.from(editorRoot.childNodes).forEach(walk);

    return {
      text: text.replace(/\n{3,}/g, '\n\n').trim(),
      newFiles: newFiles
    };
  }

  const form = editorRoot.closest('form');
  if (form) {
    form.addEventListener('submit', function () {
      try {
        const result = serializeEditor();
        contentField.value = result.text;

        const dt = new DataTransfer();
        result.newFiles.forEach(function (file) {
          if (file) dt.items.add(file);
        });
        imagesInput.files = dt.files;
      } catch (err) {
        // 직렬화 중 에러가 나도 최소한 입력한 텍스트라도 저장되게 (이미지 없이라도 게시는 되도록)
        console.error('contentEditor: 본문 직렬화 실패', err);
        if (!contentField.value) {
          contentField.value = (editorRoot.textContent || '').trim() || ' ';
        }
      }
    });
  }
})();
