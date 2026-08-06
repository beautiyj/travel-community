// community/contentEditor/constants.js — 본문 에디터 전역에서 쓰는 상수(글자수/개수 제한, 합성 크기, 토큰 정규식 등)
(function () {
  'use strict';
  window.ContentEditor = window.ContentEditor || {};
  const CE = window.ContentEditor;

  const MAX_BUILDER_ITEMS = 4;        // 콜라주/슬라이더 빌더 각각의 최대 선택 장수
  const MAX_TOTAL_IMAGE_BLOCKS = 50;  // 본문 전체 이미지 블록(단일/콜라주/슬라이더 각 1개) 최대 개수
  const MAX_CONTENT_TEXT_LENGTH = 9999; // 본문에 실제로 타이핑한 텍스트 최대 길이

  const COMPOSE_CANVAS_MAX_DIM = 1600; // 콜라주/슬라이더를 합성한 결과 이미지의 긴 변 상한(px)
  const SLIDER_ITEM_HEIGHT = 280;      // .slider-item 기본 높이(community.css)와 동일
  const SLIDER_GAP = 8;                // .slider-track gap(community.css)과 동일

  const DEFAULT_COLLAGE_ITEM_WIDTH = 42; // % of canvas width, 구버전(폭 정보 없는) 토큰의 기본값과도 동일

  // 슬라이더 빌더 썸네일 박스 크기 산정 기준
  const SLIDER_THUMB_MAX_SIDE = 150;
  const SLIDER_THUMB_MIN_RATIO = 0.5;
  const SLIDER_THUMB_MAX_RATIO = 2;

  const TOKEN_PATTERN = /\[\[IMG:(\d+)(?::(left|right))?(?::(\d{1,3}))?\]\]|\[\[SLIDER:(\d+(?:,\d+)*)(?::(left|right))?(?::(\d{1,3}))?\]\]|\[\[COLLAGE:(?:(\d+)-(\d+):)?(\d+-\d+-\d+(?:-\d+)?(?:,\d+-\d+-\d+(?:-\d+)?)*)(?::(left|right))?(?::(\d{1,3}))?\]\]/g;

  CE.constants = {
    MAX_BUILDER_ITEMS: MAX_BUILDER_ITEMS,
    MAX_TOTAL_IMAGE_BLOCKS: MAX_TOTAL_IMAGE_BLOCKS,
    MAX_CONTENT_TEXT_LENGTH: MAX_CONTENT_TEXT_LENGTH,
    COMPOSE_CANVAS_MAX_DIM: COMPOSE_CANVAS_MAX_DIM,
    SLIDER_ITEM_HEIGHT: SLIDER_ITEM_HEIGHT,
    SLIDER_GAP: SLIDER_GAP,
    DEFAULT_COLLAGE_ITEM_WIDTH: DEFAULT_COLLAGE_ITEM_WIDTH,
    SLIDER_THUMB_MAX_SIDE: SLIDER_THUMB_MAX_SIDE,
    SLIDER_THUMB_MIN_RATIO: SLIDER_THUMB_MIN_RATIO,
    SLIDER_THUMB_MAX_RATIO: SLIDER_THUMB_MAX_RATIO,
    TOKEN_PATTERN: TOKEN_PATTERN
  };
})();
