<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%--
날짜별 마감 설정 카드 (마감 관리 탭 공용 컴포넌트)
--%>
<div class="business-card business-closure-date-card" data-place-id="${param.placeId}">
    <h2 class="business-card__title" style="margin-bottom: 4px;">마감 스케쥴 설정</h2>

    <div class="business-closure-calendar">
        <div class="business-closure-calendar__header">
            <button type="button" id="closure-cal-prev" class="business-closure-calendar__nav" aria-label="이전 달">&lsaquo;</button>
            <span class="business-closure-calendar__title" id="closure-cal-title"></span>
            <button type="button" id="closure-cal-next" class="business-closure-calendar__nav" aria-label="다음 달">&rsaquo;</button>
        </div>
        <div class="business-closure-calendar__weekdays">
            <span>일</span><span>월</span><span>화</span><span>수</span><span>목</span><span>금</span><span>토</span>
        </div>
        <div class="business-closure-calendar__grid" id="closure-cal-grid"></div>
    </div>

    <div class="business-closure-date-form">
        <span class="business-closure-date-form__label" id="closure-selection-label"></span>
        <div class="business-closure-date-form__actions">
            <button type="button" id="closure-edit-cancel" class="business-btn business-btn--outline business-btn--sm" style="display:none;">편집 취소</button>
            <button type="button" id="closure-date-add" class="business-btn business-btn--primary" disabled>마감 설정</button>
        </div>
    </div>

    <div id="closure-date-list" class="business-closure-date-list"></div>
    <p class="business-empty" id="closure-date-empty">설정된 마감 날짜가 없습니다</p>
</div>

<%--
fetch(DELETE)로 처리해야 해서 business-closure.js가 이 폼의 submit을 가로채 자체 처리
--%>
<jsp:include page="/WEB-INF/views/common/confirmModal.jsp">
    <jsp:param name="modalId" value="closureDeleteModal" />
    <jsp:param name="action" value="/business/closure" />
    <jsp:param name="title" value="마감 날짜 삭제" />
    <jsp:param name="message" value="선택한 마감 날짜를 삭제하시겠습니까?" />
    <jsp:param name="confirmText" value="삭제" />
    <jsp:param name="tone" value="danger" />
</jsp:include>
