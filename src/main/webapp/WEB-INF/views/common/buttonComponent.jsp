<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="btnWidth" value="${empty param.width ? 'auto' : param.width}" />
<c:set var="btnColor" value="${empty param.color ? 'var(--primary)' : param.color}" />
<c:set var="fontSize" value="${empty param.size ? 'var(--text-base)' : param.size}" />

<button class="btn-main" style="width: ${btnWidth};" onclick="alert('Pressed!')">
	<span class="btn-main-text" style="font-size: ${fontSize};">
		${param.text}
	</span>
</button>