/* =========================================================
   reservation-calendar.js — 예약 폼(reservationForm.jsp)의 방문날짜 캘린더
   - /reservations/availability?placeId=… 로 이미 예약된 날짜 목록을 받아
     예약가능(초록) / 마감(빨강·선택불가) / 지난날(비활성)을 그린다
   - 숙박(stay): 체크인·체크아웃 두 번 클릭으로 기간 선택, 또는 마우스로 끌어서(드래그) 한 번에 선택 (#visitDate, #checkOutDate)
     맛집/관광지·무료: 기존대로 날짜 하나만 선택 (#visitDate)
   - 날짜 선택 시 hidden에 값을 넣고 'change' 이벤트를 쏴서,
     reservation-form.js(요약·검증·임시저장)가 그대로 이어받게 한다 (기존 흐름 재사용)
   - 서버 주입 전역값: window.RESERVATION_PLACE_ID, window.RESERVATION_PLACE_TYPE
   ========================================================= */
(function () {
    var placeId  = window.RESERVATION_PLACE_ID;
    var hidden   = document.getElementById('visitDate');
    var hiddenTo = document.getElementById('checkOutDate');
    var grid     = document.getElementById('calGrid');
    var title    = document.getElementById('calTitle');
    var prevBtn  = document.getElementById('calPrev');
    var nextBtn  = document.getElementById('calNext');
    if (!hidden || !grid) return;

    var placeType = window.RESERVATION_PLACE_TYPE;
    var PAY_ON_SITE = (placeType === 'tour' || placeType === 'food');
    // 숙박만 기간(체크인~체크아웃) 예약. 맛집/관광지·무료는 당일 방문이라 날짜 하나만 고른다
    var RANGE_MODE = !PAY_ON_SITE && placeType !== 'free';
    // 숙박만 '1팀 단독'이라 예약된 날을 막는다. 맛집/관광지는 하루에 여러 팀 → 마감 없음(항상 초록)
    var BLOCK_BOOKED = !PAY_ON_SITE;

    var booked      = {};                    // { 'yyyy-MM-dd': 예약인원합 } — 활성예약이 있는 날만 담김
    var closed      = false;                 // 장소 전체가 휴무로 지정됐는지 (PLACE.is_closed)
    var closedDates = {};                    // { 'yyyy-MM-dd': true } — 날짜별 마감(PLACE_CLOSED_DATE)
    var capacity = 0;                        // 맛집·관광지 하루 정원(표시용). 0이면 표시 안 함
    var checkIn  = hidden.value || null;     // 복원된 값이 있으면 유지
    var checkOut = (hiddenTo && hiddenTo.value) || null;

    // 숙박 기간 드래그 선택용 상태 (마우스 누른 채로 끌어서 체크인~체크아웃 한 번에 선택)
    var dragging   = false;
    var dragMoved  = false;   // 실제로 다른 날짜로 넘어갔는지(=드래그). false면 일반 클릭이라 onPick에 맡긴다
    var dragAnchor = null;
    var dragEnd    = null;

    var today = new Date();
    today.setHours(0, 0, 0, 0);
    var viewYear  = today.getFullYear();
    var viewMonth = today.getMonth();        // 0~11

    // 복원된 선택 날짜가 있으면 그 달부터 보여준다
    if (checkIn) {
        var p = checkIn.split('-');
        viewYear  = parseInt(p[0], 10);
        viewMonth = parseInt(p[1], 10) - 1;
    }

    function pad(n) { return n < 10 ? '0' + n : '' + n; }
    function ymd(y, m, d) { return y + '-' + pad(m + 1) + '-' + pad(d); }

    function isPast(y, m, d) {
        var cell = new Date(y, m, d);
        cell.setHours(0, 0, 0, 0);
        return cell < today;
    }

    /** 'yyyy-MM-dd' 문자열은 사전순 비교가 곧 날짜순 비교라 그대로 대소 비교한다 */
    function isBlocked(dateStr) {
        return closed || !!closedDates[dateStr] || (booked[dateStr] || 0) > 0;
    }

    /** 체크인~체크아웃 사이(반개구간)에 마감일이 끼어 있으면 그 기간은 잡을 수 없다 */
    function hasBlockedBetween(fromStr, toStr) {
        var d = new Date(fromStr);
        var end = new Date(toStr);
        for (; d < end; d.setDate(d.getDate() + 1)) {
            if (isBlocked(ymd(d.getFullYear(), d.getMonth(), d.getDate()))) return true;
        }
        return false;
    }

    /** 범례에 선택한 날짜의 남은 자리를 보여준다 (맛집·관광지 전용, 표시용) */
    function updateSeatInfo() {
        var box = document.getElementById('calSeatInfo');
        if (!box || capacity <= 0) return;
        if (!checkIn) {
            box.textContent = '날짜를 선택하세요';
            return;
        }
        var left = capacity - (booked[checkIn] || 0);
        if (left < 0) left = 0;
        box.textContent = '남은 자리 ' + left + '/' + capacity;
    }

    /** 드래그 중 미리보기: DOM을 다시 그리지 않고 이미 있는 셀들의 선택 클래스만 갱신한다 */
    function previewRange(fromStr, toStr) {
        var cells = grid.querySelectorAll('.rsv-cal-day[data-date]');
        for (var i = 0; i < cells.length; i++) {
            var ds = cells[i].getAttribute('data-date');
            cells[i].classList.remove('is-selected', 'is-range-start', 'is-range-end', 'is-in-range');
            if (ds === fromStr) {
                cells[i].classList.add('is-selected', 'is-range-start');
            } else if (toStr && ds === toStr) {
                cells[i].classList.add('is-range-end');
            } else if (toStr && ds > fromStr && ds < toStr) {
                cells[i].classList.add('is-in-range');
            }
        }
    }

    /** hidden 값을 갱신하고 reservation-form.js가 듣는 change를 쏜다 */
    function apply() {
        hidden.value = checkIn || '';
        if (hiddenTo) hiddenTo.value = checkOut || '';
        hidden.dispatchEvent(new Event('change'));
        render();
    }

    function onPick(dateStr) {
        if (!RANGE_MODE) {          // 당일 방문: 그냥 하나만 선택
            checkIn = dateStr;
            apply();
            return;
        }

        // 아직 체크인이 없거나, 이미 기간이 완성됐거나, 체크인보다 이전을 누르면 → 체크인부터 다시
        if (!checkIn || checkOut || dateStr <= checkIn) {
            checkIn = dateStr;
            checkOut = null;
            apply();
            return;
        }

        // 체크인~체크아웃 사이에 마감일이 끼면 그 날을 새 체크인으로 삼는다
        if (hasBlockedBetween(checkIn, dateStr)) {
            checkIn = dateStr;
            checkOut = null;
            apply();
            return;
        }

        checkOut = dateStr;
        apply();
    }

    function render() {
        title.textContent = viewYear + '년 ' + (viewMonth + 1) + '월';
        grid.innerHTML = '';

        var firstDow    = new Date(viewYear, viewMonth, 1).getDay();     // 0=일요일
        var daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();

        // 1일 앞의 빈 칸
        for (var i = 0; i < firstDow; i++) {
            var blank = document.createElement('span');
            blank.className = 'rsv-cal-day is-blank';
            grid.appendChild(blank);
        }

        for (var d = 1; d <= daysInMonth; d++) {
            var dateStr = ymd(viewYear, viewMonth, d);
            var cell = document.createElement('button');
            cell.type = 'button';
            cell.className = 'rsv-cal-day';
            cell.textContent = d;
            cell.setAttribute('data-date', dateStr);

            // 체크아웃 당일은 다음 손님이 체크인할 수 있으므로(반개구간) 마감으로 치지 않는다
            // 휴무(closed)·날짜별 마감(closedDates)은 장소 타입 무관하게 항상 막는다.
            // 예약된 날짜(booked)만 숙박(BLOCK_BOOKED)에서만 막는다 — 맛집/관광지는 하루 여러 팀 가능.
            var blockedHere = closed || !!closedDates[dateStr] || (BLOCK_BOOKED && (booked[dateStr] || 0) > 0);

            if (isPast(viewYear, viewMonth, d)) {
                cell.classList.add('is-past');
                cell.disabled = true;
            } else if (blockedHere) {
                cell.classList.add('is-full');       // 휴무 장소이거나 이미 예약 있는 날 = 빨강, 선택 불가
                cell.disabled = true;
            } else {
                cell.classList.add('is-available');  // 예약 가능 = 초록
                (function (ds) {
                    cell.addEventListener('click', function () { onPick(ds); });
                    if (RANGE_MODE) {
                        // 마우스를 누른 채 끌면 체크인~체크아웃을 한 번에 선택. 그냥 클릭(안 끌면)은 위 click(onPick)이 처리
                        cell.addEventListener('mousedown', function () {
                            dragging = true;
                            dragMoved = false;
                            dragAnchor = ds;
                            dragEnd = null;
                        });
                        cell.addEventListener('mouseenter', function () {
                            if (!dragging || ds === dragAnchor) return;
                            dragMoved = true;
                            if (ds > dragAnchor && !hasBlockedBetween(dragAnchor, ds)) {
                                dragEnd = ds;
                                previewRange(dragAnchor, ds);
                            }
                        });
                    }
                })(dateStr);
            }

            // 선택 상태 표시: 체크인 / 사이 구간 / 체크아웃. 마감이면 선택 표시를 얹지 않는다(마감이 우선)
            // — 예: 새로고침으로 복원된 날짜를 그 사이 관리자가 마감시킨 경우, 파랑(선택)이 아니라 빨강(마감)으로 보여야 한다
            if (!blockedHere && dateStr === checkIn) {
                cell.classList.add('is-selected');
                if (RANGE_MODE && checkOut) cell.classList.add('is-range-start');
            } else if (!blockedHere && RANGE_MODE && checkOut) {
                if (dateStr === checkOut) cell.classList.add('is-range-end');
                else if (dateStr > checkIn && dateStr < checkOut) cell.classList.add('is-in-range');
            }

            grid.appendChild(cell);
        }

        // 이전 달 버튼: 오늘이 속한 달보다 과거로는 못 넘어가게
        prevBtn.disabled = (viewYear === today.getFullYear() && viewMonth === today.getMonth());

        updateSeatInfo();
    }

    prevBtn.addEventListener('click', function () {
        if (--viewMonth < 0) { viewMonth = 11; viewYear--; }
        render();
    });
    nextBtn.addEventListener('click', function () {
        if (++viewMonth > 11) { viewMonth = 0; viewYear++; }
        render();
    });

    // 드래그 종료: 실제로 다른 날짜까지 끌었을 때만 확정한다. 제자리 클릭이면 그냥 두고 click(onPick)이 처리하게 둔다
    document.addEventListener('mouseup', function () {
        if (!dragging) return;
        dragging = false;
        if (dragMoved && dragEnd) {
            checkIn = dragAnchor;
            checkOut = dragEnd;
            apply();
        }
        dragAnchor = null;
        dragEnd = null;
    });

    render();   // 우선 한 번 그려서 빈 화면 방지 (마감 현황 로드 전엔 전부 가능)

    // 마감/휴무 현황 로드 (장소 타입 무관 — 휴무는 숙박/맛집/관광지 모두 확인해야 함).
    // 숙박만 예약된 날짜(booked)도 함께 반영, 맛집/관광지는 booked 무시(전부 초록·휴무 시에만 빨강).
    // 실패하면 전부 가능으로 둔다(프론트는 UX용, 최종 방어는 서버)
    function loadAvailability() {
        if (placeId == null || placeId === '') return;
        fetch('/reservations/availability?placeId=' + placeId)
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (data) {
                if (!data) return;
                // 숙박은 마감 판정에, 맛집/관광지는 남은 자리 표시에 booked를 쓴다
                booked = data.booked || {};
                closed = !!data.closed;
                closedDates = {};
                (data.closedDates || []).forEach(function (d) { closedDates[d] = true; });
                if (!BLOCK_BOOKED) capacity = data.capacity || 0;
                render();
                // 이미 선택된 날짜가 있으면(복원값) 방금 받은 정원 정보로 결제하기 버튼 상태를 다시 계산시킨다
                if (checkIn) hidden.dispatchEvent(new Event('change'));
            })
            .catch(function () { /* 조회 실패 무시 */ });
    }

    loadAvailability();   // 최초 로드

    // 탭이 백그라운드에 있다가 다시 보이게 될 때(포커스 복귀) 마감 현황 재조회
    document.addEventListener('visibilitychange', function () {
        if (document.visibilityState === 'visible') loadAvailability();
    });

    /** 맛집·관광지 정원 체크용: 해당 날짜에 남은 자리 수. 숙박(정원 표시 없음)은 항상 무제한 취급 */
    window.RESERVATION_REMAINING_SEATS = function (dateStr) {
        if (capacity <= 0) return Infinity;
        return Math.max(0, capacity - (booked[dateStr] || 0));
    };
})();
