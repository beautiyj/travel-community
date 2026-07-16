<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>예약/결제 테스트</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Pretendard', 'Malgun Gothic', sans-serif; background: #f5f6f8; }
        .wrap { max-width: 720px; margin: 0 auto; padding: 32px 20px; }
        h2 { margin-bottom: 4px; font-size: 22px; }
        .sub { color: #888; font-size: 13px; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px;
                box-shadow: 0 2px 12px rgba(0,0,0,.06); }
        .card h3 { font-size: 15px; margin-bottom: 4px; }
        .card p { font-size: 12px; color: #888; margin-bottom: 12px; }
        .row { display: flex; gap: 8px; }
        .row input { flex: 1; padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
        .row button { padding: 9px 18px; border: none; border-radius: 8px; background: #3b82f6;
                      color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; white-space: nowrap; }
        .row button.danger { background: #ef4444; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { padding: 8px 6px; border-bottom: 1px solid #eee; text-align: left; }
        th { color: #888; font-weight: 600; }
        .status { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
        .s-pending  { background: #fef3c7; color: #92400e; }
        .s-paid     { background: #dcfce7; color: #166534; }
        .s-canceled { background: #fee2e2; color: #991b1b; }
        .s-expired  { background: #e5e7eb; color: #4b5563; }
        .empty { color: #aaa; text-align: center; padding: 16px 0; }
        a.refresh { font-size: 12px; color: #3b82f6; text-decoration: none; float: right; }
    </style>
</head>
<body>
<div class="wrap">
    <h2>예약/결제 테스트</h2>
    <p class="sub">reservation + 카카오페이 흐름 수동 테스트용 페이지 (개발 전용)</p>

    <!-- 1. 예약 폼 -->
    <div class="card">
        <h3>1. 예약 폼 열기</h3>
        <p>placeId를 넣고 예약 폼으로 이동 → 폼 제출하면 예약 생성 후 결제 페이지로 넘어감</p>
        <div class="row">
            <input type="number" id="placeId" value="1" min="1">
            <button type="button" onclick="location.href='/reservations/new?placeId=' + document.getElementById('placeId').value">
                예약 폼으로 이동
            </button>
        </div>
    </div>

    <!-- 2. 결제 페이지 -->
    <div class="card">
        <h3>2. 결제 페이지 열기</h3>
        <p>이미 만들어진 예약(예약중 상태)의 결제 페이지로 바로 이동</p>
        <div class="row">
            <input type="number" id="reservationId" placeholder="reservationId" min="1">
            <button type="button" onclick="goCheckout()">결제 페이지로 이동</button>
        </div>
    </div>

    <!-- 3. 결제 완료 페이지 -->
    <div class="card">
        <h3>3. 결제 완료 페이지 열기</h3>
        <p>결제 완료된 건의 완료 화면 확인 (paymentId 기준)</p>
        <div class="row">
            <input type="number" id="completePaymentId" placeholder="paymentId" min="1">
            <button type="button" onclick="goComplete()">완료 페이지로 이동</button>
        </div>
    </div>

    <!-- 4. 결제 취소 -->
    <div class="card">
        <h3>4. 결제 취소(환불) API 호출</h3>
        <p>POST /payments/{paymentId}/cancel — 카카오페이 취소 + 예약 '예약취소' 전환</p>
        <div class="row">
            <input type="number" id="cancelPaymentId" placeholder="paymentId" min="1">
            <button type="button" class="danger" onclick="cancelPayment()">결제 취소</button>
        </div>
    </div>

    <!-- 5. 예약 목록 -->
    <div class="card">
        <h3>5. 내 예약 목록 (memberId=1) <a class="refresh" href="/reservations/test">새로고침</a></h3>
        <p>예약 생성/결제/취소/만료 후 상태 변화를 여기서 확인</p>
        <table>
            <thead>
            <tr>
                <th>예약ID</th><th>placeId</th><th>방문자</th><th>방문일</th><th>인원</th><th>상태</th><th>생성일시</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="r" items="${reservations}">
                <tr>
                    <td>${r.reservationId}</td>
                    <td>${r.placeId}</td>
                    <td>${r.visitorName}</td>
                    <td>${r.visitDate}</td>
                    <td>${r.headcount}</td>
                    <td>
                        <span class="status
                            <c:choose>
                                <c:when test="${r.status == '예약중'}">s-pending</c:when>
                                <c:when test="${r.status == '예약완료'}">s-paid</c:when>
                                <c:when test="${r.status == '예약취소'}">s-canceled</c:when>
                                <c:otherwise>s-expired</c:otherwise>
                            </c:choose>">${r.status}</span>
                    </td>
                    <td>${r.createdAt}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty reservations}">
                <tr><td colspan="7" class="empty">예약이 없습니다. 1번에서 예약을 만들어 보세요.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<script>
    function goCheckout() {
        var id = document.getElementById('reservationId').value;
        if (!id) { alert('reservationId를 입력하세요.'); return; }
        location.href = '/payments/checkout/' + id;
    }

    function goComplete() {
        var id = document.getElementById('completePaymentId').value;
        if (!id) { alert('paymentId를 입력하세요.'); return; }
        location.href = '/payments/complete/' + id;
    }

    function cancelPayment() {
        var id = document.getElementById('cancelPaymentId').value;
        if (!id) { alert('paymentId를 입력하세요.'); return; }
        if (!confirm(id + '번 결제를 취소할까요?')) return;
        fetch('/payments/' + id + '/cancel', { method: 'POST' })
            .then(function (res) {
                if (!res.ok) throw new Error();
                alert('취소되었습니다.');
                location.reload();
            })
            .catch(function () { alert('취소 실패 (이미 취소됐거나 없는 결제)'); });
    }
</script>
</body>
</html>
