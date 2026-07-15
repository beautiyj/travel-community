<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제 완료</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Pretendard', 'Malgun Gothic', sans-serif; background: #f5f6f8; }
        .container { max-width: 480px; margin: 60px auto; background: #fff; text-align: center;
                     border-radius: 12px; padding: 40px 32px; box-shadow: 0 2px 12px rgba(0,0,0,.06); }
        .check { width: 64px; height: 64px; margin: 0 auto 16px; border-radius: 50%;
                 background: #22c55e; color: #fff; font-size: 34px; line-height: 64px; }
        h2 { margin-bottom: 24px; font-size: 22px; }
        .info { background: #f8f9fb; border-radius: 8px; padding: 16px; margin-bottom: 24px;
                font-size: 14px; text-align: left; }
        .info div { display: flex; justify-content: space-between; padding: 4px 0; color: #555; }
        a.btn, button.btn { display: block; width: 100%; padding: 14px; border-radius: 8px; border: none;
                font-size: 15px; font-weight: 600; text-decoration: none; cursor: pointer; margin-bottom: 10px; }
        .primary { background: #3b82f6; color: #fff; }
        .danger  { background: #fff; color: #ef4444; border: 1px solid #ef4444 !important; }
    </style>
</head>
<body>
<div class="container">
    <div class="check">&#10003;</div>
    <h2>결제가 완료되었습니다</h2>

    <div class="info">
        <div><span>예약번호</span><span>${payment.reservationId}</span></div>
        <div><span>주문번호</span><span>${payment.orderId}</span></div>
        <div><span>결제수단</span><span>${method}</span></div>
        <div><span>결제금액</span>
            <span><fmt:formatNumber value="${payment.amount}" pattern="#,###"/>원</span></div>
    </div>

    <a class="btn primary" href="/mypage/reservations">내 예약 확인하기</a>
    <button class="btn danger" id="cancelBtn">결제 취소(환불)</button>
</div>

<script>
    document.getElementById("cancelBtn").addEventListener("click", function () {
        if (!confirm("정말 결제를 취소하시겠습니까?")) return;
        fetch("/payments/${payment.paymentId}/cancel", { method: "POST" })
            .then(function (res) {
                if (!res.ok) throw new Error();
                alert("결제가 취소되었습니다.");
                location.href = "/payments/failed?message=" + encodeURIComponent("결제가 취소(환불)되었습니다.");
            })
            .catch(function () { alert("취소에 실패했습니다."); });
    });
</script>
</body>
</html>
