<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>숙박 예약</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Pretendard', 'Malgun Gothic', sans-serif; background: #f5f6f8; }
        .wrap { max-width: 480px; margin: 0 auto; padding: 32px 20px; }
        h2 { margin-bottom: 16px; font-size: 20px; }
        form { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 12px rgba(0,0,0,.06); }
        .field { margin-bottom: 18px; }
        .field label, .field-label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
        .field input[type=text], .field input[type=tel], .field input[type=date] {
            width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
        .headcount-box { display: flex; align-items: center; gap: 10px; }
        .headcount-box button { width: 32px; height: 32px; border: 1px solid #ddd; border-radius: 6px;
                                 background: #fff; font-size: 16px; cursor: pointer; }
        .headcount-box input { width: 48px; text-align: center; border: none; font-size: 15px; }
        .actions { display: flex; gap: 10px; margin-top: 24px; }
        .actions button { flex: 1; padding: 14px; border: none; border-radius: 8px; font-size: 15px;
                           font-weight: 600; cursor: pointer; }
        .actions button[type=button] { background: #eee; color: #333; }
        .actions button[type=submit] { background: #3b82f6; color: #fff; }
    </style>
</head>
<body>
<div class="wrap">

    <h2>예약하기</h2>
    <!-- TODO: 숙박/맛집 파트 완성 후 placeId로 조회한 장소 정보(이름/주소/사진) 카드 표시 -->

    <form action="/reservations" method="post" id="reservationForm">

        <!-- 서버로 넘길 식별/금액 값 -->
        <input type="hidden" name="placeId" value="${placeId}">
        <input type="hidden" name="amount" id="amount" value="${price * 2}">

        <div class="field">
            <label for="visitorName">예약자 이름</label>
            <input type="text" id="visitorName" name="visitorName" required maxlength="50">
        </div>

        <div class="field">
            <label for="phone">연락처</label>
            <input type="tel" id="phone" name="phone" required maxlength="20" placeholder="010-0000-0000">
        </div>

        <div class="field">
            <label for="visitDate">체크인 날짜</label>
            <input type="date" id="visitDate" name="visitDate" required>
        </div>

        <div class="field">
            <label for="headcount">인원</label>
            <div class="headcount-box">
                <button type="button" id="btnMinus" aria-label="인원 줄이기">−</button>
                <input type="number" id="headcount" name="headcount" value="2" min="1" max="10" readonly>
                <button type="button" id="btnPlus" aria-label="인원 늘리기">+</button>
                <span>명</span>
            </div>
        </div>

        <div class="actions">
            <button type="button" onclick="history.back()">취소</button>
            <button type="submit">결제하기</button>
        </div>
    </form>
</div>

<script>
    const UNIT_PRICE = parseInt("${price}", 10) || 0;  // 서버에서 내려준 1인 단가
    const headcountInput = document.getElementById('headcount');
    const amountInput = document.getElementById('amount');

    function updateAmount() {
        const count = parseInt(headcountInput.value, 10);
        amountInput.value = UNIT_PRICE * count;   // 인원 바뀔 때마다 amount 갱신
    }

    document.getElementById('btnMinus').addEventListener('click', () => {
        const v = parseInt(headcountInput.value, 10);
        if (v > 1) { headcountInput.value = v - 1; updateAmount(); }
    });

    document.getElementById('btnPlus').addEventListener('click', () => {
        const v = parseInt(headcountInput.value, 10);
        if (v < 10) { headcountInput.value = v + 1; updateAmount(); }
    });

    updateAmount();
</script>
</body>
</html>
