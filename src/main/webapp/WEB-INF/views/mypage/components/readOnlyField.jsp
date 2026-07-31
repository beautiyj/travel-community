<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<div class="input-field input-field--readonly">
    <label for="${param.id}"><c:out value="${param.label}"/></label>
    <input id="${param.id}" type="text"
           value="<c:out value='${param.value}'/>"
           readonly aria-readonly="true">
    <c:if test="${not empty param.helpText}">
        <p class="input-field__help"><c:out value="${param.helpText}"/></p>
    </c:if>
</div>
