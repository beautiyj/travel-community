// community/contentEditor/range.js — 캐럿(Range) 저장/복원, 클릭 좌표→Range 변환, 블록 삽입 후 캐럿 앵커링 공용 유틸
(function () {
  'use strict';
  const CE = window.ContentEditor;
  const dom = CE.dom;
  const state = CE.state;

  function endOfEditorRange() {
    const range = document.createRange();
    range.selectNodeContents(dom.editorRoot);
    range.collapse(false);
    return range;
  }

  function saveCurrentRange() {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0 && dom.editorRoot.contains(sel.anchorNode)) {
      state.savedRange = sel.getRangeAt(0).cloneRange();
    }
  }

  function restoreRange(range) {
    dom.editorRoot.focus();
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
    const rect = dom.editorRoot.getBoundingClientRect();
    const clampedX = Math.min(Math.max(x, rect.left + 1), rect.right - 1);
    const clampedY = Math.min(Math.max(y, rect.top + 1), rect.bottom - 1);
    const range = getRangeFromPoint(clampedX, clampedY);
    if (range && dom.editorRoot.contains(range.startContainer)) return range;
    return endOfEditorRange();
  }

  // 블록 바로 뒤에 이어 쓸 실제 텍스트가 없으면(빈 에디터에 처음 삽입했거나, 블록을
  // 연달아 삽입/재배치한 경우) 캐럿을 앵커링할 자리를 만들어야 한다. 본문 이미지 블록은
  // align-center(폭 100%, 줄 전체 차지) 또는 align-left/right(플로트)로 렌더링되는데,
  // 이런 레이아웃 바로 뒤에 "빈 텍스트 노드"만 두면 크롬이 그 자리에 실제 라인 박스를
  // 만들어주지 않아(Range.getBoundingClientRect()가 전부 0으로 나옴) 포커스/셀렉션은
  // 정상으로 잡혀 있어도 타이핑한 글자가 소리 없이 사라진다. <br>을 그 자리에 두고
  // 캐럿을 <br> 바로 앞 "컨테이너 오프셋"(텍스트 노드 안이 아니라 부모 기준 인덱스)에
  // 둬야 실제로 타이핑이 먹는 자리로 인식되고, 타이핑을 시작하면 브라우저가 그 <br>을
  // 알아서 걷어낸다(표준 contenteditable 동작). 이미 텍스트가 있던 자리에 삽입한
  // 경우엔 insertNode가 분할해 남긴 텍스트 노드를 그대로 재사용한다(이미 라인 박스가
  // 있으므로 문제 없음). 블록을 삽입하거나(insertBlockAtRange) 드래그로 재배치한
  // 뒤(블록 재배치 핸들러) 모두 이 헬퍼로 캐럿을 앵커링해야 한다 - setStartAfter(blockEl)나
  // 빈 텍스트 노드처럼 라인 박스 없는 위치에 직접 캐럿을 두면 다시 같은 버그로 회귀한다.
  function anchorCaretAfterBlock(blockEl) {
    const parent = blockEl.parentNode;
    const sibling = blockEl.nextSibling;
    const range = document.createRange();

    if (sibling && sibling.nodeType === Node.TEXT_NODE && sibling.textContent.length > 0) {
      range.setStart(sibling, 0);
      range.collapse(true);
      return range;
    }

    const br = (sibling && sibling.nodeName === 'BR') ? sibling : document.createElement('br');
    if (br !== sibling) parent.insertBefore(br, sibling);

    range.setStart(parent, Array.prototype.indexOf.call(parent.childNodes, br));
    range.collapse(true);
    return range;
  }

  // 블록을 range 위치에 그대로 끼워 넣는다. 강제 줄바꿈을 추가하지 않으므로
  // 삽입한 자리 바로 옆(또는 정렬에 따라 옆/위아래)에서 바로 이어 쓸 수 있다.
  function insertBlockAtRange(range, blockEl) {
    range.deleteContents();
    range.insertNode(blockEl);

    range = anchorCaretAfterBlock(blockEl);
    restoreRange(range);
    state.savedRange = range.cloneRange();
    return range;
  }

  CE.range = {
    endOfEditorRange: endOfEditorRange,
    saveCurrentRange: saveCurrentRange,
    restoreRange: restoreRange,
    getRangeFromPoint: getRangeFromPoint,
    getRangeFromPointInEditor: getRangeFromPointInEditor,
    anchorCaretAfterBlock: anchorCaretAfterBlock,
    insertBlockAtRange: insertBlockAtRange
  };
})();
