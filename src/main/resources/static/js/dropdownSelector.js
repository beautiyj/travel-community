// dropdownSelector.js - 드롭다운 셀렉터 이벤트 핸들러
// 리다이렉션 없이 선택값 표시 + 활성 클래스 토글만 처리

(function () {
  function initDropdowns() {
    document.querySelectorAll(".drop-select-container").forEach(function (container) {
      // 이미 초기화된 컨테이너는 스킵 (중복 이벤트 방지)
      if (container.dataset.initialized) return;
      container.dataset.initialized = "true";

      var trigger = container.querySelector(".drop-select-trigger");
      var label   = container.querySelector(".drop-select-text");
      var menuItems = container.querySelectorAll(".drop-menu-item");

      if (!trigger || !label || !menuItems.length) return;

      // 부트스트랩 드롭다운 직접 토글 제어 (클릭 시 창 안 펼쳐짐 문제 해결)
      trigger.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();

        if (typeof bootstrap !== "undefined" && bootstrap.Dropdown) {
          var bsDropdown = bootstrap.Dropdown.getOrCreateInstance(trigger);
          bsDropdown.toggle();
        } else {
          var menu = container.querySelector(".drop-select-menu");
          if (menu) menu.classList.toggle("show");
        }
      });

      menuItems.forEach(function (btn) {
        btn.addEventListener("click", function () {
          var val          = btn.getAttribute("data-value");
          var selectedText = btn.getAttribute("data-label") || btn.textContent.trim();

          // 1. 활성화 클래스 변경
          menuItems.forEach(function (item) {
            item.classList.remove("is-active");
          });
          btn.classList.add("is-active");

          // 2. 버튼 라벨 텍스트 변경
          label.textContent = selectedText;

          // 3. 선택값 유무에 따라 is-selected 토글
          if (val) {
            trigger.classList.add("is-selected");
          } else {
            trigger.classList.remove("is-selected");
          }
        });
      });
    });
  }

  // DOM 로드 완료 여부에 상관없이 실행
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initDropdowns);
  } else {
    initDropdowns();
  }
})();