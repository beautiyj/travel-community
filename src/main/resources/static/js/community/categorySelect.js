// community/categorySelect.js — write.jsp / edit.jsp 공용
// dropdownSelector.js 는 라벨 텍스트/active 클래스 전환만 처리하므로, 여기서는
// 메뉴 아이템 클릭 시 실제 제출용 숨은 라디오(input[name="category"])를 체크하고
// change 이벤트를 발생시켜 placeTag.js(카테고리별 장소 태그 노출)가 그대로 동작하게 한다.
document.addEventListener('DOMContentLoaded', function () {
  const dropdown = document.getElementById('categoryDropdown');
  if (!dropdown) return;

  const menuItems = dropdown.querySelectorAll('.drop-menu-item');
  const radios = document.querySelectorAll('.category-radio-group input[name="category"]');
  const trigger = dropdown.querySelector('.drop-select-trigger');
  const menu = dropdown.querySelector('.drop-select-menu');

  menuItems.forEach(function (item) {
    item.addEventListener('click', function () {
      const value = item.getAttribute('data-value');

      radios.forEach(function (radio) {
        if (radio.value === value) {
          radio.checked = true;
          radio.dispatchEvent(new Event('change', { bubbles: true }));
        }
      });

      // dropdownSelector.js 는 라벨/active 클래스만 갱신하고 메뉴를 닫지는 않으므로
      // 선택 즉시 여기서 메뉴를 닫아준다 (bootstrap dropdown 을 쓰는 경우도 함께 처리)
      if (menu) menu.classList.remove('show');
      if (trigger) {
        trigger.setAttribute('aria-expanded', 'false');
        if (typeof bootstrap !== 'undefined' && bootstrap.Dropdown) {
          const bsDropdown = bootstrap.Dropdown.getInstance(trigger);
          if (bsDropdown) bsDropdown.hide();
        }
      }
    });
  });
});
