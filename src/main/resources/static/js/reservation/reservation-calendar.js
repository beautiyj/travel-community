/* =========================================================
   reservation-calendar.js — 예약 폼(reservationForm.jsp)의 방문날짜 캘린더
   - /reservations/availability?placeId=… 로 이미 예약된 날짜 목록을 받아
     예약가능(초록) / 마감(빨강·선택불가) / 지난날(비활성)을 그린다
   - 날짜 선택 시 hidden #visitDate 에 값을 넣고 'change' 이벤트를 쏴서,
     reservation-form.js(요약·검증·임시저장)가 그대로 이어받게 한다 (기존 흐름 재사용)
   - 서버 주입 전역값: window.RESERVATION_PLACE_ID
   ========================================================= */
(function () {
    var placeId = window.RESERVATION_PLACE_ID;
    var hidden  = document.getElementById('visitDate');
    var grid    = document.getElementById('calGrid');
    var title   = document.getElementById('calTitle');
    var prevBtn = document.getElementById('calPrev');
    var nextBtn = document.getElementById('calNext');
    if (!hidden || !grid) return;

    var booked   = {};                     // { 'yyyy-MM-dd': 예약인원합 } — 활성예약이 있는 날만 담김
    var closed   = false;                  // 장소 전체가 휴무로 지정됐는지 (PLACE.is_closed)
    var selected = hidden.value || null;   // 복원된 값이 있으면 유지

    // 숙박만 '1일 1팀'이라 예약된 날을 막는다. 맛집/관광지(tour/food)는 하루에 여러 팀 → 마감 없음(항상 초록)
    var BLOCK_BOOKED = !(window.RESERVATION_PLACE_TYPE === 'tour' || window.RESERVATION_PLACE_TYPE === 'food');

    var today = new Date();
    today.setHours(0, 0, 0, 0);
    var viewYear  = today.getFullYear();
    var viewMonth = today.getMonth();      // 0~11

    // 복원된 선택 날짜가 있으면 그 달부터 보여준다
    if (selected) {
        var p = selected.split('-');
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

    function select(dateStr) {
        selected = dateStr;
        hidden.value = dateStr;
        // reservation-form.js가 이 change를 듣고 요약/검증/임시저장을 처리한다
        hidden.dispatchEvent(new Event('change'));
        render();
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

            if (isPast(viewYear, viewMonth, d)) {
                cell.classList.add('is-past');
                cell.disabled = true;
            } else if (closed || (booked[dateStr] || 0) > 0) {
                cell.classList.add('is-full');       // 휴무 장소이거나 이미 예약 있는 날 = 빨강, 선택 불가 (1일 1팀)
                cell.disabled = true;
            } else {
                cell.classList.add('is-available');  // 예약 가능 = 초록
                (function (ds) {
                    cell.addEventListener('click', function () { select(ds); });
                })(dateStr);
            }

            if (dateStr === selected) cell.classList.add('is-selected');
            grid.appendChild(cell);
        }

        // 이전 달 버튼: 오늘이 속한 달보다 과거로는 못 넘어가게
        prevBtn.disabled = (viewYear === today.getFullYear() && viewMonth === today.getMonth());
    }

    prevBtn.addEventListener('click', function () {
        if (--viewMonth < 0) { viewMonth = 11; viewYear--; }
        render();
    });
    nextBtn.addEventListener('click', function () {
        if (++viewMonth > 11) { viewMonth = 0; viewYear++; }
        render();
    });

    render();   // 우선 한 번 그려서 빈 화면 방지 (마감 현황 로드 전엔 전부 가능)

    // 마감/휴무 현황 로드 (장소 타입 무관 — 휴무는 숙박/맛집/관광지 모두 확인해야 함).
    // 숙박만 예약된 날짜(booked)도 함께 반영, 맛집/관광지는 booked 무시(전부 초록·휴무 시에만 빨강).
    // 실패하면 전부 가능으로 둔다(프론트는 UX용, 최종 방어는 서버)
    if (placeId != null && placeId !== '') {
        fetch('/reservations/availability?placeId=' + placeId)
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (data) {
                if (!data) return;
                if (BLOCK_BOOKED) booked = data.booked || {};
                closed = !!data.closed;
                render();
            })
            .catch(function () { /* 조회 실패 무시 */ });
    }
})();
