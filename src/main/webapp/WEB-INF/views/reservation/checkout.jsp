<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제하기</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Pretendard', 'Malgun Gothic', sans-serif; background: #f5f6f8; }
        .container { max-width: 480px; margin: 60px auto; background: #fff;
                     border-radius: 12px; padding: 32px; box-shadow: 0 2px 12px rgba(0,0,0,.06); }
        h2 { margin-bottom: 20px; font-size: 22px; }
        .info { background: #f8f9fb; border-radius: 8px; padding: 16px; margin-bottom: 24px; font-size: 14px; }
        .info div { display: flex; justify-content: space-between; padding: 4px 0; color: #555; }
        .amount { font-size: 18px; font-weight: 700; color: #111; border-top: 1px solid #e5e7eb;
                  margin-top: 8px; padding-top: 12px !important; }
        .pay-btn { display: block; width: 100%; padding: 15px; margin-bottom: 12px; border: none;
                   border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; }
        .kakao { background: #FEE500; color: #191919; }
        .pay-btn:hover { opacity: .9; }
        .pay-btn:disabled { opacity: .5; cursor: not-allowed; }
        #waiting { display: none; text-align: center; color: #666; font-size: 14px; margin-top: 8px; }
    </style>
</head>
<body>
<div class="container">
    <h2>결제하기</h2>

    <div class="info">
        <div><span>예약번호</span><span>${reservation.reservationId}</span></div>
        <div><span>방문자</span><span>${reservation.visitorName}</span></div>
        <div><span>방문일</span><span>${reservation.visitDate}</span></div>
        <div><span>인원</span><span>${reservation.headcount}명</span></div>
        <div class="amount"><span>결제 금액</span>
            <span><fmt:formatNumber value="${amount}" pattern="#,###"/>원</span></div>
    </div>

    <button type="button" class="pay-btn kakao" id="kakaoBtn">카카오페이로 결제</button>
    <p id="waiting">팝업에서 결제를 진행해 주세요...</p>
</div>

<script>
    var reservationId = "${reservation.reservationId}";
    var popupOptions = "width=500,height=750,left=" + ((screen.width - 500) / 2)
                     + ",top=" + ((screen.height - 750) / 2);

    function lockButtons(lock) {
        document.getElementById("kakaoBtn").disabled = lock;
        document.getElementById("waiting").style.display = lock ? "block" : "none";
    }

    function watchPopup(popup) {
        // 사용자가 팝업을 결제 없이 그냥 닫으면 버튼 다시 활성화
        var timer = setInterval(function () {
            if (!popup || popup.closed) {
                clearInterval(timer);
                lockButtons(false);
            }
        }, 500);
    }

    // 카카오: 팝업을 먼저 열고(차단 방지), ready 응답의 URL로 이동시키기
    document.getElementById("kakaoBtn").addEventListener("click", function () {
        var popup = window.open("about:blank", "kakaoPay", popupOptions);
        if (!popup) { alert("팝업이 차단되었습니다. 팝업을 허용해 주세요."); return; }
        lockButtons(true);
        watchPopup(popup);

        fetch("/payments/kakao/ready/" + reservationId, { method: "POST" })
            .then(function (res) {
                if (!res.ok) throw new Error("ready 실패");
                return res.json();
            })
            .then(function (data) {
                popup.location.href = data.redirectUrl;
            })
            .catch(function () {
                popup.close();
                lockButtons(false);
                alert("카카오페이 결제 준비에 실패했습니다.");
            });
    });
</script>
</body>
</html>
