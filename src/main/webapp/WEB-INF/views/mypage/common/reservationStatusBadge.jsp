<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${param.status eq 'PAID'}">
        <c:set var="statusClass" value="paid"/>
        <c:set var="statusLabel" value="결제완료"/>
        <c:set var="tagType" value="food"/>
    </c:when>
    <c:when test="${param.status eq 'CANCEL_REQUESTED'}">
        <c:set var="statusClass" value="requested"/>
        <c:set var="statusLabel" value="취소요청"/>
        <c:set var="tagType" value=""/>
    </c:when>
    <c:when test="${param.status eq 'CONFIRMED'}">
        <c:set var="statusClass" value="confirmed"/>
        <c:set var="statusLabel" value="예약 확정"/>
        <c:set var="tagType" value="tour"/>
    </c:when>
    <c:when test="${param.status eq 'COMPLETED'}">
        <c:set var="statusClass" value="completed"/>
        <c:set var="statusLabel" value="이용 완료"/>
        <c:set var="tagType" value="stay"/>
    </c:when>
    <c:when test="${param.status eq 'CANCELED' or param.status eq 'CANCELLED'}">
        <c:set var="statusClass" value="cancelled"/>
        <c:set var="statusLabel" value="예약취소"/>
        <c:set var="tagType" value=""/>
    </c:when>
    <c:when test="${param.status eq 'EXPIRED'}">
        <c:set var="statusClass" value="expired"/>
        <c:set var="statusLabel" value="예약만료"/>
        <c:set var="tagType" value=""/>
    </c:when>
    <c:otherwise>
        <c:set var="statusClass" value="pending"/>
        <c:set var="statusLabel" value="예약대기"/>
        <c:set var="tagType" value="food"/>
    </c:otherwise>
</c:choose>
<div class="mypage-status mypage-status--${statusClass}">
    <jsp:include page="/WEB-INF/views/common/tagButton.jsp">
        <jsp:param name="place_type" value="${tagType}"/>
        <jsp:param name="text" value="${statusLabel}"/>
    </jsp:include>
</div>
