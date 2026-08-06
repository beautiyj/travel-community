// community/contentEditor/canvasUtils.js — 합성용 <canvas>를 업로드 가능한 File로 변환하는 공용 유틸
(function () {
  'use strict';
  const CE = window.ContentEditor;

  // 캔버스에 그려 넣은 결과를 하나의 이미지 File로 만든다(콜라주/슬라이더 공용).
  // 콜라주/슬라이더 모두 사진끼리 빈틈없이 이어 그리므로 투명 이슈가 없어
  // 용량이 작은 JPEG를 그대로 쓴다.
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

  CE.canvasUtils = {
    canvasToImageFile: canvasToImageFile
  };
})();
