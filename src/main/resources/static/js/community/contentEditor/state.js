// community/contentEditor/state.js — 본문 에디터 여러 모듈이 함께 읽고 쓰는 가변 상태
(function () {
  'use strict';
  window.ContentEditor = window.ContentEditor || {};
  const CE = window.ContentEditor;

  CE.state = {
    fileMap: new Map(),          // data-file-id -> File (새로 추가된 이미지만)
    fileIdSeq: 0,
    existingImageCount: 0,       // edit.jsp: 이미 저장된(잠금) 이미지 개수, write.jsp: 0
    existingImageUrls: [],       // edit.jsp: rehydrateFromServer()가 채움. 제출 시 삭제된 것 계산에 씀
    savedRange: null,            // 툴바 클릭 직전까지의 캐럿 위치
    builderFiles: [],            // [{id, file, url, x?, y?}] — 빌더 모달에서 고른 이미지들
    pendingTextOverflow: false   // contentLimitModal 확인 클릭 시 텍스트를 잘라낼지 여부
  };
})();
