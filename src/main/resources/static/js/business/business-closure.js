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
            + "&isClosed=" + (nextClosed ? 1 : 0);

        fetch(url, { method: "PATCH" })
            .then(function (res) {
                if (!res.ok) throw new Error("요청 실패");
                // 사이드바 상태 표시는 새로고침
                window.location.reload();
            })
            .catch(function () {
                window.alert("마감 상태 변경에 실패했습니다. 다시 시도해주세요.");
                applyState(card, toggle, !nextClosed);
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

        updateSidebarStatus(isClosed);
    }

    // 사이드바 상단 상태(dot + 라벨)는 서버 렌더링 시 한 번만 그려지므로,
    // 토글로 즉시 마감 상태가 바뀔 때 여기서도 같이 갱신해줘야 화면 간 상태가 어긋나지 않는다.
    function updateSidebarStatus(isClosed) {
        var status = document.querySelector(".business-sidebar__status");
        if (!status) return;

        var dot = status.querySelector(".business-status-dot");
        var label = status.querySelector(".business-status-label");
        if (!dot || !label) return;

        dot.classList.toggle("business-status-dot--closed", isClosed);
        dot.classList.toggle("business-status-dot--open", !isClosed);
        label.classList.toggle("business-status-label--closed", isClosed);
        label.classList.toggle("business-status-label--open", !isClosed);
        label.textContent = isClosed ? "예약 마감" : "예약 운영중";
    }

    // ── 날짜별 마감 설정 (달력형 UI) ──
    // PLACE_CLOSED_DATE에 저장한다. 화면 진입 시 GET으로 목록을 불러와 달력에 마킹하고,
    // 날짜(또는 기간)를 클릭/드래그로 선택한 뒤 '마감 설정'을 눌러야 서버에 반영된다.
    // (즉시 마감 토글과 달리 낙관적 갱신을 쓰지 않는다 — 목록은 되돌리기가 번거롭다)
    //
    // 이미 마감된 날짜는 달력에서 클릭을 막는다 — 새로 선택하는 흐름과 "눌러서 삭제"가
    // 같은 클릭에 섞이면 헷갈리므로, 삭제/수정은 하단 목록의 버튼으로만 하도록 분리했다.
    // 단, "수정" 중인 구간의 날짜만은 예외적으로 다시 선택 가능하게 풀어준다 (editingGroup).
    var dateCard = document.querySelector(".business-closure-date-card");
    var calGrid = document.getElementById("closure-cal-grid");
    var calTitle = document.getElementById("closure-cal-title");
    var calPrev = document.getElementById("closure-cal-prev");
    var calNext = document.getElementById("closure-cal-next");
    var selectionLabel = document.getElementById("closure-selection-label");
    var addBtn = document.getElementById("closure-date-add");
    var cancelEditBtn = document.getElementById("closure-edit-cancel");
    var listEl = document.getElementById("closure-date-list");
    var emptyEl = document.getElementById("closure-date-empty");
    // 삭제 확인 모달 (공용 confirmModal.jsp, common.js의 openModal/closeModal로 열고 닫는다).
    // 이 모달은 원래 form-submit 방식이라 실제 삭제(fetch DELETE)는 그 submit을 가로채 처리한다.
    var deleteModal = document.getElementById("closureDeleteModal");
    var deleteForm = deleteModal ? deleteModal.querySelector("form") : null;
    var deleteMessageEl = deleteModal ? deleteModal.querySelector(".modal-message") : null;

    if (dateCard && calGrid && calTitle && calPrev && calNext && selectionLabel && addBtn
            && cancelEditBtn && listEl && emptyEl) {
        var placeId = dateCard.dataset.placeId;
        var closedDates = [];
        // "삭제" 확인 모달에서 확정을 누르면 실제로 지울 대상 (목록 × 버튼을 누른 시점에 채워진다)
        var pendingDelete = null;

        function pad2(n) {
            return n < 10 ? "0" + n : String(n);
        }

        function toIso(date) {
            return date.getFullYear() + "-" + pad2(date.getMonth() + 1) + "-" + pad2(date.getDate());
        }

        var today = new Date();
        today.setHours(0, 0, 0, 0);
        var todayStr = toIso(today);

        // 달력에 표시 중인 달(1일 기준). 달을 넘겨도 선택 상태는 유지된다.
        var viewDate = new Date(today.getFullYear(), today.getMonth(), 1);
        // 첫 번째로 고른 날짜(구간 시작). rangeEnd가 없으면 아직 "종료일 선택 대기" 상태다.
        var rangeStart = null;
        var rangeEnd = null;
        // 수정 중인 구간(목록의 "수정" 버튼으로 진입). null이면 평소의 "새로 추가" 모드다.
        var editingGroup = null;

        // 드래그 선택 상태. mousedown~mouseup 사이 실제로 다른 칸으로 이동했는지(dragMoved)로
        // "그냥 클릭"과 "드래그"를 구분한다 (같은 칸에서 누르고 뗀 경우는 click 이벤트로만 처리).
        var isDragging = false;
        var dragAnchor = null;
        var dragMoved = false;

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
                    renderCalendar();
                })
                .catch(function () {
                    window.alert("마감 날짜를 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.");
                });
        }

        // 하루 단위로 저장된 날짜들을 연속 구간으로 묶는다. 정렬된 배열을 앞에서부터 훑다가
        // 전날+1일이 아니면 새 구간을 시작한다. 한 번에 등록했든 따로 등록했든 연속이면 묶어 보여준다
        // — 사용자 입장에서는 "8/29, 8/30이 이어져 있다"가 중요하지 어떻게 저장됐는지는 안 중요하다.
        function groupConsecutiveDates(dates) {
            var groups = [];
            dates.forEach(function (date) {
                var last = groups[groups.length - 1];
                if (last) {
                    var next = new Date(last.end + "T00:00:00");
                    next.setDate(next.getDate() + 1);
                    if (toIso(next) === date) {
                        last.end = date;
                        return;
                    }
                }
                groups.push({ start: date, end: date });
            });
            return groups;
        }

        function renderDateList() {
            listEl.innerHTML = "";
            var groups = groupConsecutiveDates(closedDates);

            groups.forEach(function (group) {
                var isRange = group.start !== group.end;

                var row = document.createElement("div");
                row.className = "business-closure-date-row";
                row.dataset.start = group.start;
                row.dataset.end = group.end;

                var info = document.createElement("div");
                info.className = "business-closure-date-row__info";

                var dot = document.createElement("span");
                dot.className = "business-status-dot business-status-dot--closed";

                var dateLabel = document.createElement("span");
                dateLabel.className = "business-closure-date-row__date";
                dateLabel.textContent = isRange
                    ? formatKoreanDate(group.start) + " ~ " + formatKoreanDate(group.end)
                        + " (" + dayCount(group.start, group.end) + "일)"
                    : formatKoreanDate(group.start);

                var tag = document.createElement("span");
                tag.className = "business-closure-date-row__tag";
                tag.textContent = "예약 마감";

                info.appendChild(dot);
                info.appendChild(dateLabel);
                info.appendChild(tag);

                var actions = document.createElement("div");
                actions.className = "business-closure-date-row__actions";

                var editBtn = document.createElement("button");
                editBtn.type = "button";
                editBtn.className = "business-closure-date-row__edit";
                editBtn.textContent = "수정";

                var removeBtn = document.createElement("button");
                removeBtn.type = "button";
                removeBtn.className = "business-closure-date-row__remove";
                removeBtn.setAttribute("aria-label", "마감 날짜 삭제");
                removeBtn.textContent = "×";

                actions.appendChild(editBtn);
                actions.appendChild(removeBtn);

                row.appendChild(info);
                row.appendChild(actions);
                listEl.appendChild(row);
            });

            emptyEl.style.display = groups.length === 0 ? "" : "none";
        }

        // 선택 구간에 포함되는지: 시작일만 골랐으면 그 하루만, 종료일까지 골랐으면 사이 날짜 전부
        function isInSelection(iso) {
            if (!rangeStart) return false;
            if (!rangeEnd) return iso === rangeStart;
            return iso >= rangeStart && iso <= rangeEnd;
        }

        // 수정 중인 구간에 속한 날짜인지 (원래 구간 범위 기준 — 이 날짜들만 예외적으로 선택 가능)
        function isEditingGroupDate(iso) {
            return editingGroup && iso >= editingGroup.start && iso <= editingGroup.end;
        }

        function formatKoreanDate(iso) {
            var parts = iso.split("-");
            return parseInt(parts[1], 10) + "월 " + parseInt(parts[2], 10) + "일";
        }

        // 두 ISO 날짜 사이의 일수(양끝 포함). "T00:00:00"을 붙여 로컬 자정으로 파싱한다
        // (안 붙이면 브라우저가 UTC 자정으로 해석해 시간대에 따라 하루가 밀릴 수 있다).
        function dayCount(startIso, endIso) {
            var start = new Date(startIso + "T00:00:00");
            var end = new Date(endIso + "T00:00:00");
            return Math.round((end - start) / 86400000) + 1;
        }

        function updateSelectionLabel() {
            if (!rangeStart) {
                selectionLabel.textContent = editingGroup
                    ? "새로 마감할 날짜(또는 기간)를 선택하세요"
                    : "달력에서 마감할 날짜(또는 기간)를 선택하세요";
                addBtn.disabled = true;
            } else if (!rangeEnd) {
                selectionLabel.textContent = formatKoreanDate(rangeStart) + " — 종료일을 선택하세요 (같은 날을 다시 누르면 하루만 마감)";
                addBtn.disabled = true;
            } else {
                selectionLabel.textContent = formatKoreanDate(rangeStart)
                    + (rangeEnd !== rangeStart ? " ~ " + formatKoreanDate(rangeEnd) : "")
                    + " (" + dayCount(rangeStart, rangeEnd) + "일)";
                addBtn.disabled = false;
            }
            addBtn.textContent = editingGroup ? "수정 완료" : "마감 설정";
            cancelEditBtn.style.display = editingGroup ? "" : "none";
        }

        function renderCalendar() {
            var year = viewDate.getFullYear();
            var month = viewDate.getMonth();
            calTitle.textContent = year + "년 " + (month + 1) + "월";

            var firstWeekday = new Date(year, month, 1).getDay();
            var daysInMonth = new Date(year, month + 1, 0).getDate();

            calGrid.innerHTML = "";

            for (var i = 0; i < firstWeekday; i++) {
                var empty = document.createElement("span");
                empty.className = "business-closure-calendar__day business-closure-calendar__day--empty";
                calGrid.appendChild(empty);
            }

            for (var day = 1; day <= daysInMonth; day++) {
                var iso = toIso(new Date(year, month, day));
                var btn = document.createElement("button");
                btn.type = "button";
                btn.className = "business-closure-calendar__day";
                btn.textContent = String(day);
                btn.dataset.date = iso;

                var isPast = iso < todayStr;
                // 수정 중인 구간의 날짜는 실제로는 closedDates에 남아있어도 선택 가능하게 풀어준다
                var isClosed = !isEditingGroupDate(iso) && closedDates.indexOf(iso) !== -1;

                if (isPast) {
                    btn.disabled = true;
                    btn.classList.add("is-past");
                } else if (isClosed) {
                    // 이미 마감된 날짜는 새로 선택하는 대상에서 빼고, 지우거나 수정하려면 아래 목록을 쓰게 한다
                    btn.disabled = true;
                    btn.classList.add("is-closed");
                } else {
                    if (iso === todayStr) btn.classList.add("is-today");
                    if (isInSelection(iso)) btn.classList.add("is-selected");
                }

                calGrid.appendChild(btn);
            }
        }

        // 클릭(또는 드래그 없는 mousedown+mouseup)으로 하루씩 골라 나가는 기존 방식
        function handleDayClick(iso) {
            if (!rangeStart || rangeEnd || iso < rangeStart) {
                // 새 선택 시작: 아직 아무것도 안 골랐거나, 이전 선택이 이미 확정됐거나,
                // 현재 시작일보다 이른 날짜를 눌렀을 때(그 날짜부터 다시 고르게 한다)
                rangeStart = iso;
                rangeEnd = null;
            } else {
                // 시작일과 같은 날을 다시 누르면 하루짜리 구간으로 확정된다
                rangeEnd = iso;
            }
            renderCalendar();
            updateSelectionLabel();
        }

        function exitEditMode() {
            editingGroup = null;
            rangeStart = null;
            rangeEnd = null;
            renderCalendar();
            updateSelectionLabel();
        }

        calGrid.addEventListener("click", function (e) {
            // 드래그로 방금 구간을 확정한 직후에 뒤따라오는 click은 무시한다
            // (mousedown~mouseup이 서로 다른 칸이면 이 click은 애초에 안 뜨지만,
            //  드래그하다 다시 시작 칸으로 돌아와서 뗀 경우엔 뜨므로 방어적으로 한 번 더 막는다)
            if (dragMoved) { dragMoved = false; return; }

            var cell = e.target.closest(".business-closure-calendar__day");
            if (!cell || cell.disabled || !cell.dataset.date) return;
            handleDayClick(cell.dataset.date);
        });

        // ── 드래그로 기간 선택 ──
        calGrid.addEventListener("mousedown", function (e) {
            var cell = e.target.closest(".business-closure-calendar__day");
            if (!cell || cell.disabled || !cell.dataset.date) return;
            isDragging = true;
            dragAnchor = cell.dataset.date;
            dragMoved = false;
        });

        calGrid.addEventListener("mouseover", function (e) {
            if (!isDragging) return;
            var cell = e.target.closest(".business-closure-calendar__day");
            if (!cell || cell.disabled || !cell.dataset.date) return;
            var iso = cell.dataset.date;
            if (iso === dragAnchor) return;

            dragMoved = true;
            rangeStart = iso < dragAnchor ? iso : dragAnchor;
            rangeEnd = iso < dragAnchor ? dragAnchor : iso;
            renderCalendar();
            updateSelectionLabel();
        });

        // 그리드 밖에서 손을 뗄 수도 있으므로 document 전체에서 mouseup을 받는다.
        //
        // dragMoved는 여기서 바로 지우지 않고 setTimeout(0)으로 미룬다: 드래그가 시작 칸과
        // 다른 칸에서 끝나면 애초에 click이 안 뜨니 상관없지만, 드래그하다 시작 칸으로 되돌아와
        // 뗀 경우엔 mouseup 바로 뒤에 같은 칸에서 click이 동기적으로 뒤따라온다 — 그 click의
        // 가드(위 calGrid click 핸들러)가 아직 dragMoved=true를 보고 무시할 수 있어야
        // 방금 드래그로 정해둔 범위가 그 click 때문에 다시 초기화되지 않는다.
        // 반대로 여기서 바로 지우면, 드래그가 다른 칸에서 끝난 경우 dragMoved=true가 계속 남아
        // 있다가 완전히 별개인 "다음 클릭" 한 번을 조용히 무시해버리는 문제가 생긴다.
        document.addEventListener("mouseup", function () {
            isDragging = false;
            dragAnchor = null;
            setTimeout(function () { dragMoved = false; }, 0);
        });

        calPrev.addEventListener("click", function () {
            viewDate.setMonth(viewDate.getMonth() - 1);
            renderCalendar();
        });

        calNext.addEventListener("click", function () {
            viewDate.setMonth(viewDate.getMonth() + 1);
            renderCalendar();
        });

        cancelEditBtn.addEventListener("click", exitEditMode);

        addBtn.addEventListener("click", function () {
            if (!rangeStart || !rangeEnd) return;

            addBtn.disabled = true;
            var request;
            if (editingGroup) {
                var params = "&oldStartDate=" + encodeURIComponent(editingGroup.start)
                    + "&oldEndDate=" + encodeURIComponent(editingGroup.end)
                    + "&newStartDate=" + encodeURIComponent(rangeStart)
                    + "&newEndDate=" + encodeURIComponent(rangeEnd);
                request = fetch(datesUrl(params), { method: "PUT" });
            } else {
                var addParams = "&startDate=" + encodeURIComponent(rangeStart) + "&endDate=" + encodeURIComponent(rangeEnd);
                request = fetch(datesUrl(addParams), { method: "POST" });
            }

            request
                .then(function (res) {
                    // 90일 초과/역순 구간 등 검증 실패는 서버가 400 + 이유 문구를 그대로 내려준다
                    if (!res.ok) return res.text().then(function (msg) { throw new Error(msg); });
                    editingGroup = null;
                    rangeStart = null;
                    rangeEnd = null;
                    updateSelectionLabel();
                    loadDateList();
                })
                .catch(function (err) {
                    window.alert(err && err.message
                        ? err.message
                        : (editingGroup ? "마감 기간 수정에 실패했습니다. 다시 시도해주세요." : "마감 날짜 추가에 실패했습니다. 다시 시도해주세요."));
                    addBtn.disabled = false;
                });
        });

        listEl.addEventListener("click", function (e) {
            var row = e.target.closest(".business-closure-date-row");
            if (!row) return;
            var start = row.dataset.start;
            var end = row.dataset.end;

            var editBtn = e.target.closest(".business-closure-date-row__edit");
            if (editBtn) {
                editingGroup = { start: start, end: end };
                rangeStart = start;
                rangeEnd = end;
                renderCalendar();
                updateSelectionLabel();
                return;
            }

            var removeBtn = e.target.closest(".business-closure-date-row__remove");
            if (!removeBtn || removeBtn.disabled) return;

            // 바로 지우지 않고 확인 모달을 띄운다. 실제 삭제는 모달의 폼 submit(가로채서 처리)에서 실행
            if (!deleteModal || !deleteForm) {
                window.alert("삭제 확인 창을 열 수 없습니다. 새로고침 후 다시 시도해주세요.");
                return;
            }
            pendingDelete = { start: start, end: end };
            if (deleteMessageEl) {
                var label = start === end
                    ? formatKoreanDate(start)
                    : formatKoreanDate(start) + " ~ " + formatKoreanDate(end) + " (" + dayCount(start, end) + "일)";
                deleteMessageEl.textContent = label + " 마감을 삭제하시겠습니까?";
            }
            window.openModal("closureDeleteModal");
        });

        // 삭제 확인 모달의 "삭제" 버튼(폼 submit)을 가로채 fetch(DELETE)로 처리한다.
        // confirmModal.jsp는 원래 실제 폼 제출(페이지 이동) 방식이라, AJAX 삭제에 맞게 여기서 막는다.
        if (deleteForm) {
            deleteForm.addEventListener("submit", function (e) {
                e.preventDefault();
                if (!pendingDelete) return;

                var start = pendingDelete.start;
                var end = pendingDelete.end;
                var submitBtn = deleteForm.querySelector(".modal-btn-confirm button");
                if (submitBtn) submitBtn.disabled = true;

                var delParams = "&startDate=" + encodeURIComponent(start) + "&endDate=" + encodeURIComponent(end);
                fetch(datesUrl(delParams), { method: "DELETE" })
                    .then(function (res) {
                        if (!res.ok) throw new Error("삭제 실패");
                        window.closeModal("closureDeleteModal");
                        pendingDelete = null;
                        // 지우려던 구간을 마침 수정 중이었다면 편집 상태도 함께 정리한다
                        if (editingGroup && editingGroup.start === start && editingGroup.end === end) {
                            exitEditMode();
                        }
                        loadDateList();
                    })
                    .catch(function () {
                        window.alert("마감 날짜 삭제에 실패했습니다. 다시 시도해주세요.");
                    })
                    .finally(function () {
                        if (submitBtn) submitBtn.disabled = false;
                    });
            });
        }

        renderCalendar();
        updateSelectionLabel();
        loadDateList();
    }
});
