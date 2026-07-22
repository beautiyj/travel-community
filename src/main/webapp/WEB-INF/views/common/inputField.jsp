<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
inputField.jsp — 라벨 + 입력창 재사용 컴포넌트
param:
label       : 라벨 텍스트
name        : input name (폼 전송용, 필수)
type        : input type (기본 text)
id          : input id (기본값 = name)
placeholder : 안내 문구 (선택)
maxlength   : 최대 글자수 (선택)
value       : 초기값 (선택)
required    : 'true'이면 필수 입력 (선택)
사용 시 head에 components/inputField.css 로드 필요
--%>
<c:set var="fType" value="${empty param.type ? 'text' : param.type}" />
<c:set var="fId" value="${empty param.id ? param.name : param.id}" />

<div class="input-field">
    <label for="${fId}">${param.label}</label>
    <input type="${fType}" id="${fId}" name="${param.name}"
    <c:if test="${not empty param.placeholder}">placeholder="${param.placeholder}"</c:if>
        <c:if test="${not empty param.maxlength}">maxlength="${param.maxlength}"</c:if>
            <c:if test="${not empty param.value}">value="${param.value}"</c:if>
                <c:if test="${param.required eq 'true'}">required</c:if> />
            </div>