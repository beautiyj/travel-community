// community/placeTag.js
// write.jsp / edit.jsp 공용: "방문자인증후기"/"일반후기" 카테고리일 때만 장소 태그 필드를 보여주고,
// 장소 검색 모달(placeSearchModal.jsp)에서 장소를 검색/선택/해제하는 로직을 담당함.
// - 방문자인증후기: 로그인 회원의 확정(결제완료) 예약 장소만 검색됨 (서버 /community/place/search가 category로 분기)
// - 일반후기: 전체 장소 검색
// ※ window.CP 전역변수(contextPath)가 이 스크립트보다 먼저 정의되어 있어야 함.

const VERIFIED_REVIEW_VALUE = '방문자인증후기';
const GENERAL_REVIEW_VALUE = '일반후기';
let placeSearchTimer = null;

// 카테고리 라디오 값에 따라 장소 태그 필드 노출/숨김
function syncPlaceFieldVisibility() {
  const checked = document.querySelector('input[name="category"]:checked');
  const field = document.getElementById('place-tag-field');
  if (!field) return;

  const showField = checked && (checked.value === VERIFIED_REVIEW_VALUE || checked.value === GENERAL_REVIEW_VALUE);
  field.style.display = showField ? '' : 'none';

  // 장소 태그 대상 카테고리가 아닌 값으로 바뀌면 이미 골라둔 태그도 같이 초기화
  if (!showField) clearSelectedPlace();
}

// HTML 삽입 전 이스케이프 (장소 이름은 place 테이블 값이라 사용자 입력이 그대로 노출될 수 있음)
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// place_type(1=숙박/2=맛집/그 외=여행지) → 배지 클래스/텍스트
// common/tagButton.jsp · tagButton.css 의 type-food/type-stay/type-tour 클래스를 그대로 재사용
function placeTypeMeta(placeType) {
  const type = Number(placeType);
  if (type === 1) return { cls: 'stay', label: '숙박' };
  if (type === 2) return { cls: 'food', label: '맛집' };
  return { cls: 'tour', label: '여행지' };
}

function placeTypeBadgeHtml(placeType) {
  const meta = placeTypeMeta(placeType);
  return '<div class="tag-view type-' + meta.cls + '"><span class="tag-text">' + meta.label + '</span></div>';
}

// 태그 해제 (hidden input 비우고, 선택 표시 숨기고, 검색 버튼 다시 노출)
function clearSelectedPlace() {
  const placeIdInput = document.getElementById('placeId');
  const selected = document.getElementById('place-tag-selected');
  const openBtn = document.getElementById('place-tag-open-btn');

  if (placeIdInput) placeIdInput.value = '';
  if (selected) selected.style.display = 'none';
  if (openBtn) openBtn.style.display = '';
}

// 검색 결과에서 장소를 클릭했을 때 호출됨 (placeSearchModal.jsp 안의 결과 항목 onclick에서 호출)
function selectPlaceTag(placeId, placeName, placeType) {
  const placeIdInput = document.getElementById('placeId');
  const selected = document.getElementById('place-tag-selected');
  const nameEl = document.getElementById('place-tag-selected-name');
  const openBtn = document.getElementById('place-tag-open-btn');
  const modal = document.getElementById('placeSearchModal');

  if (placeIdInput) placeIdInput.value = placeId;
  if (nameEl) nameEl.innerHTML = escapeHtml(placeName) + ' ' + placeTypeBadgeHtml(placeType);
  if (selected) selected.style.display = '';
  if (openBtn) openBtn.style.display = 'none';
  if (modal) modal.classList.remove('open');
}

// 검색창 입력할 때마다 (250ms 디바운스) 서버에 장소 이름 검색 요청
// 현재 선택된 카테고리를 함께 보내서, 서버가 방문자인증후기/그 외를 구분해 검색 대상을 다르게 처리함
function searchPlaceTag(inputEl) {
  clearTimeout(placeSearchTimer);
  const keyword = inputEl.value.trim();
  const resultsEl = inputEl.closest('.place-search-modal').querySelector('.place-search-results');
  const checkedCategory = document.querySelector('input[name="category"]:checked');
  const category = checkedCategory ? checkedCategory.value : '';

  if (keyword.length === 0) {
    resultsEl.innerHTML = '';
    return;
  }

  placeSearchTimer = setTimeout(function () {
    fetch(window.CP + '/community/place/search?keyword=' + encodeURIComponent(keyword) + '&category=' + encodeURIComponent(category))
      .then(function (res) { return res.json(); })
      .then(function (list) {
        if (!list || list.length === 0) {
          resultsEl.innerHTML = '<div class="place-search-empty">검색 결과가 없습니다</div>';
          return;
        }
        resultsEl.innerHTML = list.map(function (p) {
          const safeName = String(p.name).replace(/'/g, "\\'");
          return '<div class="place-search-item" onclick="selectPlaceTag(' + p.placeId + ", '" + safeName + "', " + Number(p.placeType) + ')">'
            + '<span class="place-search-item-name">' + escapeHtml(p.name) + '</span>'
            + placeTypeBadgeHtml(p.placeType)
            + '</div>';
        }).join('');
      })
      .catch(function () {
        resultsEl.innerHTML = '<div class="place-search-empty">검색 중 오류가 발생했습니다</div>';
      });
  }, 250);
}

document.addEventListener('DOMContentLoaded', function () {
  // 카테고리 바뀔 때마다 장소 태그 필드 노출 여부 갱신
  document.querySelectorAll('input[name="category"]').forEach(function (radio) {
    radio.addEventListener('change', syncPlaceFieldVisibility);
  });
  syncPlaceFieldVisibility(); // 최초 로드 시(수정 폼에서 이미 방문자인증후기가 선택돼 있는 경우) 반영

  // "장소 검색" 버튼 → 모달 열기
  const openBtn = document.getElementById('place-tag-open-btn');
  if (openBtn) {
    openBtn.addEventListener('click', function () {
      const modal = document.getElementById('placeSearchModal');
      if (modal) modal.classList.add('open');
    });
  }

  // 태그 해제 버튼
  const removeBtn = document.getElementById('place-tag-remove');
  if (removeBtn) removeBtn.addEventListener('click', clearSelectedPlace);
});