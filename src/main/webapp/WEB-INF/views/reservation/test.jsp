<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>예약/결제 테스트</title>
    <link rel="stylesheet" href="/css/reservation-test.css">
</head>
<body>
<div class="wrap">
    <h2>예약/결제 테스트</h2>
    <p class="sub">reservation + 카카오페이 흐름 수동 테스트용 페이지 (개발 전용)</p>

    <!-- 1. 예약 폼 -->
    <div class="card">
        <h3>1. 예약 폼 열기</h3>
        <p>타입별로 예약 폼 이동 → 폼 제출하면 예약 생성 후 결제 페이지로 넘어감</p>
        <p class="sub">숙박=정가(인원×단가) · 맛집/관광지=예약금(만나서결제) 흐름을 각각 테스트</p>
        <p class="sub">타입마다 placeId를 다르게 둠 — 같은 placeId를 쓰면 회원+장소+날짜 중복체크가 타입 구분 없이 걸려
            "이미 예약된 날짜"로 막힘(실제로는 place마다 타입이 고정이라 안 생기는 문제, 테스트만 이렇게 나눔)</p>
        <p class="sub">장소 타입(tour/food/stay)은 이제 PLACE.place_type 조회로 정해짐 — placeId 1=stay, 2=food, 3=tour로 로컬 DB에 맞춰둠</p>
        <div class="row">
            <input type="number" id="placeIdStay" value="1" min="1" title="숙박 placeId">
            <button type="button" onclick="goForm('placeIdStay')">숙박(정가)</button>
        </div>
        <div class="row">
            <input type="number" id="placeIdFood" value="2" min="1" title="맛집 placeId">
            <button type="button" onclick="goForm('placeIdFood')">맛집(예약금)</button>
        </div>
        <div class="row">
            <input type="number" id="placeIdTour" value="3" min="1" title="관광지 placeId">
            <button type="button" onclick="goForm('placeIdTour')">관광지(예약금)</button>
        </div>
        <div class="row">
            <input type="number" id="placeIdFree" value="4" min="1" title="무료 테스트 placeId">
            <button type="button" onclick="goForm('placeIdFree', true)">무료(0원, 결제 없이 즉시완료)</button>
        </div>
    </div>

    <!-- 2. 결제 페이지 -->
    <div class="card">
        <h3>2. 결제 페이지 열기</h3>
        <p>이미 만들어진 예약(PENDING 상태)의 결제 페이지로 바로 이동</p>
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
        <p>POST /payments/{paymentId}/cancel — 결제수단별 취소(토스/카카오는 PG 취소 API 호출, 무통장·가상카드·무료는 상태만 전환) + 예약 '예약취소' 전환</p>
        <div class="row">
            <input type="number" id="cancelPaymentId" placeholder="paymentId" min="1">
            <button type="button" class="danger" onclick="cancelPayment()">결제 취소</button>
        </div>
    </div>

    <!-- 5. 예약 목록 -->
    <div class="card">
        <h3>5. 내 예약 목록 (memberId=1) <a class="refresh" href="/reservations/test">새로고침</a></h3>
        <p>예약 생성/결제/취소/만료 후 상태 변화를 여기서 확인</p>
        <div class="table-scroll">
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
                    <td>${r.visitDate}<c:if test="${not empty r.checkOutDate}"> ~ ${r.checkOutDate}</c:if></td>
                    <td>${r.headcount}</td>
                    <td>
                        <span class="status
                            <c:choose>
                                <c:when test="${r.status == 'PENDING'}">s-pending</c:when>
                                <c:when test="${r.status == 'PAID'}">s-paid</c:when>
                                <c:when test="${r.status == 'CANCELED'}">s-canceled</c:when>
                                <c:otherwise>s-expired</c:otherwise>
                            </c:choose>">${r.status.label}</span>
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
</div>

<script src="/js/reservation/reservation-test.js"></script>
</body>
</html>
