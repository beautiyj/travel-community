<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%--
날짜별 마감 설정 카드 (마감 관리 탭 공용 컴포넌트)
todo: 기능 구현 예정
--%>
<div class="business-card business-closure-date-card">
    <h2 class="business-card__title" style="margin-bottom: 4px;">날짜별 마감 설정</h2>
    <p class="business-closure-card__desc" style="margin-bottom: 20px;">특정 날짜에 예약을 받지 않도록 설정합니다</p>

    <div class="business-closure-date-form">
        <input type="date" id="closure-date-input" class="business-form-input" />
        <button type="button" id="closure-date-add" class="business-btn business-btn--primary" disabled>추가</button>
    </div>

    <div id="closure-date-list" class="business-closure-date-list"></div>
    <p class="business-empty" id="closure-date-empty">설정된 마감 날짜가 없습니다</p>
</div>
