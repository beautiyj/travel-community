// 마감 관리: 즉시 예약 마감 토글(.business-closure-card 안의 .business-toggle) 공용 스크립트.
// 카드가 여러 개 있어도(향후 재사용 대비) 동작하도록 이벤트 위임으로 처리한다.
// 클릭 시 먼저 화면을 낙관적으로 바꾸고, API 실패 시에만 원래 상태로 되돌린다.
document.addEventListener("DOMContentLoaded", function () {
    document.addEventListener("click", function (e) {
        var toggle = e.target.closest(".business-toggle");
        if (!toggle || toggle.disabled) return;

        var card = toggle.closest(".business-closure-card");
        if (!card) return;

        var placeId = card.dataset.placeId;
        var nextClosed = !toggle.classList.contains("is-on");

        toggle.disabled = true;
        applyState(card, toggle, nextClosed);

        // memberId는 서버가 로그인 세션에서 파생하므로 더 이상 보내지 않는다
        var url = "/api/business/place/closed"
            + "?placeId=" + encodeURIComponent(placeId)
            + "&isClosed=" + nextClosed;

        fetch(url, { method: "PATCH" })
            .then(function (res) {
                if (!res.ok) throw new Error("요청 실패");
            })
            .catch(function () {
                window.alert("마감 상태 변경에 실패했습니다. 다시 시도해주세요.");
                applyState(card, toggle, !nextClosed);
            })
            .finally(function () {
                toggle.disabled = false;
            });
    });

    function applyState(card, toggle, isClosed) {
        toggle.classList.toggle("is-on", isClosed);
        toggle.setAttribute("aria-pressed", String(isClosed));

        var banner = card.querySelector(".business-closure-banner");
        var dot = banner.querySelector(".business-status-dot");
        var text = banner.querySelector(".business-closure-banner__text");

        banner.classList.toggle("business-closure-banner--closed", isClosed);
        banner.classList.toggle("business-closure-banner--open", !isClosed);
        dot.classList.toggle("business-status-dot--closed", isClosed);
        dot.classList.toggle("business-status-dot--open", !isClosed);
        text.textContent = isClosed
            ? "현재 예약 마감 상태입니다. 신규 예약이 차단됩니다."
            : "예약을 정상적으로 받고 있습니다.";
    }

    // ── 날짜별 마감 설정 ──
    // PLACE_CLOSED_DATE 테이블에 저장한다. 화면 진입 시 GET으로 목록을 불러오고,
    // 추가/삭제는 POST/DELETE 후 서버 응답이 성공했을 때만 화면에 반영한다.
    // (즉시 마감 토글과 달리 낙관적 갱신을 쓰지 않는다 — 목록은 되돌리기가 번거롭다)
    var dateCard = document.querySelector(".business-closure-date-card");
    var dateInput = document.getElementById("closure-date-input");
    var addBtn = document.getElementById("closure-date-add");
    var listEl = document.getElementById("closure-date-list");
    var emptyEl = document.getElementById("closure-date-empty");

    if (dateCard && dateInput && addBtn && listEl && emptyEl) {
        var placeId = dateCard.dataset.placeId;
        var closedDates = [];

        // 지난 날짜는 서버에서도 거부하므로 달력에서 아예 못 고르게 막는다
        dateInput.min = new Date().toISOString().slice(0, 10);

        function datesUrl(extraParams) {
            var url = "/api/business/place/closed-dates?placeId=" + encodeURIComponent(placeId);
            return extraParams ? url + extraParams : url;
        }

        function loadDateList() {
            fetch(datesUrl())
                .then(function (res) {
                    if (!res.ok) throw new Error("조회 실패");
                    return res.json();
                })
                .then(function (dates) {
                    closedDates = dates;
                    renderDateList();
                })
                .catch(function () {
                    window.alert("마감 날짜를 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.");
                });
        }

        function renderDateList() {
            listEl.innerHTML = "";
            closedDates.forEach(function (date) {
                var row = document.createElement("div");
                row.className = "business-closure-date-row";
                row.dataset.date = date;

                var info = document.createElement("div");
                info.className = "business-closure-date-row__info";

                var dot = document.createElement("span");
                dot.className = "business-status-dot business-status-dot--closed";

                var dateLabel = document.createElement("span");
                dateLabel.className = "business-closure-date-row__date";
                dateLabel.textContent = date;

                var tag = document.createElement("span");
                tag.className = "business-closure-date-row__tag";
                tag.textContent = "예약 마감";

                info.appendChild(dot);
                info.appendChild(dateLabel);
                info.appendChild(tag);

                var removeBtn = document.createElement("button");
                removeBtn.type = "button";
                removeBtn.className = "business-closure-date-row__remove";
                removeBtn.setAttribute("aria-label", "마감 날짜 삭제");
                removeBtn.textContent = "×";

                row.appendChild(info);
                row.appendChild(removeBtn);
                listEl.appendChild(row);
            });

            emptyEl.style.display = closedDates.length === 0 ? "" : "none";
        }

        dateInput.addEventListener("input", function () {
            addBtn.disabled = !dateInput.value;
        });

        addBtn.addEventListener("click", function () {
            var value = dateInput.value;
            if (!value || closedDates.indexOf(value) !== -1) return;

            addBtn.disabled = true;
            fetch(datesUrl("&closedDate=" + encodeURIComponent(value)), { method: "POST" })
                .then(function (res) {
                    if (!res.ok) throw new Error("추가 실패");
                    dateInput.value = "";
                    loadDateList();
                })
                .catch(function () {
                    window.alert("마감 날짜 추가에 실패했습니다. 다시 시도해주세요.");
                    addBtn.disabled = false;
                });
        });

        listEl.addEventListener("click", function (e) {
            var removeBtn = e.target.closest(".business-closure-date-row__remove");
            if (!removeBtn || removeBtn.disabled) return;
            var row = removeBtn.closest(".business-closure-date-row");
            var date = row.dataset.date;

            removeBtn.disabled = true;
            fetch(datesUrl("&closedDate=" + encodeURIComponent(date)), { method: "DELETE" })
                .then(function (res) {
                    if (!res.ok) throw new Error("삭제 실패");
                    loadDateList();
                })
                .catch(function () {
                    window.alert("마감 날짜 삭제에 실패했습니다. 다시 시도해주세요.");
                    removeBtn.disabled = false;
                });
        });

        loadDateList();
    }
});
