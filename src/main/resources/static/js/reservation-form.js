/* =========================================================
   reservation-form.js — 예약 폼(reservationForm.jsp) 로직
   - 서버 값은 JSP에서 window.RESERVATION_UNIT_PRICE 로 주입
   - body 끝에서 로드되어 DOM 준비 후 실행
   ========================================================= */
(function () {
    var UNIT_PRICE = window.RESERVATION_UNIT_PRICE || 0;   // 서버에서 내려준 1인 단가

    var headcountInput = document.getElementById('headcount');
    var amountInput    = document.getElementById('amount');
    var nameInput      = document.getElementById('visitorName');
    var phoneInput     = document.getElementById('phone');
    var dateInput      = document.getElementById('visitDate');
    var submitBtn      = document.getElementById('submitBtn');

    // 인원 변경 시: 서버로 보낼 amount + 요약 카드 갱신
    function updateAmount() {
        var count = parseInt(headcountInput.value, 10);
        amountInput.value = UNIT_PRICE * count;
        document.getElementById('sumPeople').textContent = count + '명';
        document.getElementById('sumUnitCount').textContent = count;
        document.getElementById('sumTotal').textContent = (UNIT_PRICE * count).toLocaleString() + '원';
    }

    // 필수값(이름/연락처/날짜) 다 채워야 결제하기 활성화
    function validate() {
        submitBtn.disabled = !(nameInput.value.trim() && phoneInput.value.trim() && dateInput.value);
    }

    document.getElementById('btnMinus').addEventListener('click', function () {
        var v = parseInt(headcountInput.value, 10);
        if (v > 1) { headcountInput.value = v - 1; updateAmount(); }
    });

    document.getElementById('btnPlus').addEventListener('click', function () {
        var v = parseInt(headcountInput.value, 10);
        if (v < 10) { headcountInput.value = v + 1; updateAmount(); }
    });

    nameInput.addEventListener('input', validate);
    phoneInput.addEventListener('input', validate);
    dateInput.addEventListener('change', function () {
        document.getElementById('sumDate').textContent = this.value || '—';
        validate();
    });

    updateAmount();
    validate();
})();
