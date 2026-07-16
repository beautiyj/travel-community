<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제 실패</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Pretendard', 'Malgun Gothic', sans-serif; background: #f5f6f8; }
        .container { max-width: 480px; margin: 60px auto; background: #fff; text-align: center;
                     border-radius: 12px; padding: 40px 32px; box-shadow: 0 2px 12px rgba(0,0,0,.06); }
        .x { width: 64px; height: 64px; margin: 0 auto 16px; border-radius: 50%;
             background: #ef4444; color: #fff; font-size: 30px; line-height: 64px; }
        h2 { margin-bottom: 12px; font-size: 22px; }
        p { color: #666; margin-bottom: 28px; font-size: 14px; }
        a.btn { display: block; padding: 14px; border-radius: 8px; background: #6b7280;
                color: #fff; font-weight: 600; text-decoration: none; }
    </style>
</head>
<body>
<div class="container">
    <div class="x">&#10005;</div>
    <h2>결제에 실패했습니다</h2>
    <p>${message}</p>
    <a class="btn" href="javascript:history.back()">이전으로 돌아가기</a>
</div>
</body>
</html>
