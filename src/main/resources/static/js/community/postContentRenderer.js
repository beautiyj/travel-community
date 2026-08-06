/* ============================================================
   postContentRenderer.js — 게시글 상세(detail.jsp) 전용
   - post.content(토큰 포함 텍스트) + post.imageList 를 읽어 텍스트/단일 이미지(가운데·좌·우)/
     콜라주(자유 배치, 겹침 허용)/슬라이더(나란히 + 하단 슬라이드바)를 작성 시 순서·위치 그대로 렌더링한다.
   - 텍스트는 createTextNode로만 삽입되므로 사용자가 어떤 문자를 입력했든 HTML로 해석되지 않는다(XSS 안전).
   - 토큰이 하나도 없는(이번 변경 이전에 작성된) 과거 글은 텍스트 뒤에 이미지를 순서대로 쌓아서 폴백한다.
   ============================================================ */
(function () {
  'use strict';

  const TOKEN_PATTERN = /\[\[IMG:(\d+)(?::(left|right))?(?::(\d{1,3}))?\]\]|\[\[SLIDER:(\d+(?:,\d+)*)(?::(left|right))?(?::(\d{1,3}))?\]\]|\[\[COLLAGE:(?:(\d+)-(\d+):)?(\d+-\d+-\d+(?:-\d+)?(?:,\d+-\d+-\d+(?:-\d+)?)*)(?::(left|right))?(?::(\d{1,3}))?\]\]/g;

  const DEFAULT_COLLAGE_ITEM_WIDTH = 42; // % of canvas width, 폭 정보 없는 구버전 토큰의 기본값

  function appendText(root, str) {
    if (!str) return;
    const lines = str.split('\n');
    lines.forEach(function (line, i) {
      if (i > 0) root.appendChild(document.createElement('br'));
      if (line) root.appendChild(document.createTextNode(line));
    });
  }

  function appendSingle(root, imgUrlFor, total, n, align, bw) {
    if (n < 0 || n >= total) return;
    const wrap = document.createElement('div');
    wrap.className = 'content-img-block align-' + (align || 'center');
    if (bw) wrap.style.width = bw + '%';
    const img = document.createElement('img');
    img.src = imgUrlFor(n);
    img.alt = '첨부 이미지';
    wrap.appendChild(img);
    root.appendChild(wrap);
  }

  // entries: [{n, x, y, w}] — 겹침이 있을 수 있는 자유 배치 콜라주를 그대로 재현
  // ratio: {rw, rh} — 삽입 당시 여백을 잘라낸 캔버스 비율(없으면 정사각형 기본값 유지)
  // bw: 작성/수정 시 전체 블록 리사이즈 핸들로 조절한 폭(에디터 폭 대비 %, 없으면 CSS 기본값 사용)
  function appendCollage(root, imgUrlFor, total, entries, align, ratio, bw) {
    const valid = entries.filter(function (e) { return e.n >= 0 && e.n < total; });
    if (!valid.length) return;

    const wrap = document.createElement('div');
    wrap.className = 'content-collage-block align-' + (align || 'center');
    if (bw) wrap.style.width = bw + '%';
    const canvas = document.createElement('div');
    canvas.className = 'collage-canvas';
    if (ratio) canvas.style.aspectRatio = ratio.rw + ' / ' + ratio.rh;
    valid.forEach(function (entry) {
      const cell = document.createElement('div');
      cell.className = 'collage-item';
      cell.style.left = entry.x + '%';
      cell.style.top = entry.y + '%';
      cell.style.width = (entry.w || DEFAULT_COLLAGE_ITEM_WIDTH) + '%';
      const img = document.createElement('img');
      img.src = imgUrlFor(entry.n);
      img.alt = '첨부 이미지';
      cell.appendChild(img);
      canvas.appendChild(cell);
    });
    wrap.appendChild(canvas);
    root.appendChild(wrap);
  }

  // 화살표 없이 사진들을 나란히 배치한 스트립 + 하단 커스텀 슬라이드바로 옆으로 스크롤
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

    thumb.addEventListener('pointerdown', function (e) {
      e.preventDefault();
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
      new ResizeObserver(function () { recompute(); }).observe(viewport);
    }
  }

  function appendSlider(root, imgUrlFor, total, indices, align, bw) {
    const valid = indices.filter(function (n) { return n >= 0 && n < total; });
    if (!valid.length) return;

    const wrap = document.createElement('div');
    wrap.className = 'content-slider-block align-' + (align || 'center');
    if (bw) wrap.style.width = bw + '%';
    wrap.innerHTML =
      '<div class="slider-viewport"><div class="slider-track"></div></div>' +
      '<div class="slider-scrollbar-track"><div class="slider-scrollbar-thumb"></div></div>';

    const track = wrap.querySelector('.slider-track');
    valid.forEach(function (n) {
      const slideItem = document.createElement('div');
      slideItem.className = 'slider-item';
      const img = document.createElement('img');
      img.src = imgUrlFor(n);
      img.alt = '첨부 이미지';
      slideItem.appendChild(img);
      track.appendChild(slideItem);
    });

    root.appendChild(wrap);
    initSliderStrip(wrap);
  }

  function render() {
    const root = document.getElementById('post-content-root');
    const dataEl = document.getElementById('postContentData');
    if (!root || !dataEl) return;

    const listEl = document.getElementById('postImageData');
    const imageUrls = listEl
      ? Array.from(listEl.querySelectorAll('li')).map(function (li) { return li.getAttribute('data-url'); })
      : [];
    const cp = window.CP || '';

    // Cloudinary로 저장된 최신 글은 절대 URL이 그대로 들어있고, 예전에 로컬 디스크에
    // 저장된 글은 파일명만 있어서 /upload/ 접두사를 붙여야 한다. 둘 다 지원한다.
    function imgUrlFor(n) {
      const url = imageUrls[n];
      return /^https?:\/\//.test(url) ? url : cp + '/upload/' + url;
    }

    const rawContent = dataEl.textContent;
    let lastIndex = 0;
    let match;
    let hasToken = false;

    while ((match = TOKEN_PATTERN.exec(rawContent)) !== null) {
      hasToken = true;
      appendText(root, rawContent.slice(lastIndex, match.index));

      if (match[1] !== undefined) {
        appendSingle(root, imgUrlFor, imageUrls.length, parseInt(match[1], 10), match[2], match[3]);
      } else if (match[4] !== undefined) {
        appendSlider(root, imgUrlFor, imageUrls.length, match[4].split(',').map(Number), match[5], match[6]);
      } else {
        const entries = match[9].split(',').map(function (s) {
          const parts = s.split('-').map(Number);
          return { n: parts[0], x: parts[1], y: parts[2], w: parts.length > 3 ? parts[3] : DEFAULT_COLLAGE_ITEM_WIDTH };
        });
        const ratio = (match[7] !== undefined && match[8] !== undefined)
          ? { rw: Number(match[7]), rh: Number(match[8]) }
          : null;
        appendCollage(root, imgUrlFor, imageUrls.length, entries, match[10], ratio, match[11]);
      }
      lastIndex = TOKEN_PATTERN.lastIndex;
    }
    appendText(root, rawContent.slice(lastIndex));

    // 토큰 없는 과거 글: 텍스트 뒤에 이미지를 순서대로 쌓아서 폴백
    if (!hasToken && imageUrls.length > 0) {
      for (let n = 0; n < imageUrls.length; n++) {
        appendSingle(root, imgUrlFor, imageUrls.length, n, 'center');
      }
    }
  }

  document.addEventListener('DOMContentLoaded', render);
})();
