<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>처리 중...</title></head>
<body>
<p style="font-family:sans-serif; text-align:center; margin-top:80px;">결제 처리 중입니다...</p>

<%-- 서버값(이동 목적지) 주입 후 외부 JS 로드 --%>
<script>
    var bridgeTarget = "${target}";
</script>
<script src="/js/payment-bridge.js"></script>
</body>
</html>
