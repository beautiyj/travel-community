<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제 실패</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/reservation.css">
    <link rel="stylesheet" href="/css/components/smallButton.css">
</head>
<body>
<div class="result-card">
    <div class="result-icon fail">&#10005;</div>
    <h2>결제에 실패했습니다</h2>
    <p class="desc">${message}</p>

    <div class="result-actions">
        <jsp:include page="/WEB-INF/views/common/smallButton.jsp">
            <jsp:param name="text" value="이전으로 돌아가기" />
            <jsp:param name="width" value="100%" />
            <jsp:param name="theme" value="secondary" />
            <jsp:param name="onclick" value="history.back()" />
        </jsp:include>
    </div>
</div>
</body>
</html>
