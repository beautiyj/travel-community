// 업소 등록/수정 폼 공용: 사진 그리드(#venue-photos-grid)에 카드를 넣고 드래그로 순서를 섞을 수 있게 한다.

// 그리드에 data-order-field 속성이 있으면(수정 폼만 해당) 각 카드에 그 이름의 hidden input을 붙인다.
//   - 기존 사진: value = 이미지 URL
//   - 새 사진: value = "new" (실제 파일은 별도 file input으로 제출되므로, 서버는 "new" 토큰을 만날 때마다
//              해당 리스트에서 순서대로 하나씩 꺼내 매칭한다. 그래서 새 카드의 순서 = file input의 파일 순서로
//              항상 동기화해야 하고, 그 동기화는 syncFileInputOrder()가 담당한다.)
// 등록 폼은 이 속성이 없어서 hidden input을 붙이지 않고, 파일 제출 순서 자체가 곧 최종 순서가 된다.
//
// 삭제는 기존/신규 카드 모두 카드 우상단 × 버튼(.venue-photo-preview__remove)으로 통일한다.
//   - 신규 카드: 아직 서버에 없으니 DOM에서 완전히 제거
//   - 기존 카드: removeImageUrls 체크박스를 계속 폼에 남겨서 제출해야 하므로, DOM에서 지우지 않고
//              is-removed 클래스로 시각적으로만 숨긴 뒤 체크박스를 checked 처리한다.
document.addEventListener("DOMContentLoaded", function () {
    var MAX_PHOTOS = 5;

    var grid = document.getElementById("venue-photos-grid");
    var fileInput = document.getElementById("venue-photos-input");
    var countLabel = document.getElementById("venue-photos-count");
    var remainingLabel = document.getElementById("venue-photos-remaining");
    var dropzone = document.getElementById("venue-photos-dropzone");
    if (!grid || !fileInput) return;

    var orderField = grid.dataset.orderField || null;

    function getVisibleItems() {
        return Array.from(grid.children).filter(function (item) {
            var checkbox = item.querySelector('input[type="checkbox"]');
            return !checkbox || !checkbox.checked;
        });
    }

    function refresh() {
        var visibleItems = getVisibleItems();
        Array.from(grid.children).forEach(function (item) {
            item.classList.remove("venue-photo-grid__item--main");
        });
        if (visibleItems[0]) visibleItems[0].classList.add("venue-photo-grid__item--main");
        if (countLabel) countLabel.textContent = String(visibleItems.length);

        var remaining = Math.max(MAX_PHOTOS - visibleItems.length, 0);
        if (remainingLabel) remainingLabel.textContent = String(remaining);
        if (dropzone) dropzone.classList.toggle("is-hidden", remaining === 0);
    }

    // 드래그로 바뀐 새 카드들의 순서를 file input의 실제 FileList 순서에 반영한다.
    // FileList는 직접 수정할 수 없어서 DataTransfer로 다시 만들어 끼워 넣는다.
    function syncFileInputOrder() {
        var dt = new DataTransfer();
        Array.from(grid.querySelectorAll(".venue-photo-grid__item--new")).forEach(function (item) {
            if (item._file) dt.items.add(item._file);
        });
        fileInput.files = dt.files;
    }

    function createNewPhotoCard(file) {
        var item = document.createElement("div");
        item.className = "venue-photo-grid__item venue-photo-grid__item--new";
        item.draggable = true;
        item._file = file;

        var mainBadge = document.createElement("span");
        mainBadge.className = "venue-photo-grid__badge";
        mainBadge.textContent = "대표";
        item.appendChild(mainBadge);

        var newTag = document.createElement("span");
        newTag.className = "venue-photo-grid__new-tag";
        newTag.textContent = "신규";
        item.appendChild(newTag);

        var img = document.createElement("img");
        img.alt = file.name;
        item.appendChild(img);
        var reader = new FileReader();
        reader.onload = function (e) { img.src = e.target.result; };
        reader.readAsDataURL(file);

        var removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "venue-photo-preview__remove";
        removeBtn.setAttribute("aria-label", "사진 제거");
        removeBtn.textContent = "×";
        item.appendChild(removeBtn);

        //수정폼만 해당
        if (orderField) {
            var hidden = document.createElement("input");
            hidden.type = "hidden";
            hidden.name = orderField;
            hidden.value = "new";
            item.appendChild(hidden);
        }

        return item;
    }

    fileInput.addEventListener("change", function () {
        var room = MAX_PHOTOS - getVisibleItems().length;
        var incoming = Array.from(fileInput.files);
        if (incoming.length > Math.max(room, 0)) {
            window.alert("사진은 최대 " + MAX_PHOTOS + "장까지 등록할 수 있습니다. 남은 자리만큼만 추가됩니다.");
        }
        incoming.slice(0, Math.max(room, 0)).forEach(function (file) {
            grid.appendChild(createNewPhotoCard(file));
        });
        syncFileInputOrder();
        refresh();
    });

    // ── 사진 삭제 (× 버튼, 기존/신규 카드 공통 위임 처리) ──
    grid.addEventListener("click", function (e) {
        var removeBtn = e.target.closest(".venue-photo-preview__remove");
        if (!removeBtn) return;
        var item = removeBtn.closest(".venue-photo-grid__item");
        if (!item) return;

        if (item.classList.contains("venue-photo-grid__item--new")) {
            item.remove();
            syncFileInputOrder();
        } else {
            var checkbox = item.querySelector('input[type="checkbox"]');
            if (checkbox) checkbox.checked = true;
            item.classList.add("is-removed");
        }
        refresh();
    });

    // ── 드래그 순서변경 (기존/신규 카드 구분 없이 동일하게 동작) ──
    var draggedItem = null;

    grid.addEventListener("dragstart", function (e) {
        var item = e.target.closest(".venue-photo-grid__item");
        if (!item) return;
        draggedItem = item;
        item.classList.add("is-dragging");
        if (e.dataTransfer) e.dataTransfer.effectAllowed = "move";
    });

    grid.addEventListener("dragend", function () {
        if (draggedItem) draggedItem.classList.remove("is-dragging");
        draggedItem = null;
        syncFileInputOrder();
        refresh();
    });

    grid.addEventListener("dragover", function (e) {
        if (!draggedItem) return;
        e.preventDefault();
        var target = e.target.closest(".venue-photo-grid__item");
        if (!target || target === draggedItem) return;
        var rect = target.getBoundingClientRect();
        var insertBeforeTarget = (e.clientX - rect.left) < rect.width / 2;
        grid.insertBefore(draggedItem, insertBeforeTarget ? target : target.nextSibling);
    });

    refresh();
});
