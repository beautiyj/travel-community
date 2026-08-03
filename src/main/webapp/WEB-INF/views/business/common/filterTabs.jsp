<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
목록 화면 상단의 필터 탭 (예약 관리 상태 필터 / 후기 확인 감성 필터 공용)

모델에서 읽는 값 (컨트롤러의 addFilterAttributes가 채운다):
- filterTabs     : "라벨 -> 건수" 순서가 유지된 Map. 건수가 없는 탭(전체)은 값이 null이라 뱃지를 안 그린다
- filterSelected : 현재 선택된 라벨

사용법 (jsp:param으로 전달하는 값):
- baseUrl    : 탭 링크의 기본 경로 (예: /business/reservations)
- paramName  : 라벨을 실어 보낼 쿼리 파라미터 이름 (예: status / sentiment)
- totalCount : 오른쪽에 표시할 "총 N건"의 N
--%>
<div class="business-filter-row">
    <c:forEach var="tab" items="${filterTabs}">
        <c:url value="${param.baseUrl}" var="tabUrl">
            <%-- 전체 탭은 파라미터 없는 기본 목록으로 보낸다 --%>
            <c:if test="${tab.key != '전체'}">
                <c:param name="${param.paramName}" value="${tab.key}" />
            </c:if>
        </c:url>
        <a href="${tabUrl}" class="business-filter-btn${filterSelected == tab.key ? ' is-active' : ''}">${tab.key}<c:if test="${not empty tab.value}"> <span class="business-filter-btn__count">${tab.value}</span></c:if></a>
    </c:forEach>
    <span class="business-filter-row__total">총 ${param.totalCount}건</span>
</div>
