<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><jsp:include page="/WEB-INF/views/mypage/common/pageHead.jsp"><jsp:param name="title" value="회원 탈퇴" /></jsp:include></head>
<body>
<main class="mypage-page">
    <h1 class="mypage-page__title">마이페이지</h1>
    <div class="mypage-layout">
        <jsp:include page="/WEB-INF/views/mypage/user/components/sidebar.jsp"><jsp:param name="active" value="withdraw" /></jsp:include>
        <section class="mypage-content mypage-content--form" aria-labelledby="withdraw-title">
            <div class="mypage-content__header"><h2 id="withdraw-title">회원 탈퇴</h2></div>
            <div class="withdraw-card">
                <div class="withdraw-card__icon" aria-hidden="true">!</div>
                <p>탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</p>
                <div class="withdraw-notice"><strong>탈퇴 전 꼭 확인하세요</strong><ul><li>예약 중인 상품은 자동 취소될 수 있습니다.</li><li>찜목록과 후기 등 모든 정보가 삭제됩니다.</li><li>탈퇴 후 동일 이메일로 재가입이 제한될 수 있습니다.</li></ul></div>
                <form action="${pageContext.request.contextPath}/mypage/withdraw" method="post" onsubmit="return confirm('정말 회원 탈퇴하시겠습니까?');">
                    <div class="withdraw-password"><label for="currentPassword">비밀번호 확인</label><input id="currentPassword" type="password" name="currentPassword" placeholder="현재 비밀번호를 입력하세요" required></div>
                    <label class="withdraw-consent"><input type="checkbox" required> <span>위 내용을 확인했으며 탈퇴에 동의합니다.</span></label>
                    <div class="withdraw-card__actions"><a href="${pageContext.request.contextPath}/mypage/info">취소</a><button type="submit">탈퇴하기</button></div>
                </form>
            </div>
        </section>
    </div>
</main>
</body>
</html>
