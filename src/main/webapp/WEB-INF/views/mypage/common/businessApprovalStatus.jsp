<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${application.status eq 'PENDING'}">승인 대기</c:when>
    <c:when test="${application.status eq 'APPROVED'}">승인 완료</c:when>
    <c:when test="${application.status eq 'REJECTED'}">승인 반려</c:when>
    <c:otherwise><c:out value="${application.status}"/></c:otherwise>
</c:choose>
