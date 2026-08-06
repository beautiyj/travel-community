// community/contentEditor/slider.js — 슬라이더(나란히 배치 + 하단 슬라이드바) 블록 삽입/서버 데이터 복원과 슬라이더 빌더 모달 담당
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

    thumb.addEventListener('pointerdown', function (e) {
      e.preventDefault();
      e.stopPropagation(); // 블록 재배치용 위임 pointerdown(editorRoot)으로 버블링되어 포인터 캡처를 가로채는 것 방지
      thumb.setPointerCapture(e.pointerId);
      const barRect = barTrack.getBoundingClientRect();

      function onMove(ev) {
        if (ev.pointerId !== e.pointerId) return;
        const ratio = (ev.clientX - barRect.left) / barRect.width;
        setOffset(ratio * maxOffset);
      }
      function onUp(ev) {
        if (ev.pointerId !== e.pointerId) return;
        thumb.releasePointerCapture(e.pointerId);
        thumb.removeEventListener('pointermove', onMove);
        thumb.removeEventListener('pointerup', onUp);
        thumb.removeEventListener('pointercancel', onUp);
      }
      thumb.addEventListener('pointermove', onMove);
      thumb.addEventListener('pointerup', onUp);
      thumb.addEventListener('pointercancel', onUp);
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
    const widths = sizes.map(function (s) { return constants.SLIDER_ITEM_HEIGHT * (s.naturalW / s.naturalH); });
    const rawWidth = widths.reduce(function (sum, w) { return sum + w; }, 0) + constants.SLIDER_GAP * Math.max(0, items.length - 1);
    const scale = rawWidth > constants.COMPOSE_CANVAS_MAX_DIM ? constants.COMPOSE_CANVAS_MAX_DIM / rawWidth : 1;

    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(rawWidth * scale));
    canvas.height = Math.max(1, Math.round(constants.SLIDER_ITEM_HEIGHT * scale));
    const ctx = canvas.getContext('2d');

    let x = 0;
    sizes.forEach(function (s, i) {
      if (!s.img) return;
      const w = widths[i] * scale;
      const h = constants.SLIDER_ITEM_HEIGHT * scale;
      ctx.drawImage(s.img, x, 0, w, h);
      x += w + constants.SLIDER_GAP * scale;
    });

    return canvasUtils.canvasToImageFile(canvas, 'slider');
  }

  // 합성된 슬라이더 이미지 1장(가로로 넓은 스트립)을 슬라이더 블록 구조(뷰포트+스크롤바) 안에
  // 넣어 삽입한다. 이미지가 블록 폭보다 넓으면 initSliderStrip이 기존과 동일하게 하단
  // 슬라이드바를 보여주고 드래그로 옆 스크롤이 되게 한다.
  function insertSliderBlock(file, insertRange) {
    const id = 'f' + (state.fileIdSeq++);
    state.fileMap.set(id, file);
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
    blockChrome.appendBlockControls(wrapper);
    initSliderStrip(wrapper);

    return range.insertBlockAtRange(insertRange, wrapper);
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
    blockChrome.appendBlockControls(wrapper);

    container.appendChild(wrapper);
    initSliderStrip(wrapper);
  }

  /* ---------- "슬라이더" 버튼: 빌더 모달 (목록에서 순서만 조정) ---------- */

  function openSliderBuilder() {
    range.saveCurrentRange();
    state.builderFiles = [];
    renderSliderPreview();
    if (window.openModal) window.openModal('sliderModal');
  }

  dom.toolSliderBtn && dom.toolSliderBtn.addEventListener('click', openSliderBuilder);

  dom.sliderFileInput && dom.sliderFileInput.addEventListener('change', function () {
    const files = Array.from(dom.sliderFileInput.files).filter(function (f) { return f.type.indexOf('image/') === 0; });
    const availableSlots = constants.MAX_BUILDER_ITEMS - state.builderFiles.length;
    files.slice(0, availableSlots).forEach(function (file) {
      const item = { id: 'f' + (state.fileIdSeq++), file: file, url: URL.createObjectURL(file) };
      state.builderFiles.push(item);
      const probe = new Image();
      probe.onload = function () {
        item.naturalWidth = probe.naturalWidth;
        item.naturalHeight = probe.naturalHeight;
        renderSliderPreview();
      };
      probe.src = item.url;
    });
    dom.sliderFileInput.value = '';
    renderSliderPreview();
    if (files.length > availableSlots && window.openModal) window.openModal('sliderLimitModal');
  });

  // 슬라이더 썸네일 박스 크기: 선택한 사진 중 "가장 큰"(면적 기준) 사진의 원본 비율에 맞춰
  // 하나의 크기를 정하고, 모든 썸네일이 그 크기를 공유한다(작거나 비율이 다른 사진은
  // object-fit:contain 으로 잘리지 않고 여백과 함께 표시됨). 아직 크기를 모르는(로딩 전) 사진이
  // 있으면 기본 정사각형으로 대체.
  function computeSliderThumbSize() {
    let ref = null;
    state.builderFiles.forEach(function (item) {
      if (!item.naturalWidth || !item.naturalHeight) return;
      if (!ref || (item.naturalWidth * item.naturalHeight) > (ref.naturalWidth * ref.naturalHeight)) ref = item;
    });
    if (!ref) return { w: constants.SLIDER_THUMB_MAX_SIDE, h: constants.SLIDER_THUMB_MAX_SIDE };
    let ratio = ref.naturalWidth / ref.naturalHeight;
    ratio = Math.min(constants.SLIDER_THUMB_MAX_RATIO, Math.max(constants.SLIDER_THUMB_MIN_RATIO, ratio));
    return ratio >= 1
      ? { w: constants.SLIDER_THUMB_MAX_SIDE, h: Math.round(constants.SLIDER_THUMB_MAX_SIDE / ratio) }
      : { w: Math.round(constants.SLIDER_THUMB_MAX_SIDE * ratio), h: constants.SLIDER_THUMB_MAX_SIDE };
  }

  // 슬라이더: 목록 형태 + ‹/›로 순서(=나열 순서) 조정, ×로 제거
  function renderSliderPreview() {
    if (!dom.sliderPreview) return;
    dom.sliderPreview.innerHTML = '';
    const thumbSize = computeSliderThumbSize();
    state.builderFiles.forEach(function (item, i) {
      const div = document.createElement('div');
      div.className = 'img-block-thumb';
      div.style.width = thumbSize.w + 'px';
      div.style.height = thumbSize.h + 'px';
      div.innerHTML =
        '<img src="' + item.url + '" alt="선택한 이미지">' +
        '<div class="img-block-thumb-order">' +
          '<button type="button" class="img-block-thumb-move" data-i="' + i + '" data-dir="-1" title="앞으로"' + (i === 0 ? ' disabled' : '') + '>‹</button>' +
          '<button type="button" class="img-block-thumb-move" data-i="' + i + '" data-dir="1" title="뒤로"' + (i === state.builderFiles.length - 1 ? ' disabled' : '') + '>›</button>' +
        '</div>' +
        '<button type="button" class="modal-photo-remove" data-i="' + i + '" title="제거">×</button>';
      dom.sliderPreview.appendChild(div);
    });
    if (dom.sliderConfirmBtn) dom.sliderConfirmBtn.disabled = state.builderFiles.length < 2;
  }

  dom.sliderPreview && dom.sliderPreview.addEventListener('click', function (e) {
    const moveBtn = e.target.closest('.img-block-thumb-move');
    if (moveBtn) {
      const i = Number(moveBtn.getAttribute('data-i'));
      const j = i + Number(moveBtn.getAttribute('data-dir'));
      if (j < 0 || j >= state.builderFiles.length) return;
      const tmp = state.builderFiles[i];
      state.builderFiles[i] = state.builderFiles[j];
      state.builderFiles[j] = tmp;
      renderSliderPreview();
      return;
    }

    const removeBtn = e.target.closest('.modal-photo-remove');
    if (removeBtn) {
      state.builderFiles.splice(Number(removeBtn.getAttribute('data-i')), 1);
      renderSliderPreview();
    }
  });

  // 완료 시 미리보기 순서 그대로 하나의 가로 스트립 이미지로 합성한 뒤 현재 캐럿 위치에 삽입한다.
  dom.sliderConfirmBtn && dom.sliderConfirmBtn.addEventListener('click', function () {
    if (state.builderFiles.length < 2 || dom.sliderConfirmBtn.disabled) return;
    const confirmRange = state.savedRange ? state.savedRange.cloneRange() : range.endOfEditorRange();
    range.restoreRange(confirmRange);
    const thumbEls = Array.from(dom.sliderPreview.querySelectorAll('.img-block-thumb'));

    dom.sliderConfirmBtn.disabled = true;
    composeSliderImage(state.builderFiles, thumbEls).then(function (file) {
      range.restoreRange(confirmRange);
      insertSliderBlock(file, confirmRange);
      if (window.closeModal) window.closeModal('sliderModal');
      state.builderFiles = [];
    }).catch(function (err) {
      console.error('슬라이더 합성 실패', err);
    }).then(function () {
      dom.sliderConfirmBtn.disabled = state.builderFiles.length < 2;
    });
  });

  CE.slider = {
    appendLockedSlider: appendLockedSlider
  };
})();
