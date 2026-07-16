<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>처리 중...</title></head>
<body>
<p style="font-family:sans-serif; text-align:center; margin-top:80px;">결제 처리 중입니다...</p>
<script>
    (function () {
        var target = "${target}";
        if (window.opener && !window.opener.closed) {
            // 팝업으로 열린 경우: 부모 창을 이동시키고 팝업 닫기
            window.opener.location.href = target;
            window.close();
        } else {
            // 팝업이 아닌 경우(직접 이동 등): 현재 창에서 이동
            window.location.href = target;
        }
    })();
</script>
</body>
</html>
