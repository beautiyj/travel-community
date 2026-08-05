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

  const collageFileInput = document.getElementById('collageFileInput');
  const collageCanvas = document.getElementById('collageCanvas');
  const collageConfirmBtn = document.getElementById('collageConfirmBtn');
  const sliderFileInput = document.getElementById('sliderFileInput');
  const sliderPreview = document.getElementById('sliderPreview');
  const sliderConfirmBtn = document.getElementById('sliderConfirmBtn');

  const fileMap = new Map();   // data-file-id -> File (새로 추가된 이미지만)
  let fileIdSeq = 0;
  let existingImageCount = 0;  // edit.jsp: 이미 저장된(잠금) 이미지 개수, write.jsp: 0
  let savedRange = null;       // 툴바 클릭 직전까지의 캐럿 위치
  let builderFiles = [];       // [{id, file, url, x?, y?}] — 빌더 모달에서 고른 이미지들
  let pendingTextOverflow = false; // contentLimitModal 확인 클릭 시 텍스트를 잘라낼지 여부

  const MAX_BUILDER_ITEMS = 4;        // 콜라주/슬라이더 빌더 각각의 최대 선택 장수
  const MAX_TOTAL_IMAGE_BLOCKS = 50;  // 본문 전체 이미지 블록(단일/콜라주/슬라이더 각 1개) 최대 개수
  const MAX_CONTENT_TEXT_LENGTH = 9999; // 본문에 실제로 타이핑한 텍스트 최대 길이

  const COMPOSE_CANVAS_MAX_DIM = 1600; // 콜라주/슬라이더를 합성한 결과 이미지의 긴 변 상한(px)
  const SLIDER_ITEM_HEIGHT = 280;      // .slider-item 기본 높이(community.css)와 동일
  const SLIDER_GAP = 8;                // .slider-track gap(community.css)과 동일

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

    // 블록 바로 뒤에 텍스트 노드가 없으면(빈 에디터에 처음 삽입했거나, 블록을 연달아
    // 삽입한 경우) contenteditable=false 블록 바로 뒤 컨테이너 경계에는 캐럿이 제대로
    // 앵커링되지 않아 이어서 타이핑해도 글자가 씹히므로, 빈 텍스트 노드를 하나 만들어
    // 그 안에 캐럿을 둔다. 이미 텍스트가 있던 자리에 삽입한 경우엔 insertNode가 그
    // 텍스트 노드를 분할해 남긴 노드를 그대로 재사용한다.
    let caretNode = blockEl.nextSibling;
    if (!caretNode || caretNode.nodeType !== Node.TEXT_NODE) {
      caretNode = document.createTextNode('');
      blockEl.parentNode.insertBefore(caretNode, blockEl.nextSibling);
    }
    range.setStart(caretNode, 0);
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

  const DEFAULT_COLLAGE_ITEM_WIDTH = 42; // % of canvas width, 구버전(폭 정보 없는) 토큰의 기본값과도 동일

  // 콜라주 아이템(캔버스 안에 배치된 사진 한 장)의 폭을 드래그로 조절하는 핸들.
  // 높이는 CSS(.collage-item img { height:auto }) 덕분에 원본 비율 그대로 폭을 따라간다.
  // stopPropagation으로 에디터 루트에 달린 블록 전체 드래그-재배치/리사이즈 리스너로
  // 이벤트가 새지 않게 막는다.
  function appendCollageItemResizeHandle(cell, canvasEl) {
    const handle = document.createElement('span');
    handle.className = 'collage-item-resize-handle';
    handle.title = '드래그해서 크기 조절';
    cell.appendChild(handle);

    handle.addEventListener('mousedown', function (e) {
      e.preventDefault();
      e.stopPropagation();

      const img = cell.querySelector('img');
      const canvasRect = canvasEl.getBoundingClientRect();
      const startWidthPercent = parseFloat(cell.style.width) || DEFAULT_COLLAGE_ITEM_WIDTH;
      const startX = e.clientX;

      function onMove(ev) {
        const deltaPercent = ((ev.clientX - startX) / canvasRect.width) * 100;
        const w = Math.min(80, Math.max(15, startWidthPercent + deltaPercent));
        cell.style.width = w + '%';
        if (img) img.setAttribute('data-w', Math.round(w));
      }
      function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }

  // 빌더 캔버스(정사각형)에 배치된 사진들이 실제로 차지하는 영역(바운딩 박스)만 남기고
  // 나머지 여백은 잘라낸다. cellEls는 items와 같은 순서로 렌더링된 .collage-builder-item
  // 요소들이며, 이미 화면에 표시되어 있어 img.naturalWidth/Height를 바로 읽을 수 있다.
  // 반환하는 items의 x/y/w는 그 바운딩 박스를 100%로 삼은 새 좌표계 값이고,
  // ratio는 그 박스의 가로:세로 비율(캔버스를 더 이상 정사각형으로 그리지 않기 위함)이다.
  function cropCollageToContent(items, cellEls) {
    let left = Infinity, top = Infinity, right = -Infinity, bottom = -Infinity;
    items.forEach(function (item, i) {
      const img = cellEls[i].querySelector('img');
      const naturalW = (img && img.naturalWidth) || 1;
      const naturalH = (img && img.naturalHeight) || 1;
      const h = item.w * (naturalH / naturalW);
      left = Math.min(left, item.x - item.w / 2);
      right = Math.max(right, item.x + item.w / 2);
      top = Math.min(top, item.y - h / 2);
      bottom = Math.max(bottom, item.y + h / 2);
    });
    const cropW = Math.max(1, right - left);
    const cropH = Math.max(1, bottom - top);

    const normalized = items.map(function (item) {
      return {
        id: item.id,
        file: item.file,
        url: item.url,
        x: (item.x - left) / cropW * 100,
        y: (item.y - top) / cropH * 100,
        w: item.w / cropW * 100
      };
    });

    return { items: normalized, ratio: { rw: Math.max(1, Math.round(cropW)), rh: Math.max(1, Math.round(cropH)) } };
  }

  // 캔버스에 그려 넣은 결과를 하나의 이미지 File로 만든다(콜라주/슬라이더 공용).
  // 콜라주는 겹치지 않은 빈 자리가 반드시 생기므로 투명도가 있는 PNG로 저장해서
  // .collage-canvas의 배경(var(--muted))이 그 틈으로 비쳐 보이게 한다. JPEG로 저장하면
  // 알파 채널이 없어 투명 영역이 검은색으로 구워진다. 슬라이더는 사진끼리 빈틈없이 이어
  // 그리므로 투명 이슈가 없어 용량이 작은 JPEG를 그대로 쓴다.
  function canvasToImageFile(canvas, namePrefix, mimeType) {
    mimeType = mimeType || 'image/jpeg';
    const ext = mimeType === 'image/png' ? 'png' : 'jpg';
    const quality = mimeType === 'image/png' ? undefined : 0.9;
    return new Promise(function (resolve, reject) {
      canvas.toBlob(function (blob) {
        if (!blob) { reject(new Error(namePrefix + ' 합성 실패')); return; }
        resolve(new File([blob], namePrefix + '_' + Date.now() + '.' + ext, { type: mimeType }));
      }, mimeType, quality);
    });
  }

  // 콜라주 빌더에 배치된 사진들을 items(x/y/w, % 단위, cropCollageToContent가 계산한 좌표계)
  // 그대로 캔버스에 합성해 하나의 이미지 File로 만든다. cellEls[i]는 items[i]와 같은 순서로
  // 빌더 캔버스에 렌더링된 .collage-builder-item 이고, 이미 화면에 표시되어 있어 그 안의
  // <img>를 그대로 drawImage 소스로 쓸 수 있다(원본 File을 다시 읽어 로드를 기다릴 필요 없음).
  function composeCollageImage(items, cellEls, ratio) {
    const wide = ratio.rw >= ratio.rh;
    const canvasW = wide ? COMPOSE_CANVAS_MAX_DIM : Math.round(COMPOSE_CANVAS_MAX_DIM * ratio.rw / ratio.rh);
    const canvasH = wide ? Math.round(COMPOSE_CANVAS_MAX_DIM * ratio.rh / ratio.rw) : COMPOSE_CANVAS_MAX_DIM;

    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, canvasW);
    canvas.height = Math.max(1, canvasH);
    const ctx = canvas.getContext('2d');

    items.forEach(function (item, i) {
      const img = cellEls[i] && cellEls[i].querySelector('img');
      if (!img) return;
      const naturalW = img.naturalWidth || 1;
      const naturalH = img.naturalHeight || 1;
      const drawW = (item.w / 100) * canvasW;
      const drawH = drawW * (naturalH / naturalW);
      const cx = (item.x / 100) * canvasW;
      const cy = (item.y / 100) * canvasH;
      ctx.drawImage(img, cx - drawW / 2, cy - drawH / 2, drawW, drawH);
    });

    return canvasToImageFile(canvas, 'collage', 'image/png');
  }

  /* ---------- 슬라이더 빌더에 배치된 사진들을 하나의 이미지로 합성 ---------- */

  // thumbEls는 items와 같은 순서로 슬라이더 빌더 미리보기(renderSliderPreview)에 렌더링된
  // .img-block-thumb 이고, 콜라주와 마찬가지로 이미 화면에 로드되어 있는 <img>를 그대로 쓴다.
  // 모든 이미지를 SLIDER_ITEM_HEIGHT 높이에 맞춰(원본 비율 유지) 좌→우로 나란히 그린 뒤,
  // 전체 폭이 COMPOSE_CANVAS_MAX_DIM을 넘으면 비율대로 축소한다.
  function composeSliderImage(items, thumbEls) {
    const sizes = items.map(function (item, i) {
      const img = thumbEls[i] && thumbEls[i].querySelector('img');
      return { img: img, naturalW: (img && img.naturalWidth) || 1, naturalH: (img && img.naturalHeight) || 1 };
    });
    const widths = sizes.map(function (s) { return SLIDER_ITEM_HEIGHT * (s.naturalW / s.naturalH); });
    const rawWidth = widths.reduce(function (sum, w) { return sum + w; }, 0) + SLIDER_GAP * Math.max(0, items.length - 1);
    const scale = rawWidth > COMPOSE_CANVAS_MAX_DIM ? COMPOSE_CANVAS_MAX_DIM / rawWidth : 1;

    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(rawWidth * scale));
    canvas.height = Math.max(1, Math.round(SLIDER_ITEM_HEIGHT * scale));
    const ctx = canvas.getContext('2d');

    let x = 0;
    sizes.forEach(function (s, i) {
      if (!s.img) return;
      const w = widths[i] * scale;
      const h = SLIDER_ITEM_HEIGHT * scale;
      ctx.drawImage(s.img, x, 0, w, h);
      x += w + SLIDER_GAP * scale;
    });

    return canvasToImageFile(canvas, 'slider');
  }

  // 합성된 콜라주 이미지 1장을 콜라주 블록 구조(.collage-canvas 배경 + 100% 채운 .collage-item)
  // 안에 넣어 삽입한다. 합성 캔버스가 ratio(rw:rh)와 정확히 같은 비율로 그려지므로 이미지가
  // .collage-canvas를 꽉 채우고, PNG 투명 영역으로 .collage-canvas 배경(var(--muted))이 비쳐
  // 보인다(appendLockedCollage/appendCollage가 이미 항목 1개짜리 콜라주도 그대로 지원함).
  function insertCollageBlock(file, range, ratio) {
    const id = 'f' + (fileIdSeq++);
    fileMap.set(id, file);
    const url = URL.createObjectURL(file);

    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.draggable = false;
    wrapper.className = 'content-collage-block align-center';
    wrapper.setAttribute('data-block', 'collage');
    wrapper.setAttribute('data-align', 'center');
    wrapper.setAttribute('data-canvas-rw', ratio.rw);
    wrapper.setAttribute('data-canvas-rh', ratio.rh);

    const canvas = document.createElement('div');
    canvas.className = 'collage-canvas';
    canvas.style.aspectRatio = ratio.rw + ' / ' + ratio.rh;
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
    appendBlockControls(wrapper);

    return insertBlockAtRange(range, wrapper);
  }

  // 합성된 슬라이더 이미지 1장(가로로 넓은 스트립)을 슬라이더 블록 구조(뷰포트+스크롤바) 안에
  // 넣어 삽입한다. 이미지가 블록 폭보다 넓으면 initSliderStrip이 기존과 동일하게 하단
  // 슬라이드바를 보여주고 드래그로 옆 스크롤이 되게 한다.
  function insertSliderBlock(file, range) {
    const id = 'f' + (fileIdSeq++);
    fileMap.set(id, file);
    const url = URL.createObjectURL(file);

    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.draggable = false;
    wrapper.className = 'content-slider-block align-center';
    wrapper.setAttribute('data-block', 'slider');
    wrapper.setAttribute('data-align', 'center');
    wrapper.innerHTML = sliderMarkup();

    const track = wrapper.querySelector('.slider-track');
    const slideItem = document.createElement('div');
    slideItem.className = 'slider-item';
    const img = document.createElement('img');
    img.src = url;
    img.setAttribute('data-file-id', id);
    img.alt = '첨부 이미지';
    img.draggable = false;
    slideItem.appendChild(img);
    track.appendChild(slideItem);
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
    return { x: p[0], y: p[1], w: 38 };
  }

  function openCollageBuilder() {
    saveCurrentRange();
    builderFiles = [];
    renderCollageCanvas();
    if (window.openModal) window.openModal('collageModal');
  }

  function openSliderBuilder() {
    saveCurrentRange();
    builderFiles = [];
    renderSliderPreview();
    if (window.openModal) window.openModal('sliderModal');
  }

  toolCollageBtn && toolCollageBtn.addEventListener('click', openCollageBuilder);
  toolSliderBtn && toolSliderBtn.addEventListener('click', openSliderBuilder);

  collageFileInput && collageFileInput.addEventListener('change', function () {
    const files = Array.from(collageFileInput.files).filter(function (f) { return f.type.indexOf('image/') === 0; });
    const availableSlots = MAX_BUILDER_ITEMS - builderFiles.length;
    files.slice(0, availableSlots).forEach(function (file) {
      const item = { id: 'f' + (fileIdSeq++), file: file, url: URL.createObjectURL(file) };
      const pos = defaultCollagePosition(builderFiles.length);
      item.x = pos.x;
      item.y = pos.y;
      item.w = pos.w;
      builderFiles.push(item);
    });
    collageFileInput.value = '';
    renderCollageCanvas();
    if (files.length > availableSlots && window.openModal) window.openModal('collageLimitModal');
  });

  sliderFileInput && sliderFileInput.addEventListener('change', function () {
    const files = Array.from(sliderFileInput.files).filter(function (f) { return f.type.indexOf('image/') === 0; });
    const availableSlots = MAX_BUILDER_ITEMS - builderFiles.length;
    files.slice(0, availableSlots).forEach(function (file) {
      const item = { id: 'f' + (fileIdSeq++), file: file, url: URL.createObjectURL(file) };
      builderFiles.push(item);
      const probe = new Image();
      probe.onload = function () {
        item.naturalWidth = probe.naturalWidth;
        item.naturalHeight = probe.naturalHeight;
        renderSliderPreview();
      };
      probe.src = item.url;
    });
    sliderFileInput.value = '';
    renderSliderPreview();
    if (files.length > availableSlots && window.openModal) window.openModal('sliderLimitModal');
  });

  // 콜라주: 고정 캔버스 위에 사진을 자유롭게 드래그로 배치(겹침 허용), ×로 제거
  function renderCollageCanvas() {
    if (!collageCanvas) return;
    collageCanvas.innerHTML = '';
    builderFiles.forEach(function (item, i) {
      const w = item.w || DEFAULT_COLLAGE_ITEM_WIDTH;
      const el = document.createElement('div');
      el.className = 'collage-builder-item';
      el.style.left = item.x + '%';
      el.style.top = item.y + '%';
      el.style.width = w + '%';
      el.innerHTML =
        '<img src="' + item.url + '" alt="선택한 이미지" draggable="false">' +
        '<button type="button" class="collage-builder-item-remove" data-i="' + i + '">×</button>' +
        '<span class="collage-builder-item-resize-handle" title="드래그해서 크기 조절"></span>';
      collageCanvas.appendChild(el);

      const resizeHandle = el.querySelector('.collage-builder-item-resize-handle');
      resizeHandle.addEventListener('mousedown', function (e) {
        e.preventDefault();
        e.stopPropagation();
        const canvasRect = collageCanvas.getBoundingClientRect();
        const startWidthPercent = item.w || DEFAULT_COLLAGE_ITEM_WIDTH;
        const startX = e.clientX;

        function onMove(ev) {
          const deltaPercent = ((ev.clientX - startX) / canvasRect.width) * 100;
          const newW = Math.min(80, Math.max(15, startWidthPercent + deltaPercent));
          item.w = newW;
          el.style.width = newW + '%';
        }
        function onUp() {
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
        }
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      });

      el.addEventListener('mousedown', function (e) {
        if (e.target.closest('.collage-builder-item-remove') || e.target.closest('.collage-builder-item-resize-handle')) return;
        e.preventDefault();
        const canvasRect = collageCanvas.getBoundingClientRect();

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
    if (collageConfirmBtn) collageConfirmBtn.disabled = builderFiles.length < 2;
  }

  collageCanvas && collageCanvas.addEventListener('click', function (e) {
    const btn = e.target.closest('.collage-builder-item-remove');
    if (!btn) return;
    builderFiles.splice(Number(btn.getAttribute('data-i')), 1);
    renderCollageCanvas();
  });

  // 슬라이더 썸네일 박스 크기: 선택한 사진 중 "가장 큰"(면적 기준) 사진의 원본 비율에 맞춰
  // 하나의 크기를 정하고, 모든 썸네일이 그 크기를 공유한다(작거나 비율이 다른 사진은
  // object-fit:contain 으로 잘리지 않고 여백과 함께 표시됨). 아직 크기를 모르는(로딩 전) 사진이
  // 있으면 기본 정사각형으로 대체.
  const SLIDER_THUMB_MAX_SIDE = 150;
  const SLIDER_THUMB_MIN_RATIO = 0.5;
  const SLIDER_THUMB_MAX_RATIO = 2;

  function computeSliderThumbSize() {
    let ref = null;
    builderFiles.forEach(function (item) {
      if (!item.naturalWidth || !item.naturalHeight) return;
      if (!ref || (item.naturalWidth * item.naturalHeight) > (ref.naturalWidth * ref.naturalHeight)) ref = item;
    });
    if (!ref) return { w: SLIDER_THUMB_MAX_SIDE, h: SLIDER_THUMB_MAX_SIDE };
    let ratio = ref.naturalWidth / ref.naturalHeight;
    ratio = Math.min(SLIDER_THUMB_MAX_RATIO, Math.max(SLIDER_THUMB_MIN_RATIO, ratio));
    return ratio >= 1
      ? { w: SLIDER_THUMB_MAX_SIDE, h: Math.round(SLIDER_THUMB_MAX_SIDE / ratio) }
      : { w: Math.round(SLIDER_THUMB_MAX_SIDE * ratio), h: SLIDER_THUMB_MAX_SIDE };
  }

  // 슬라이더: 목록 형태 + ‹/›로 순서(=나열 순서) 조정, ×로 제거
  function renderSliderPreview() {
    if (!sliderPreview) return;
    sliderPreview.innerHTML = '';
    const thumbSize = computeSliderThumbSize();
    builderFiles.forEach(function (item, i) {
      const div = document.createElement('div');
      div.className = 'img-block-thumb';
      div.style.width = thumbSize.w + 'px';
      div.style.height = thumbSize.h + 'px';
      div.innerHTML =
        '<img src="' + item.url + '" alt="선택한 이미지">' +
        '<div class="img-block-thumb-order">' +
          '<button type="button" class="img-block-thumb-move" data-i="' + i + '" data-dir="-1" title="앞으로"' + (i === 0 ? ' disabled' : '') + '>‹</button>' +
          '<button type="button" class="img-block-thumb-move" data-i="' + i + '" data-dir="1" title="뒤로"' + (i === builderFiles.length - 1 ? ' disabled' : '') + '>›</button>' +
        '</div>' +
        '<button type="button" class="img-block-thumb-remove" data-i="' + i + '" title="제거">×</button>';
      sliderPreview.appendChild(div);
    });
    if (sliderConfirmBtn) sliderConfirmBtn.disabled = builderFiles.length < 2;
  }

  sliderPreview && sliderPreview.addEventListener('click', function (e) {
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

  // 완료 시 빌더의 배치를 그대로 캔버스에 합성해 하나의 이미지로 만든 뒤,
  // 일반 사진 한 장과 동일하게(insertSingleImage) 삽입한다.
  collageConfirmBtn && collageConfirmBtn.addEventListener('click', function () {
    if (builderFiles.length < 2 || collageConfirmBtn.disabled) return;
    const range = savedRange ? savedRange.cloneRange() : endOfEditorRange();
    restoreRange(range);
    const cellEls = Array.from(collageCanvas.querySelectorAll('.collage-builder-item'));
    const cropped = cropCollageToContent(builderFiles, cellEls);

    collageConfirmBtn.disabled = true;
    composeCollageImage(cropped.items, cellEls, cropped.ratio).then(function (file) {
      restoreRange(range);
      insertCollageBlock(file, range, cropped.ratio);
      if (window.closeModal) window.closeModal('collageModal');
      builderFiles = [];
    }).catch(function (err) {
      console.error('콜라주 합성 실패', err);
    }).then(function () {
      collageConfirmBtn.disabled = builderFiles.length < 2;
    });
  });

  sliderConfirmBtn && sliderConfirmBtn.addEventListener('click', function () {
    if (builderFiles.length < 2 || sliderConfirmBtn.disabled) return;
    const range = savedRange ? savedRange.cloneRange() : endOfEditorRange();
    restoreRange(range);
    const thumbEls = Array.from(sliderPreview.querySelectorAll('.img-block-thumb'));

    sliderConfirmBtn.disabled = true;
    composeSliderImage(builderFiles, thumbEls).then(function (file) {
      restoreRange(range);
      insertSliderBlock(file, range);
      if (window.closeModal) window.closeModal('sliderModal');
      builderFiles = [];
    }).catch(function (err) {
      console.error('슬라이더 합성 실패', err);
    }).then(function () {
      sliderConfirmBtn.disabled = builderFiles.length < 2;
    });
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
      block.setAttribute('data-block-width', Math.round(newWidth / maxWidth * 100));

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
     네이티브 HTML5 draggable 대신 mousedown/mousemove/mouseup 으로 직접 구현한다.
     draggable=false가 이미 모든 블록에 설정돼 있어 네이티브 드래그는 원래 안 걸리므로,
     mousedown 시점엔 preventDefault를 하지 않고 브라우저의 기본 캐럿 배치를 그대로 둔다
     (막아버리면 블록이 에디터 전체를 채울 때 클릭해도 캐럿을 못 두는 문제가 생김).
     대신 실제로 드래그가 확정된 뒤부터만 selectstart를 막아 네이티브 텍스트 선택만 억제한다.
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

    // 여기서 바로 preventDefault를 하면(드래그 여부와 무관하게) 브라우저의 기본 동작인
    // "클릭 지점에 캐럿 배치"까지 막혀버린다. 본문에 텍스트 없이 이미지 블록만 있어서
    // 블록이 에디터 전체를 채우는 경우, 사용자가 커서를 두려고 아무 데나 클릭해도 전부
    // 블록 위 클릭이 되어 캐럿이 아예 안 놓이고 타이핑이 씹히는 버그로 이어졌다.
    // 그래서 실제로 드래그가 확정된 시점(4px 이상 이동)부터만 selectstart를 막아
    // 커스텀 드래그 중 네이티브 텍스트 선택만 억제하고, 그냥 클릭은 브라우저가 정상적으로
    // 캐럿을 배치하도록 둔다. (draggable=false라 네이티브 HTML5 드래그는 원래 안 걸림)
    const startX = e.clientX;
    const startY = e.clientY;
    let dragging = false;

    function onSelectStart(ev) {
      if (dragging) ev.preventDefault();
    }

    function onMove(ev) {
      if (!dragging) {
        if (Math.abs(ev.clientX - startX) < 4 && Math.abs(ev.clientY - startY) < 4) return;
        dragging = true;
        block.classList.add('is-dragging');
        document.addEventListener('selectstart', onSelectStart);
      }
    }

    function onUp(ev) {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      document.removeEventListener('selectstart', onSelectStart);
      block.classList.remove('is-dragging');
      if (!dragging) return; // 이동 없이 그냥 클릭한 경우 - 브라우저가 이미 캐럿을 배치했으므로 그대로 둠

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

  const TOKEN_PATTERN = /\[\[IMG:(\d+)(?::(left|right))?(?::(\d{1,3}))?\]\]|\[\[SLIDER:(\d+(?:,\d+)*)(?::(left|right))?(?::(\d{1,3}))?\]\]|\[\[COLLAGE:(?:(\d+)-(\d+):)?(\d+-\d+-\d+(?:-\d+)?(?:,\d+-\d+-\d+(?:-\d+)?)*)(?::(left|right))?(?::(\d{1,3}))?\]\]/g;

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
    appendBlockControls(wrapper);
    container.appendChild(wrapper);
  }

  // entries: [{n, x, y, w}] — 기존 콜라주. 겹침/자유배치는 그대로 재현하고, 새 콜라주처럼 편집 가능
  // ratio: {rw, rh} — 삽입 당시 여백을 잘라낸 캔버스 비율(없으면 정사각형 기본값 유지)
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
      const w = entry.w || DEFAULT_COLLAGE_ITEM_WIDTH;
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
      appendCollageItemResizeHandle(cell, canvas);
      canvas.appendChild(cell);
    });
    wrapper.appendChild(canvas);
    appendBlockControls(wrapper);
    container.appendChild(wrapper);
  }

  function appendLockedSlider(container, imgUrlFor, indices, align, bw) {
    const wrapper = document.createElement('div');
    wrapper.contentEditable = 'false';
    wrapper.className = 'content-slider-block align-' + (align || 'center');
    wrapper.setAttribute('data-block', 'slider');
    wrapper.setAttribute('data-align', align || 'center');
    if (bw) {
      wrapper.style.width = bw + '%';
      wrapper.setAttribute('data-block-width', bw);
    }
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
        appendLockedSingle(editorRoot, imgUrlFor, parseInt(match[1], 10), match[2], match[3]);
      } else if (match[4] !== undefined) {
        const indices = match[4].split(',').map(Number);
        appendLockedSlider(editorRoot, imgUrlFor, indices, match[5], match[6]);
      } else {
        const entries = match[9].split(',').map(function (s) {
          const parts = s.split('-').map(Number);
          return { n: parts[0], x: parts[1], y: parts[2], w: parts.length > 3 ? parts[3] : DEFAULT_COLLAGE_ITEM_WIDTH };
        });
        const ratio = (match[7] !== undefined && match[8] !== undefined)
          ? { rw: Number(match[7]), rh: Number(match[8]) }
          : null;
        appendLockedCollage(editorRoot, imgUrlFor, entries, match[10], ratio, match[11]);
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

    // 본문이 이미지로 끝나는 글(뒤에 텍스트가 없는 경우)은 appendChild로만 쌓아올린 터라
    // 마지막 블록 뒤에 캐럿을 앵커링할 텍스트 노드가 없다 - insertBlockAtRange와 동일한 이유로
    // 이어서 타이핑이 안 되므로, 마지막 자식이 이미지 블록이면 빈 텍스트 노드를 붙여둔다.
    const lastChild = editorRoot.lastChild;
    if (lastChild && lastChild.nodeType === Node.ELEMENT_NODE && lastChild.getAttribute('data-block')) {
      editorRoot.appendChild(document.createTextNode(''));
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
          const w = Math.round(parseFloat(img.getAttribute('data-w') || String(DEFAULT_COLLAGE_ITEM_WIDTH)));
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

    Array.from(editorRoot.childNodes).forEach(walk);

    return {
      text: text.replace(/\n{3,}/g, '\n\n').trim(),
      newFiles: newFiles
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
    })(editorRoot);

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
    })(editorRoot);
  }

  const contentLimitModal = document.getElementById('contentLimitModal');
  if (contentLimitModal) {
    const confirmBtn = contentLimitModal.querySelector('.modal-btn');
    confirmBtn && confirmBtn.addEventListener('click', function () {
      if (pendingTextOverflow) {
        truncateEditorTrailingText(MAX_CONTENT_TEXT_LENGTH);
        pendingTextOverflow = false;
      }
    });
  }

  const form = editorRoot.closest('form');
  if (form) {
    form.addEventListener('submit', function (e) {
      const blockCount = editorRoot.querySelectorAll('[data-block]').length;
      const textLength = (editorRoot.textContent || '').length;
      const imageOver = blockCount > MAX_TOTAL_IMAGE_BLOCKS;
      const textOver = textLength > MAX_CONTENT_TEXT_LENGTH;

      if (imageOver || textOver) {
        e.preventDefault();
        const lines = [];
        if (imageOver) lines.push('본문에는 사진을 최대 ' + MAX_TOTAL_IMAGE_BLOCKS + '장까지 추가할 수 있습니다. (콜라주, 슬라이더는 각각 1장으로 계산됩니다)');
        if (textOver) lines.push('본문은 ' + MAX_CONTENT_TEXT_LENGTH + '자 이내로 작성해주세요. 확인을 누르면 초과된 내용이 자동으로 삭제됩니다.');
        pendingTextOverflow = textOver;
        const messageEl = contentLimitModal && contentLimitModal.querySelector('.modal-message');
        if (messageEl) messageEl.innerHTML = lines.join('<br>');
        if (window.openModal) window.openModal('contentLimitModal');
        return;
      }

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
