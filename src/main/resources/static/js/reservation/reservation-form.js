/* =========================================================
   reservation-form.js — 예약 폼(reservationForm.jsp) 로직
   - 서버 값은 JSP에서 window.RESERVATION_UNIT_PRICE 로 주입
   - body 끝에서 로드되어 DOM 준비 후 실행
   - 입력값은 sessionStorage에 저장돼 뒤로가기·새로고침 시 복원된다
   ========================================================= */
(function () {
    var UNIT_PRICE = window.RESERVATION_UNIT_PRICE || 0;   // 서버에서 내려준 1인 단가
    var DRAFT_KEY  = 'reservationDraft';                   // 임시 입력값 저장 키

    var headcountInput = document.getElementById('headcount');
    var amountInput    = document.getElementById('amount');
    var nameInput      = document.getElementById('visitorName');
    var phoneInput     = document.getElementById('phone');
    var phoneError     = document.getElementById('phoneError');
    var dateInput      = document.getElementById('visitDate');
    var submitBtn      = document.getElementById('submitBtn');

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
    function saveDraft() {
        sessionStorage.setItem(DRAFT_KEY, JSON.stringify({
            visitorName: nameInput.value,
            phone:       phoneInput.value,
            visitDate:   dateInput.value,
            headcount:   headcountInput.value
        }));
    }

    // 저장된 입력값을 폼에 복원 (페이지 로드 시 1회). 뒤로가기·새로고침 대응
    function restoreDraft() {
        var raw = sessionStorage.getItem(DRAFT_KEY);
        if (!raw) return;
        try {
            var d = JSON.parse(raw);
            if (d.visitorName) nameInput.value      = d.visitorName;
            if (d.phone)       phoneInput.value     = d.phone;
            if (d.visitDate)   dateInput.value      = d.visitDate;
            if (d.headcount)   headcountInput.value = d.headcount;
        } catch (e) { /* 손상된 데이터는 무시 */ }
    }

    // 인원 변경 시: 서버로 보낼 amount + 요약 카드 갱신
    function updateAmount() {
        var count = parseInt(headcountInput.value, 10);
        amountInput.value = UNIT_PRICE * count;
        document.getElementById('sumPeople').textContent = count + '명';
        document.getElementById('sumUnitCount').textContent = count;
        document.getElementById('sumTotal').textContent = (UNIT_PRICE * count).toLocaleString() + '원';
    }

    // 필수값(이름/날짜) + 연락처 형식까지 통과해야 결제하기 활성화
    function validate() {
        var phoneOk = isPhoneValid();
        // 값이 있는데 형식이 틀릴 때만 에러 노출 (빈 칸은 조용히 비활성)
        var showErr = phoneInput.value.trim() && !phoneOk;
        phoneError.style.display = showErr ? 'block' : 'none';
        phoneInput.classList.toggle('input-invalid', showErr);
        submitBtn.disabled = !(nameInput.value.trim() && phoneOk && dateInput.value);
    }

    document.getElementById('btnMinus').addEventListener('click', function () {
        var v = parseInt(headcountInput.value, 10);
        if (v > 1) { headcountInput.value = v - 1; updateAmount(); saveDraft(); }
    });

    document.getElementById('btnPlus').addEventListener('click', function () {
        var v = parseInt(headcountInput.value, 10);
        if (v < 10) { headcountInput.value = v + 1; updateAmount(); saveDraft(); }
    });

    nameInput.addEventListener('input', function () { validate(); saveDraft(); });
    phoneInput.addEventListener('input', function () {
        this.value = formatPhone(this.value);   // 입력 즉시 하이픈 자동 삽입
        validate(); saveDraft();
    });
    dateInput.addEventListener('change', function () {
        document.getElementById('sumDate').textContent = this.value || '—';
        validate(); saveDraft();
    });

    // 임시 저장값은 결제 "완료"(success.jsp)에서 제거한다.
    // submit(결제 단계로 이동) 시점에 지우면, 결제를 취소하고 돌아왔을 때 값이 사라진다.

    // ---- 초기화: 저장값 복원 후 요약/버튼 상태를 맞춘다 ----
    restoreDraft();
    document.getElementById('sumDate').textContent = dateInput.value || '—';
    updateAmount();
    validate();
})();
