// community/placeTag.js
// write.jsp / edit.jsp 공용: "방문자인증후기"/"일반후기" 카테고리일 때만 장소 태그 필드를 보여주고,
// 장소 검색 모달(placeSearchModal.jsp)에서 장소를 검색/선택/해제하는 로직을 담당함.
// - 방문자인증후기: 로그인 회원의 확정(결제완료) 예약 장소만 검색됨 (서버 /community/place/search가 category로 분기)
//   장소 태그가 필수라서, 태그 없이 제출하면 막고 안내 모달(placeTagRequiredModal)을 띄움
// - 일반후기: 전체 장소 검색, 장소 태그는 선택사항이고 여러 개 선택 가능 (최대 5개)
// ※ window.CP 전역변수(contextPath)가 이 스크립트보다 먼저 정의되어 있어야 함.

const VERIFIED_REVIEW_VALUE = '방문자인증후기';
const GENERAL_REVIEW_VALUE = '일반후기';
const MAX_PLACE_TAGS = 5;
let placeSearchTimer = null;

function currentCategoryValue() {
  const checked = document.querySelector('input[name="category"]:checked');
  return checked ? checked.value : '';
}

// 카테고리 라디오 값에 따라 장소 태그 필드 노출/숨김 + 단일(방문자인증후기)/다중(일반후기) 목록 UI 전환
function syncPlaceFieldVisibility() {
  const category = currentCategoryValue();
  const field = document.getElementById('place-tag-field');
  if (!field) return;

  const showField = category === VERIFIED_REVIEW_VALUE || category === GENERAL_REVIEW_VALUE;
  field.style.display = showField ? '' : 'none';

  const listEl = document.getElementById('place-tag-selected-list');
  if (listEl) listEl.style.display = category === GENERAL_REVIEW_VALUE ? '' : 'none';

  // 장소 태그 대상 카테고리가 아닌 값으로 바뀌면 이미 골라둔 태그도 같이 초기화
  if (!showField) clearAllPlaceTags();

  updatePlaceTagLimitUI();
}

// 일반후기 다중 태그가 최대 개수(5개)에 도달하면 "장소 검색해서 태그하기" 버튼을 숨기고
// 안내 문구를 보여줌. edit.jsp처럼 이미 5개가 채워진 상태로 페이지가 로드되는 경우도 커버해야 해서
// 카테고리 변경 시뿐 아니라 태그 추가/삭제 직후에도 호출됨
function updatePlaceTagLimitUI() {
  const openBtn = document.getElementById('place-tag-open-btn');
  const limitMsg = document.getElementById('place-tag-limit-msg');
  const listEl = document.getElementById('place-tag-selected-list');

  if (currentCategoryValue() !== GENERAL_REVIEW_VALUE) {
    if (limitMsg) limitMsg.style.display = 'none';
    return;
  }

  const atMax = !!listEl && listEl.children.length >= MAX_PLACE_TAGS;
  if (openBtn) openBtn.style.display = atMax ? 'none' : '';
  if (limitMsg) limitMsg.style.display = atMax ? '' : 'none';
}

// 사용자가 카테고리 라디오를 직접 바꿨을 때만 호출됨: 방문자인증후기(단일 placeId)와
// 일반후기(다중 placeIds)는 저장 방식이 서로 달라서, 전환 시 잔여 선택이 남지 않도록 항상 초기화
function handleCategoryChange() {
  clearAllPlaceTags();
  syncPlaceFieldVisibility();
}

function clearAllPlaceTags() {
  clearSelectedPlace();
  clearPlaceTagChips();
}

// 일반후기 다중 태그 목록을 통째로 비움
function clearPlaceTagChips() {
  const listEl = document.getElementById('place-tag-selected-list');
  if (listEl) listEl.innerHTML = '';
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

// place_type("stay"/"food"/"tour")에 대응하는 배지 라벨
// common/tagButton.jsp · tagButton.css 의 type-food/type-stay/type-tour 클래스를 그대로 재사용
const PLACE_TYPE_LABELS = { stay: '숙박', food: '맛집', tour: '관광지' };

function placeTypeBadgeHtml(placeType) {
  const label = PLACE_TYPE_LABELS[placeType] || '관광지';
  return '<div class="tag-view type-' + placeType + '"><span class="tag-text">' + label + '</span></div>';
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
// 일반후기(다중)면 칩 목록에 추가, 방문자인증후기(단일)면 기존처럼 덮어씀
function selectPlaceTag(placeId, placeName, placeType) {
  if (currentCategoryValue() === GENERAL_REVIEW_VALUE) {
    addPlaceTagChip(placeId, placeName, placeType);
  } else {
    const placeIdInput = document.getElementById('placeId');
    const selected = document.getElementById('place-tag-selected');
    const nameEl = document.getElementById('place-tag-selected-name');
    const openBtn = document.getElementById('place-tag-open-btn');

    if (placeIdInput) placeIdInput.value = placeId;
    if (nameEl) nameEl.innerHTML = escapeHtml(placeName) + ' ' + placeTypeBadgeHtml(placeType);
    if (selected) selected.style.display = '';
    if (openBtn) openBtn.style.display = 'none';
  }
  if (window.closeModal) window.closeModal('placeSearchModal');
}

// 일반후기 다중 태그: 칩 하나 추가 (이미 선택된 장소면 중복 추가하지 않음). 최대 5개.
// 각 칩은 자체 hidden input(name="placeIds")을 가지고 있어서 폼 제출 시 선택한 개수만큼 그대로 넘어감
function addPlaceTagChip(placeId, placeName, placeType) {
  const listEl = document.getElementById('place-tag-selected-list');
  if (!listEl) return;
  if (listEl.querySelector('[data-place-id="' + placeId + '"]')) return;
  if (listEl.children.length >= MAX_PLACE_TAGS) return;

  const chip = document.createElement('span');
  chip.className = 'place-tag-selected';
  chip.setAttribute('data-place-id', String(placeId));
  chip.innerHTML =
    '<input type="hidden" name="placeIds" value="' + placeId + '">' +
    '<span>' + escapeHtml(placeName) + ' ' + placeTypeBadgeHtml(placeType) + '</span>' +
    '<button type="button" class="place-tag-remove" data-remove-place-id="' + placeId + '">✕</button>';
  listEl.appendChild(chip);
  updatePlaceTagLimitUI();
}

// 검색 결과 아이템 목록의 HTML 생성 (최초 검색/더보기 공용). 이름에 매칭된 keyword는 굵게 강조.
function buildPlaceItemsHtml(list, keyword) {
  return list.map(function (p) {
    const safeName = String(p.name).replace(/'/g, "\\'");
    return '<div class="place-search-item" onclick="selectPlaceTag(' + p.placeId + ", '" + safeName + "', '" + p.placeType + "')\">"
      + '<span class="place-search-item-name">' + highlightKeyword(p.name, keyword) + '</span>'
      + placeTypeBadgeHtml(p.placeType)
      + '</div>';
  }).join('');
}

// resultsEl에 "더보기" 버튼을 붙이거나(hasMore) 제거함
function renderMoreButton(resultsEl, hasMore) {
  const existing = resultsEl.querySelector('.place-search-more-btn');
  if (existing) existing.remove();
  if (!hasMore) return;

  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'place-search-more-btn';
  btn.textContent = '더보기';
  btn.addEventListener('click', function () { loadMorePlaces(resultsEl); });
  resultsEl.appendChild(btn);
}

// keyword로 장소를 검색해 resultsEl에 렌더링 (0페이지, 새 검색 취급). keyword가 빈 문자열이면
// 서버가 전체 장소를 이름 가나다순으로 돌려줌 - 모달을 막 열어서 아직 아무것도 안 친 상태에 사용됨
function fetchAndRenderPlaces(resultsEl, category, keyword) {
  resultsEl._placeSearchState = { keyword: keyword, category: category, page: 0, loading: false };

  fetch(window.CP + '/community/place/search?keyword=' + encodeURIComponent(keyword) + '&category=' + encodeURIComponent(category) + '&page=0')
    .then(function (res) { return res.json(); })
    .then(function (data) {
      const items = (data && data.items) || [];
      if (items.length === 0) {
        resultsEl.innerHTML = '<div class="place-search-empty">검색 결과가 없습니다</div>';
        return;
      }
      resultsEl.innerHTML = buildPlaceItemsHtml(items, keyword);
      renderMoreButton(resultsEl, data && data.hasMore);
    })
    .catch(function () {
      resultsEl.innerHTML = '<div class="place-search-empty">검색 중 오류가 발생했습니다</div>';
    });
}

// 검색창 입력할 때마다 (250ms 디바운스) 서버에 장소 이름 검색 요청 (새 검색 = 항상 0페이지부터)
// 현재 선택된 카테고리를 함께 보내서, 서버가 방문자인증후기/그 외를 구분해 검색 대상을 다르게 처리함
// keyword가 빈 문자열이어도(입력을 다 지운 경우) 그대로 검색해 전체 목록으로 되돌아감
function searchPlaceTag(inputEl) {
  clearTimeout(placeSearchTimer);
  const keyword = inputEl.value.trim();
  const resultsEl = inputEl.closest('.place-search-modal').querySelector('.place-search-results');
  const checkedCategory = document.querySelector('input[name="category"]:checked');
  const category = checkedCategory ? checkedCategory.value : '';

  placeSearchTimer = setTimeout(function () {
    fetchAndRenderPlaces(resultsEl, category, keyword);
  }, 250);
}

// "더보기" 버튼 클릭 시 다음 페이지를 요청해 기존 목록 뒤에 이어붙임
function loadMorePlaces(resultsEl) {
  const state = resultsEl._placeSearchState;
  if (!state || state.loading) return;

  const btn = resultsEl.querySelector('.place-search-more-btn');
  state.loading = true;
  if (btn) { btn.disabled = true; btn.textContent = '불러오는 중...'; }

  const nextPage = state.page + 1;

  fetch(window.CP + '/community/place/search?keyword=' + encodeURIComponent(state.keyword) + '&category=' + encodeURIComponent(state.category) + '&page=' + nextPage)
    .then(function (res) { return res.json(); })
    .then(function (data) {
      const items = (data && data.items) || [];
      state.page = nextPage;
      state.loading = false;

      if (btn) btn.remove();
      resultsEl.insertAdjacentHTML('beforeend', buildPlaceItemsHtml(items, state.keyword));
      renderMoreButton(resultsEl, data && data.hasMore);
    })
    .catch(function () {
      state.loading = false;
      if (btn) { btn.disabled = false; btn.textContent = '더보기'; }
    });
}

document.addEventListener('DOMContentLoaded', function () {
  // 카테고리를 사용자가 직접 바꿀 때마다 장소 태그 필드 노출 여부 + 선택 초기화
  document.querySelectorAll('input[name="category"]').forEach(function (radio) {
    radio.addEventListener('change', handleCategoryChange);
  });
  syncPlaceFieldVisibility(); // 최초 로드 시(수정 폼에서 이미 선택돼 있는 경우) 화면만 맞춤, 기존 선택은 그대로 둠

  // "장소 검색" 버튼 → 모달 열기 + 아직 검색어를 입력하지 않은 상태이므로 전체 장소를 가나다순으로 미리 보여줌
  const openBtn = document.getElementById('place-tag-open-btn');
  if (openBtn) {
    openBtn.addEventListener('click', function () {
      const modal = document.getElementById('placeSearchModal');
      if (!modal) return;
      if (window.openModal) window.openModal('placeSearchModal');

      const inputEl = modal.querySelector('.place-search-input');
      const resultsEl = modal.querySelector('.place-search-results');
      if (inputEl) inputEl.value = '';
      if (resultsEl) {
        const checkedCategory = document.querySelector('input[name="category"]:checked');
        const category = checkedCategory ? checkedCategory.value : '';
        fetchAndRenderPlaces(resultsEl, category, '');
      }
    });
  }

  // 태그 해제 버튼 (방문자인증후기: 단일 pill)
  const removeBtn = document.getElementById('place-tag-remove');
  if (removeBtn) removeBtn.addEventListener('click', clearSelectedPlace);

  // 태그 해제 버튼 (일반후기: 다중 칩, 동적으로 추가되므로 이벤트 위임)
  const listEl = document.getElementById('place-tag-selected-list');
  if (listEl) {
    listEl.addEventListener('click', function (e) {
      const btn = e.target.closest('[data-remove-place-id]');
      if (!btn) return;
      const chip = btn.closest('[data-place-id]');
      if (chip) chip.remove();
      updatePlaceTagLimitUI();
    });
  }

  // 방문자인증후기는 장소 태그 필수: 제출 시 선택 안 되어 있으면 막고 안내 모달을 띄움
  // (폼 제출을 막을 뿐이라 작성 중이던 제목/본문은 그대로 남음)
  const placeIdInput = document.getElementById('placeId');
  const form = placeIdInput ? placeIdInput.closest('form') : null;
  if (form) {
    form.addEventListener('submit', function (e) {
      if (currentCategoryValue() === VERIFIED_REVIEW_VALUE && !placeIdInput.value) {
        e.preventDefault();
        if (typeof openModal === 'function') openModal('placeTagRequiredModal');
      }
    });
  }
});