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

    // register/edit 폼은 서로 배타적으로만 렌더링되지만 그리드/카운트 id는 폼마다 달라서
    // (venue-photos-grid vs venue-photos-grid-edit) 항상 존재하는 공용 class로 찾는다.
    var grid = document.querySelector(".venue-photo-grid");
    var fileInput = document.getElementById("venue-photos-input");
    var countLabel = document.querySelector(".venue-photos-count");
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

    //주소검색 처리
    document.querySelectorAll(".js-address-search").forEach(function (btn) {
        var prefix = btn.dataset.targetPrefix || "";
        var addressInput = document.getElementById(prefix + "address");
        var detailInput = document.getElementById(prefix + "addressDetail");

        btn.addEventListener("click", function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    addressInput.value = data.roadAddress || data.jibunAddress;
                    detailInput.focus();
                }
            }).open();
        });

        // 수정 폼은 서버에서 합쳐진 주소 한 덩어리를 address에 채워 내려주므로 상세주소는 비어 있다.
        // 이 상태로 제출하면 상세주소를 덧붙이지 않고 기존 주소가 그대로 유지된다.

        // 주소 미입력 시 제출 막기 (readonly input은 required 검증 대상에서 제외되므로 직접 체크)
        // + 제출 직전 상세주소를 address 값에 합쳐서 하나의 필드로 전송
        if (btn.form) {
            btn.form.addEventListener("submit", function (e) {
                if (!addressInput.value) {
                    e.preventDefault();
                    window.alert("주소를 검색해 입력해주세요.");
                    addressInput.focus();
                    return;
                }
                if (detailInput.value) {
                    addressInput.value = addressInput.value + " " + detailInput.value;
                }
            });
        }
    });

    refresh();
});

// ── 가격 설정 (등록/수정 폼 공용) ──
// 1) 가격은 숙박(placeType='stay')만 설정 대상이라 업종이 숙박일 때만 영역을 노출한다.
//    맛집/관광지는 서버가 무조건 FREE로 저장하므로 여기서 값을 보내지 않아도 된다.
// 2) 가격 유형 라디오에서 "가격입력"(FIXED)을 고른 경우에만 금액 input을 연다.
// 사진 그리드 로직과 무관하게 동작해야 해서 별도 리스너로 분리했다.
document.addEventListener("DOMContentLoaded", function () {
    var LODGING_PLACE_TYPE = "stay";

    var priceGroup = document.querySelector(".js-price-group");
    if (!priceGroup) return;

    var placeTypeSelect = document.querySelector('select[name="placeType"]');
    var priceInputBox = priceGroup.querySelector(".js-price-input");
    var priceInput = priceInputBox.querySelector('input[name="minPrice"]');
    var priceRadios = priceGroup.querySelectorAll('input[name="priceType"]');

    function syncPriceGroup() {
        var isLodging = !placeTypeSelect || placeTypeSelect.value === LODGING_PLACE_TYPE;
        priceGroup.classList.toggle("is-hidden", !isLodging);

        var checked = priceGroup.querySelector('input[name="priceType"]:checked');
        var isFixed = isLodging && checked && checked.value === "FIXED";
        priceInputBox.classList.toggle("is-hidden", !isFixed);

        // 숨겨진 상태의 값이 그대로 제출되지 않도록 비활성화한다 (disabled 필드는 전송 대상에서 제외됨)
        priceInput.disabled = !isFixed;
        priceInput.required = isFixed;
        if (!isFixed) priceInput.value = "";
    }

    if (placeTypeSelect) placeTypeSelect.addEventListener("change", syncPriceGroup);
    priceRadios.forEach(function (radio) {
        radio.addEventListener("change", syncPriceGroup);
    });

    syncPriceGroup();
});

// ── 부가정보 입력 (등록/수정 폼 공용) ──
// 2단계 선택이다.
//   1) 업종(placeType)을 고르면 그 업종의 항목 칩 묶음만 열고 나머지는 닫는다.
//   2) 칩을 고르면 그 항목의 입력칸이 아래에 나타난다.
//
// 입력칸은 JSP에서 disabled로 그려져 있고 칩을 골라야 활성화된다. disabled 필드는 전송 대상에서
// 빠지므로 고르지 않은 항목·선택하지 않은 업종의 항목은 서버로 가지 않는다.
// (라벨 hidden과 값 text가 짝을 이뤄 제출되는 구조라, 둘을 항상 같이 켜고 꺼야 짝이 어긋나지 않는다.
//  그래서 아래에서 행 안의 input을 통째로 켜고 끈다)
//
// 칩을 다시 눌러 뺐을 때 입력값을 지우지는 않는다. 실수로 뺐다가 다시 고르면 쓰던 값이 그대로 남아 있고,
// 뺀 상태로 저장하면 disabled라 전송되지 않으므로 결과적으로 삭제된다.
//
// 반려동물 동반 정보는 업종과 무관한 공통 묶음이라 체크박스로 따로 여닫는다.
// 수정 화면 진입 시 이미 저장된 값이 있는 항목은 칩이 켜진 채로 열어둔다.
document.addEventListener("DOMContentLoaded", function () {
    var typeGroups = Array.from(document.querySelectorAll(".js-extra-group"));
    var petGroup = document.querySelector(".js-extra-pet-group");
    var petToggle = document.querySelector(".js-extra-pet-toggle");
    if (!typeGroups.length && !petGroup) return;

    var placeTypeSelect = document.querySelector('select[name="placeType"]');
    var allGroups = typeGroups.concat(petGroup ? [petGroup] : []);

    function rowOf(chip) {
        return document.getElementById(chip.dataset.target);
    }

    // 묶음 하나를 현재 상태(업종 선택 + 칩 선택)에 맞춰 다시 그린다.
    // groupActive가 false면 칩이 켜져 있어도 입력칸은 전송 대상에서 빠진다.
    function syncGroup(group, groupActive) {
        group.classList.toggle("is-hidden", !groupActive);

        var chips = Array.from(group.querySelectorAll(".js-extra-chip"));
        var pickedCount = 0;

        chips.forEach(function (chip) {
            // 칩의 체크박스는 CSS로 숨겨져 있어서, 선택 표시는 라벨의 is-picked 클래스로 준다
            var chipLabel = chip.closest(".venue-extra-chip");
            if (chipLabel) chipLabel.classList.toggle("is-picked", chip.checked);

            var row = rowOf(chip);
            if (!row) return;
            if (chip.checked) pickedCount++;

            row.classList.toggle("is-hidden", !chip.checked);
            row.querySelectorAll("input").forEach(function (input) {
                input.disabled = !(groupActive && chip.checked);
            });
        });

        var empty = group.querySelector(".js-extra-empty");
        if (empty) empty.classList.toggle("is-hidden", pickedCount > 0);
    }

    function syncAll() {
        var selectedType = placeTypeSelect ? placeTypeSelect.value : null;
        typeGroups.forEach(function (group) {
            syncGroup(group, group.dataset.placeType === selectedType);
        });
        if (petGroup) {
            syncGroup(petGroup, !!(petToggle && petToggle.checked));
        }
    }

    // 칩 토글 (묶음 구분 없이 위임 처리)
    allGroups.forEach(function (group) {
        group.addEventListener("change", function (e) {
            var chip = e.target.closest(".js-extra-chip");
            if (!chip) return;
            syncAll();
            // 방금 켠 항목은 바로 입력할 수 있게 커서를 옮겨준다
            var row = rowOf(chip);
            if (chip.checked && row) {
                var field = row.querySelector('input[name="extraValues"]');
                if (field) field.focus();
            }
        });

        // 입력칸 우측 × 버튼은 해당 칩을 끄는 것과 같다
        group.addEventListener("click", function (e) {
            var removeBtn = e.target.closest(".js-extra-row-remove");
            if (!removeBtn) return;
            var row = removeBtn.closest(".venue-extra-row");
            if (!row) return;
            var chip = group.querySelector('.js-extra-chip[data-target="' + row.id + '"]');
            if (chip) chip.checked = false;
            syncAll();
        });
    });

    if (placeTypeSelect) placeTypeSelect.addEventListener("change", syncAll);
    if (petToggle) petToggle.addEventListener("change", syncAll);

    // 수정 화면 진입: 이미 저장된 값이 있는 항목의 칩을 켜둔다.
    // (같은 라벨이 여러 업종에 있어도 각 묶음이 자기 입력칸만 보므로 업종별로 알아서 맞는 것만 켜진다)
    allGroups.forEach(function (group) {
        group.querySelectorAll(".js-extra-chip").forEach(function (chip) {
            var row = rowOf(chip);
            if (!row) return;
            var field = row.querySelector('input[name="extraValues"]');
            if (field && field.value.trim() !== "") chip.checked = true;
        });
    });
    if (petGroup && petToggle) {
        petToggle.checked = Array.from(petGroup.querySelectorAll(".js-extra-chip"))
            .some(function (chip) { return chip.checked; });
    }

    syncAll();
});

// ── 해시태그 입력 (등록/수정 폼 ) ──
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".venue-tag-input").forEach(function (wrap) {
        var hidden = wrap.querySelector('input[type="hidden"]');
        var field = wrap.querySelector(".venue-tag-input__field");
        if (!hidden || !field) return;

        var tags = (hidden.value || "").split(",")
            .map(function (tag) { return tag.trim(); })
            .filter(Boolean);

        function render() {
            wrap.querySelectorAll(".venue-tag-input__chip").forEach(function (chip) { chip.remove(); });
            tags.forEach(function (tag) {
                var chip = document.createElement("span");
                chip.className = "venue-tag-input__chip";
                chip.textContent = tag;

                var removeBtn = document.createElement("button");
                removeBtn.type = "button";
                removeBtn.className = "venue-tag-input__chip-remove";
                removeBtn.setAttribute("aria-label", tag + " 태그 삭제");
                removeBtn.textContent = "×";
                removeBtn.addEventListener("click", function () {
                    tags = tags.filter(function (t) { return t !== tag; });
                    render();
                });
                chip.appendChild(removeBtn);

                wrap.insertBefore(chip, field);
            });
            hidden.value = tags.join(",");
        }

        function addTag(raw) {
            var tag = raw.trim();
            if (tag && tags.indexOf(tag) === -1) tags.push(tag);
        }

        //마지막 글자 중복입력 디버깅
        var isComposing = false;
        field.addEventListener("compositionstart", function () { isComposing = true; });
        field.addEventListener("compositionend", function () { isComposing = false; });

        // 콤마/Enter로 직접 입력한 경우: 글자가 필드에 남기 전에 막고 바로 칩으로 반영
        field.addEventListener("keydown", function (e) {
            if (isComposing || e.isComposing || e.keyCode === 229) return;
            if (e.key === "," || e.key === "Enter") {
                e.preventDefault();
                addTag(field.value);
                field.value = "";
                render();
            } else if (e.key === "Backspace" && !field.value && tags.length) {
                tags.pop();
                render();
            }
        });

        field.addEventListener("input", function (e) {
            if (e.isComposing) return;
            if (field.value.indexOf(",") === -1) return;
            var parts = field.value.split(",");
            field.value = parts.pop();
            parts.forEach(addTag);
            render();
        });

        // 콤마/Enter 없이 텍스트만 남긴 채 제출해도 마지막 입력이 태그로 반영되게 한다
        if (field.form) {
            field.form.addEventListener("submit", function () {
                if (field.value.trim()) {
                    addTag(field.value);
                    field.value = "";
                }
                hidden.value = tags.join(",");
            });
        }

        render();
    });
});