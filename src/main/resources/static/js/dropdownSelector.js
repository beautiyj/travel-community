// dropdownSelector.js - 드롭다운 셀렉터 이벤트 핸들러
// 선택값 표시 + 활성 클래스 토글 + 여닫힘을 순수 JS로 처리 (Bootstrap 의존 없음)
// data-radio-name 속성이 있으면 항목 클릭 시 같은 name의 라디오 그룹도 동기화 + change 이벤트 발생
// (예: community/write.jsp·edit.jsp의 카테고리 드롭다운 -> placeTag.js 연동)

(function () {
  var openContainer = null;

  function closeContainer(container) {
    var trigger = container.querySelector(".drop-select-trigger");
    var menu = container.querySelector(".drop-select-menu");

    if (menu) menu.classList.remove("show");
    if (trigger) trigger.setAttribute("aria-expanded", "false");
    if (openContainer === container) openContainer = null;
  }

  function openContainerEl(container) {
    if (openContainer && openContainer !== container) closeContainer(openContainer);

    var trigger = container.querySelector(".drop-select-trigger");
    var menu = container.querySelector(".drop-select-menu");

    if (menu) menu.classList.add("show");
    if (trigger) trigger.setAttribute("aria-expanded", "true");
    openContainer = container;
  }

  function initDropdowns() {
    document.querySelectorAll(".drop-select-container").forEach(function (container) {
      // 이미 초기화된 컨테이너는 스킵 (중복 이벤트 방지)
      if (container.dataset.initialized) return;
      container.dataset.initialized = "true";

      var trigger = container.querySelector(".drop-select-trigger");
      var label   = container.querySelector(".drop-select-text");
      var menu    = container.querySelector(".drop-select-menu");
      var menuItems = container.querySelectorAll(".drop-menu-item");
      var hiddenInput = container.querySelector("input[type=hidden]");
      var radioName = container.dataset.radioName;

      if (!trigger || !label || !menuItems.length) return;

      trigger.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();

        var isOpen = menu && menu.classList.contains("show");
        if (isOpen) {
          closeContainer(container);
        } else {
          openContainerEl(container);
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

          // 2-1. hidden input이 있으면 실제 제출값 갱신 (검색폼 등에서 선택값을 서버로 전달)
          if (hiddenInput) {
            hiddenInput.value = val || "";
          }

          // 2-2. data-radio-name이 있으면 같은 name의 숨은 라디오도 동기화 + change 이벤트 발생
          //      (라디오 자체는 프로그래밍적으로 checked만 바꿔서는 change 이벤트가 안 터지므로 수동 dispatch)
          if (radioName) {
            document.querySelectorAll('input[name="' + radioName + '"]').forEach(function (radio) {
              if (radio.value === val) {
                radio.checked = true;
                radio.dispatchEvent(new Event("change", { bubbles: true }));
              }
            });
          }

          // 3. 선택값 유무에 따라 is-selected 토글
          if (val) {
            trigger.classList.add("is-selected");
          } else {
            trigger.classList.remove("is-selected");
          }

          // 4. 항목 선택 후 메뉴 닫기
          closeContainer(container);
        });
      });
    });
  }

  // 열려 있는 드롭다운 바깥을 클릭하면 닫기
  document.addEventListener("click", function (e) {
    if (openContainer && !openContainer.contains(e.target)) {
      closeContainer(openContainer);
    }
  });

  // DOM 로드 완료 여부에 상관없이 실행
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initDropdowns);
  } else {
    initDropdowns();
  }
})();
