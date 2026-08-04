<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
business 화면 공통 상단 바 (제목 + 오늘 날짜)

사용법 (jsp:param으로 전달하는 값):
- title    : 화면 제목 (필수)
- date     : 오른쪽에 표시할 날짜 문구. 미전달이면 날짜 영역 자체를 그리지 않는다
- dateIcon : 'true'면 날짜 앞에 달력 아이콘을 붙인다 (예약 관리 화면)
--%>
<div class="business-topbar">
    <h1 class="business-topbar__title">${param.title}</h1>
    <c:if test="${not empty param.date}">
        <span class="business-topbar__date">
            <c:if test="${param.dateIcon == 'true'}">
                <svg class="business-topbar__date-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                    <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </c:if>
            ${param.date}
        </span>
    </c:if>
</div>
