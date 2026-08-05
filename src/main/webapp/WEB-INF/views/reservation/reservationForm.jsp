<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- tour(관광지)·food(맛집) = 만나서결제(예약금), 그 외(숙박 등) = 정가 --%>
<c:set var="payOnSite" value="${placeType eq 'tour' or placeType eq 'food'}" />
<%-- 숙박만 기간(체크인~체크아웃) 예약. 맛집/관광지·무료는 당일 방문 --%>
<c:set var="isStay" value="${not payOnSite and placeType ne 'free'}" />
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>예약하기</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/components/inputField.css">
    <link rel="stylesheet" href="/css/reservation/reservation.css">
    <link rel="stylesheet" href="/css/reservation/reservation-calendar.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="page-wrap">

    <a href="javascript:history.back()" class="back-link">&lsaquo; 상세페이지로</a>
    <h1 class="page-title">예약하기</h1>

    <form action="/reservations" method="post" id="reservationForm">

        <!-- 서버로 넘길 식별/금액 값. placeType은 서버가 PLACE 조회로 다시 판단하므로 폼으로 넘기지 않는다 -->
        <input type="hidden" name="placeId" value="${placeId}">
        <input type="hidden" name="freeTest" value="${placeType eq 'free'}">
        <input type="hidden" name="amount" id="amount" value="${payOnSite ? deposit : price * 2}">

        <div class="booking-grid">

            <!-- ─── 왼쪽: 입력 폼 ─── -->
            <div>
                <div class="place-card">
                    <c:choose>
                        <c:when test="${not empty placeImage}">
                            <img class="thumb" src="${fn:escapeXml(placeImage)}" alt="${fn:escapeXml(placeName)} 대표 이미지">
                        </c:when>
                        <c:otherwise>
                            <div class="thumb">&#127756;</div>
                        </c:otherwise>
                    </c:choose>
                    <div>
                        <p class="name">${fn:escapeXml(placeName)}</p>
                        <p class="desc">${not empty placeAddress ? fn:escapeXml(placeAddress) : '장소 정보 연동 예정'}</p>
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
                    <label>${isStay ? '숙박 기간' : '방문 날짜'}</label>
                    <c:if test="${isStay}">
                        <p class="cal-hint">체크인 날짜와 체크아웃 날짜를 차례로 선택하세요.</p>
                    </c:if>
                    <%-- 예약 가능/마감을 색으로 보여주는 캘린더. 이미 예약된 날(숙박만)은 빨강·선택 불가 --%>
                    <div class="rsv-cal" id="rsvCalendar">
                        <div class="rsv-cal-head">
                            <button type="button" class="rsv-cal-nav" id="calPrev" aria-label="이전 달">&lsaquo;</button>
                            <span class="rsv-cal-title" id="calTitle"></span>
                            <button type="button" class="rsv-cal-nav" id="calNext" aria-label="다음 달">&rsaquo;</button>
                        </div>
                        <div class="rsv-cal-weekdays">
                            <span>일</span><span>월</span><span>화</span><span>수</span><span>목</span><span>금</span><span>토</span>
                        </div>
                        <div class="rsv-cal-grid" id="calGrid"></div>
                        <div class="rsv-cal-legend">
                            <span><i class="dot ok"></i> 예약 가능</span>
                            <span><i class="dot full"></i> 마감</span>
                            <%-- 맛집·관광지: 선택한 날짜의 남은 자리를 캘린더가 여기 채운다 --%>
                            <c:if test="${payOnSite}">
                                <span class="seat-info" id="calSeatInfo">날짜를 선택하세요</span>
                            </c:if>
                        </div>
                    </div>
                    <%-- 선택된 날짜는 캘린더가 이 hidden에 채운다. 서버 전송·검증은 이 값 사용 --%>
                    <input type="hidden" id="visitDate" name="visitDate" required>
                    <%-- 숙박만 사용(체크아웃). 맛집/관광지는 빈 값으로 전송돼 서버에서 null 처리 --%>
                    <input type="hidden" id="checkOutDate" name="checkOutDate">
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
                <div class="summary-row"><span class="label">숙소/장소</span><span class="value">${fn:escapeXml(placeName)}</span></div>
                <div class="summary-row"><span class="label">${isStay ? '기간' : '날짜'}</span><span class="value" id="sumDate">&mdash;</span></div>
                <c:if test="${isStay}">
                    <div class="summary-row"><span class="label">숙박</span><span class="value" id="sumNights">&mdash;</span></div>
                </c:if>
                <div class="summary-row"><span class="label">인원</span><span class="value" id="sumPeople">2명</span></div>
                <c:choose>
                    <c:when test="${payOnSite}">
                        <%-- 만나서 결제: 예약금만 선결제, 나머지는 현장 --%>
                        <div class="summary-row"><span class="label">결제 방식</span><span class="value">현장 결제(만나서 결제)</span></div>
                        <div class="summary-row summary-divider">
                            <span class="label">예약금</span>
                            <span class="value"><fmt:formatNumber value="${deposit}" pattern="#,###"/>원</span>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="summary-row summary-divider">
                            <span class="label">금액</span>
                            <span class="value"><fmt:formatNumber value="${price}" pattern="#,###"/>원
                                <c:if test="${isStay}">&times; <span id="sumUnitNights">1</span>박</c:if>
                                &times; <span id="sumUnitCount">2</span>명</span>
                        </div>
                    </c:otherwise>
                </c:choose>
                <div class="summary-total"><span>합계</span><span id="sumTotal"></span></div>
            </div>

        </div>
    </form>
</div>

<!-- 서버 값 주입 후 외부 JS 로드 -->
<script>
    window.RESERVATION_UNIT_PRICE = ${price};        // 서버에서 내려준 1인 단가
    window.RESERVATION_PLACE_ID   = ${placeId};      // 캘린더가 이 장소의 마감(이미 예약된 날짜) 현황을 조회
    window.RESERVATION_PLACE_TYPE = '${placeType}';  // tour/food면 예약금(만나서결제), 그 외(숙박)는 기간 예약
    window.RESERVATION_DEPOSIT    = ${deposit};      // 예약금 정액
</script>
<%-- reservation-form.js가 visitDate의 change를 듣고, 캘린더가 날짜 선택 시 그 change를 쏜다 (로드 순서 유지) --%>
<script src="/js/reservation/reservation-form.js"></script>
<script src="/js/reservation/reservation-calendar.js"></script>
</body>
</html>
