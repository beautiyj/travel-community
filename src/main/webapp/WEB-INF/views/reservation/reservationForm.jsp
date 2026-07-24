<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>예약하기</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/components/inputField.css">
    <link rel="stylesheet" href="/css/reservation.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="page-wrap">

    <a href="javascript:history.back()" class="back-link">&lsaquo; 상세페이지로</a>
    <h1 class="page-title">예약하기</h1>

    <form action="/reservations" method="post" id="reservationForm">

        <!-- 서버로 넘길 식별/금액 값 -->
        <input type="hidden" name="placeId" value="${placeId}">
        <input type="hidden" name="amount" id="amount" value="${price * 2}">

        <div class="booking-grid">

            <!-- ─── 왼쪽: 입력 폼 ─── -->
            <div>
                <!-- TODO: 숙박/맛집 파트 완성 후 placeId로 조회한 장소 정보(이름/지역/사진/평점)로 교체 -->
                <div class="place-card">
                    <div class="thumb">&#127756;</div>
                    <div>
                        <p class="name">장소 #${placeId}</p>
                        <p class="desc">장소 정보 연동 예정</p>
                    </div>
                </div>

                <jsp:include page="/WEB-INF/views/common/inputField.jsp">
                    <jsp:param name="label" value="예약자 이름" />
                    <jsp:param name="name" value="visitorName" />
                    <jsp:param name="id" value="visitorName" />
                    <jsp:param name="placeholder" value="이름을 입력하세요" />
                    <jsp:param name="maxlength" value="50" />
                    <jsp:param name="required" value="true" />
                </jsp:include>

                <jsp:include page="/WEB-INF/views/common/inputField.jsp">
                    <jsp:param name="label" value="연락처" />
                    <jsp:param name="name" value="phone" />
                    <jsp:param name="id" value="phone" />
                    <jsp:param name="type" value="tel" />
                    <jsp:param name="placeholder" value="010-0000-0000" />
                    <jsp:param name="maxlength" value="20" />
                    <jsp:param name="required" value="true" />
                </jsp:include>
                <p class="field-error" id="phoneError" role="alert">올바른 휴대폰 번호를 입력하세요. (예: 010-1234-5678)</p>

                <div class="field">
                    <label for="visitDate">방문 날짜</label>
                    <div class="input-box">
                        <span class="icon">&#128197;</span>
                        <input type="date" id="visitDate" name="visitDate" required>
                    </div>
                </div>

                <div class="field">
                    <label for="headcount">인원</label>
                    <div class="input-box">
                        <span class="icon">&#128101;</span>
                        <button type="button" class="stepper-btn" id="btnMinus" aria-label="인원 줄이기">&minus;</button>
                        <input type="number" class="stepper-value" id="headcount" name="headcount"
                               value="2" min="1" max="10" readonly>
                        <button type="button" class="stepper-btn" id="btnPlus" aria-label="인원 늘리기">+</button>
                        <span class="stepper-unit">명</span>
                    </div>
                </div>

                <div class="btn-row">
                    <button type="button" class="btn btn-outline" onclick="history.back()">취소</button>
                    <button type="submit" class="btn btn-primary" id="submitBtn" disabled>결제하기</button>
                </div>
            </div>

            <!-- ─── 오른쪽: 예약 요약 ─── -->
            <div class="summary-card">
                <h3>예약 요약</h3>
                <div class="summary-row"><span class="label">숙소/장소</span><span class="value">장소 #${placeId}</span></div>
                <div class="summary-row"><span class="label">날짜</span><span class="value" id="sumDate">&mdash;</span></div>
                <div class="summary-row"><span class="label">인원</span><span class="value" id="sumPeople">2명</span></div>
                <div class="summary-row summary-divider">
                    <span class="label">금액</span>
                    <span class="value"><fmt:formatNumber value="${price}" pattern="#,###"/>원 &times; <span id="sumUnitCount">2</span>명</span>
                </div>
                <div class="summary-total"><span>합계</span><span id="sumTotal"></span></div>
            </div>

        </div>
    </form>
</div>

<!-- 서버 값 주입 후 외부 JS 로드 -->
<script>
    window.RESERVATION_UNIT_PRICE = ${price};   // 서버에서 내려준 1인 단가
</script>
<script src="/js/reservation/reservation-form.js"></script>
</body>
</html>
