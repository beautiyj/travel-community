/* =========================================================
   reservation-form.js — 예약 폼(reservationForm.jsp) 로직
   - 서버 값은 JSP에서 window.RESERVATION_UNIT_PRICE 로 주입
   - body 끝에서 로드되어 DOM 준비 후 실행
   - 입력값은 sessionStorage에 저장돼 뒤로가기·새로고침 시 복원된다
   ========================================================= */
(function () {
    var UNIT_PRICE = window.RESERVATION_UNIT_PRICE || 0;   // 서버에서 내려준 1인 단가
    var DRAFT_KEY  = 'reservationDraft';                   // 임시 입력값 저장 키

    // tour(관광지)·food(맛집) = 만나서결제(예약금 정액, 인원 무관). 그 외 = 정가
    var PAY_ON_SITE = (window.RESERVATION_PLACE_TYPE === 'tour' || window.RESERVATION_PLACE_TYPE === 'food');
    var DEPOSIT     = window.RESERVATION_DEPOSIT || 0;
    // 숙박만 기간 예약(박수 × 인원 × 단가). 맛집/관광지·무료는 당일 방문
    var IS_STAY     = !PAY_ON_SITE && window.RESERVATION_PLACE_TYPE !== 'free';

    var headcountInput = document.getElementById('headcount');
    var amountInput    = document.getElementById('amount');
    var nameInput      = document.getElementById('visitorName');
    var phoneInput     = document.getElementById('phone');
    var phoneError     = document.getElementById('phoneError');
    var dateInput      = document.getElementById('visitDate');
    var checkOutInput  = document.getElementById('checkOutDate');
    var submitBtn      = document.getElementById('submitBtn');

    /** 체크인·체크아웃 차이(박). 숙박이 아니거나 아직 기간이 안 잡혔으면 1 */
    function nights() {
        if (!IS_STAY || !dateInput.value || !checkOutInput || !checkOutInput.value) return 1;
        var diff = (new Date(checkOutInput.value) - new Date(dateInput.value)) / 86400000;
        return diff > 0 ? Math.round(diff) : 1;
    }

    // 한국 휴대폰 번호(010/011/016~019, 하이픈 제외 10~11자리) 형식 검사
    var PHONE_RE = /^01[016789]\d{7,8}$/;

    // 숫자만 남기고 010-1234-5678 형태로 자동 하이픈
    function formatPhone(value) {
        var d = value.replace(/\D/g, '').slice(0, 11);
        if (d.length < 4)  return d;
        if (d.length < 7)  return d.slice(0, 3) + '-' + d.slice(3);
        if (d.length <= 10) return d.slice(0, 3) + '-' + d.slice(3, 6) + '-' + d.slice(6);
        return d.slice(0, 3) + '-' + d.slice(3, 7) + '-' + d.slice(7);
    }

    function isPhoneValid() {
        return PHONE_RE.test(phoneInput.value.replace(/\D/g, ''));
    }

    // 입력값을 sessionStorage에 저장 (입력할 때마다 호출)
    // 날짜(visitDate/checkOutDate)는 일부러 저장하지 않는다 — 새로고침 사이 관리자가 마감시켜도
    // 복원된 날짜로 결제하기가 눌리는 걸 막기 위해, 날짜는 매번 캘린더에서 다시 고르게 한다
    function saveDraft() {
        sessionStorage.setItem(DRAFT_KEY, JSON.stringify({
            visitorName: nameInput.value,
            phone:       phoneInput.value,
            headcount:   headcountInput.value
        }));
    }

    // 저장된 입력값을 폼에 복원 (페이지 로드 시 1회). 뒤로가기·새로고침 대응. 날짜는 복원 대상 아님(위 saveDraft 참고)
    function restoreDraft() {
        var raw = sessionStorage.getItem(DRAFT_KEY);
        if (!raw) return;
        try {
            var d = JSON.parse(raw);
            if (d.visitorName) nameInput.value      = d.visitorName;
            if (d.phone)       phoneInput.value     = d.phone;
            if (d.headcount)   headcountInput.value = d.headcount;
        } catch (e) { /* 손상된 데이터는 무시 */ }
    }

    // 인원·기간 변경 시: 서버로 보낼 amount + 요약 카드 갱신
    // 만나서결제면 예약금 정액(인원 무관), 숙박이면 박수 × 인원 × 단가
    function updateAmount() {
        var count = parseInt(headcountInput.value, 10);
        var n = nights();
        var total = PAY_ON_SITE ? DEPOSIT : UNIT_PRICE * count * n;
        amountInput.value = total;
        document.getElementById('sumPeople').textContent = count + '명';

        var unitCount = document.getElementById('sumUnitCount');    // 정가일 때만 존재하는 요소
        if (unitCount) unitCount.textContent = count;
        var unitNights = document.getElementById('sumUnitNights');  // 숙박일 때만 존재
        if (unitNights) unitNights.textContent = n;
        var sumNights = document.getElementById('sumNights');       // 숙박일 때만 존재
        if (sumNights) {
            sumNights.textContent = (checkOutInput && checkOutInput.value) ? n + '박' : '—';
        }

        document.getElementById('sumTotal').textContent = total.toLocaleString() + '원';
    }

    /** 'yyyy-MM-dd' -> {y, m, d} 숫자 파싱 */
    function parseIso(iso) {
        var p = iso.split('-');
        return { y: parseInt(p[0], 10), m: parseInt(p[1], 10), d: parseInt(p[2], 10) };
    }

    /** 요약의 날짜 표기. 체크인은 항상 "YYYY.M.d", 체크아웃은 체크인과 같은 해면 "M.d",
     *  해가 넘어가면 "YY.M.d"(두 자리 연도)로 표시해 구분한다. 그 외(숙박 아님)는 "YYYY.M.d" 하나만. */
    function updateDateLabel() {
        var label = '—';
        if (dateInput.value) {
            var inD = parseIso(dateInput.value);
            label = inD.y + '.' + inD.m + '.' + inD.d;
            if (IS_STAY) {
                if (checkOutInput && checkOutInput.value) {
                    var outD = parseIso(checkOutInput.value);
                    var outLabel = (outD.y === inD.y)
                        ? outD.m + '.' + outD.d
                        : String(outD.y % 100).padStart(2, '0') + '.' + outD.m + '.' + outD.d;
                    label += '~' + outLabel + '(' + (nights() + 1) + '일)';
                } else {
                    label += ' ~ 체크아웃 선택';
                }
            }
        }
        document.getElementById('sumDate').textContent = label;
    }

    // 맛집·관광지 정원 초과 여부. 숙박은 정원 체크 대상이 아니라 항상 통과
    function isWithinCapacity() {
        if (!PAY_ON_SITE || !dateInput.value || !window.RESERVATION_REMAINING_SEATS) return true;
        var count = parseInt(headcountInput.value, 10) || 0;
        return count <= window.RESERVATION_REMAINING_SEATS(dateInput.value);
    }

    // 필수값(이름/날짜) + 연락처 형식까지 통과해야 결제하기 활성화
    function validate() {
        var phoneOk = isPhoneValid();
        // 값이 있는데 형식이 틀릴 때만 에러 노출 (빈 칸은 조용히 비활성)
        var showErr = phoneInput.value.trim() && !phoneOk;
        phoneError.style.display = showErr ? 'block' : 'none';
        phoneInput.classList.toggle('input-invalid', showErr);
        // 숙박은 체크아웃까지 골라야 결제로 넘어갈 수 있다
        var dateOk = dateInput.value && (!IS_STAY || (checkOutInput && checkOutInput.value));
        submitBtn.disabled = !(nameInput.value.trim() && phoneOk && dateOk && isWithinCapacity());
    }

    document.getElementById('btnMinus').addEventListener('click', function () {
        var v = parseInt(headcountInput.value, 10);
        if (v > 1) { headcountInput.value = v - 1; updateAmount(); validate(); saveDraft(); }
    });

    document.getElementById('btnPlus').addEventListener('click', function () {
        var v = parseInt(headcountInput.value, 10);
        if (v < 10) { headcountInput.value = v + 1; updateAmount(); validate(); saveDraft(); }
    });

    nameInput.addEventListener('input', function () { validate(); saveDraft(); });
    phoneInput.addEventListener('input', function () {
        this.value = formatPhone(this.value);   // 입력 즉시 하이픈 자동 삽입
        validate(); saveDraft();
    });
    // 캘린더가 날짜(체크인/체크아웃)를 채운 뒤 이 change를 쏜다 — 기간이 바뀌면 금액도 다시 계산
    dateInput.addEventListener('change', function () {
        updateDateLabel();
        updateAmount();
        validate();
        saveDraft();
    });

    // 임시 저장값은 결제 "완료"(success.jsp)에서 제거한다.
    // submit(결제 단계로 이동) 시점에 지우면, 결제를 취소하고 돌아왔을 때 값이 사라진다.

    // ---- 초기화: 저장값 복원 후 요약/버튼 상태를 맞춘다 ----
    restoreDraft();
    updateDateLabel();
    updateAmount();
    validate();
})();
