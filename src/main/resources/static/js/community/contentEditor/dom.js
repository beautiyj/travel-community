// community/contentEditor/dom.js — 본문 에디터가 사용하는 DOM 요소 참조를 한 곳에서 조회해 공유
(function () {
  'use strict';
  window.ContentEditor = window.ContentEditor || {};
  const CE = window.ContentEditor;

  const editorRoot = document.getElementById('contentEditor');

  CE.dom = {
    editorRoot: editorRoot,
    contentField: document.getElementById('content'),
    imagesInput: document.getElementById('images'),
    photoInput: document.getElementById('photoInput'),

    toolPhotoBtn: document.getElementById('toolPhotoBtn'),
    toolCollageBtn: document.getElementById('toolCollageBtn'),
    toolSliderBtn: document.getElementById('toolSliderBtn'),

    collageFileInput: document.getElementById('collageFileInput'),
    collageCanvas: document.getElementById('collageCanvas'),
    collageConfirmBtn: document.getElementById('collageConfirmBtn'),
    sliderFileInput: document.getElementById('sliderFileInput'),
    sliderPreview: document.getElementById('sliderPreview'),
    sliderConfirmBtn: document.getElementById('sliderConfirmBtn'),

    contentLimitModal: document.getElementById('contentLimitModal'),
    form: editorRoot ? editorRoot.closest('form') : null
  };
})();
