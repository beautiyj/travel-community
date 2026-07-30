<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="org.springframework.core.io.ClassPathResource" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><c:out value="${empty param.title ? '마이페이지' : param.title}" /></title>
<%-- 정적 CSS 경로가 보안 설정에서 401을 반환하는 환경을 위한 마이페이지 전용 인라인 로딩 --%>
<%
    ClassPathResource mypageCss = new ClassPathResource("static/css/mypage.css");
    try (java.io.InputStream cssStream = mypageCss.getInputStream()) {
        out.write("<style>");
        out.write(new String(cssStream.readAllBytes(), StandardCharsets.UTF_8));
        out.write("</style>");
    }
%>
