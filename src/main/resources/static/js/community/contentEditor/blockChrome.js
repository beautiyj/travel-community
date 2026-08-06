// community/contentEditor/blockChrome.js — 이미지 블록 공용 동작: 삭제(×) 버튼, 삭제 이벤트 위임,
// 드래그로 본문 안 다른 위치에 재배치, 캐럿 위치 추적(단일/콜라주/슬라이더 블록 공용)
(function () {
  'use strict';
  const CE = window.ContentEditor;
  const dom = CE.dom;
  const state = CE.state;
  const range = CE.range;
  if (!dom.editorRoot) return;

  // 콜라주/슬라이더/단일 블록 공용: 삭제(×)
  function appendBlockControls(wrapper) {
    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.className = 'block-remove';
    removeBtn.title = '삭제';
    removeBtn.textContent = '🗑 삭제';
    wrapper.appendChild(removeBtn);
  }

  /* ---------- 블록 내부 컨트롤: 삭제 (이벤트 위임) ---------- */

  dom.editorRoot.addEventListener('click', function (e) {
    const removeBtn = e.target.closest('.block-remove');
    if (removeBtn) {
      const block = removeBtn.closest('[data-block]');
      if (block) {
        block.querySelectorAll('[data-file-id]').forEach(function (el) {
          state.fileMap.delete(el.getAttribute('data-file-id'));
        });
        block.remove();
      }
    }
  });

  /* ---------- 블록을 직접 드래그해서 본문 안 다른 위치로 재배치 (단일/콜라주/슬라이더 공용) ----------
     별도 손잡이 없이, 블록 위에서 그대로 마우스를 누르고 끌면 옮겨진다.
     네이티브 HTML5 draggable 대신 mousedown/mousemove/mouseup 으로 직접 구현한다.
     draggable=false가 이미 모든 블록에 설정돼 있어 네이티브 드래그는 원래 안 걸리므로,
     mousedown 시점엔 preventDefault를 하지 않고 브라우저의 기본 캐럿 배치를 그대로 둔다
     (막아버리면 블록이 에디터 전체를 채울 때 클릭해도 캐럿을 못 두는 문제가 생김).
     대신 실제로 드래그가 확정된 뒤부터만 selectstart를 막아 네이티브 텍스트 선택만 억제한다.
     정렬은 항상 가운데(align-center) 고정이라 텍스트 옆에 배치되지 않으며, 드롭 위치는
     본문 안 순서만 바꿀 뿐 좌우 정렬에는 영향을 주지 않는다. */

  dom.editorRoot.addEventListener('pointerdown', function (e) {
    if (e.target.closest('.block-remove')) return;
    const block = e.target.closest('[data-block]');
    if (!block) return;

    // 여기서 바로 preventDefault를 하면(드래그 여부와 무관하게) 브라우저의 기본 동작인
    // "클릭 지점에 캐럿 배치"까지 막혀버린다. 본문에 텍스트 없이 이미지 블록만 있어서
    // 블록이 에디터 전체를 채우는 경우, 사용자가 커서를 두려고 아무 데나 클릭해도 전부
    // 블록 위 클릭이 되어 캐럿이 아예 안 놓이고 타이핑이 씹히는 버그로 이어졌다.
    // 그래서 실제로 드래그가 확정된 시점(4px 이상 이동)부터만 selectstart를 막아
    // 커스텀 드래그 중 네이티브 텍스트 선택만 억제하고, 그냥 클릭은 브라우저가 정상적으로
    // 캐럿을 배치하도록 둔다. (draggable=false라 네이티브 HTML5 드래그는 원래 안 걸림)
    // Pointer Capture를 block에 걸어두면 커서가 창 밖으로 나가도 pointermove/pointerup이
    // 계속 block으로 전달되고 pointerup/pointercancel이 반드시 발생하므로, 창 밖에서 마우스
    // 버튼을 떼도 리스너가 미해제 상태로 남아 이후 무관한 드래그에 반응하는 문제가 없다.
    const pointerId = e.pointerId;
    const startX = e.clientX;
    const startY = e.clientY;
    let dragging = false;

    function onSelectStart(ev) {
      if (dragging) ev.preventDefault();
    }

    function onMove(ev) {
      if (ev.pointerId !== pointerId) return;
      if (!dragging) {
        if (Math.abs(ev.clientX - startX) < 4 && Math.abs(ev.clientY - startY) < 4) return;
        dragging = true;
        block.classList.add('is-dragging');
        document.addEventListener('selectstart', onSelectStart);
      }
    }

    function onUp(ev) {
      if (ev.pointerId !== pointerId) return;
      block.releasePointerCapture(pointerId);
      block.removeEventListener('pointermove', onMove);
      block.removeEventListener('pointerup', onUp);
      block.removeEventListener('pointercancel', onUp);
      document.removeEventListener('selectstart', onSelectStart);
      block.classList.remove('is-dragging');
      if (!dragging) return; // 이동 없이 그냥 클릭한 경우 - 브라우저가 이미 캐럿을 배치했으므로 그대로 둠

      const dropRange = range.getRangeFromPointInEditor(ev.clientX, ev.clientY);

      // 드롭 지점이 옮기려는 블록 자기 자신(또는 그 내부)이면 Range.insertNode가
      // "새 자식이 부모를 포함한다"는 HierarchyRequestError를 던진다 - 그냥 제자리에 둔다.
      if (block === dropRange.startContainer || block.contains(dropRange.startContainer)) {
        return;
      }

      try {
        dropRange.insertNode(block);
      } catch (err) {
        console.error('블록 재배치 실패', err);
        return;
      }
      const caretRange = range.anchorCaretAfterBlock(block);
      range.restoreRange(caretRange);
      state.savedRange = caretRange.cloneRange();
    }

    block.setPointerCapture(pointerId);
    block.addEventListener('pointermove', onMove);
    block.addEventListener('pointerup', onUp);
    block.addEventListener('pointercancel', onUp);
  });

  /* ---------- 캐럿 위치 추적 (툴바 클릭으로 포커스가 빠지기 전까지 계속 갱신) ---------- */

  dom.editorRoot.addEventListener('keyup', range.saveCurrentRange);
  dom.editorRoot.addEventListener('mouseup', range.saveCurrentRange);
  dom.editorRoot.addEventListener('input', range.saveCurrentRange);

  CE.blockChrome = {
    appendBlockControls: appendBlockControls
  };
})();
