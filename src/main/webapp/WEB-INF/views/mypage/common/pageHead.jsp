<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.nio.file.Paths" %>
<%@ page import="org.springframework.core.io.ClassPathResource" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><c:out value="${empty param.title ? '마이페이지' : param.title}" /></title>
<%--
    정적 CSS 경로가 401을 반환하는 환경에서도 동작하도록 인라인으로 로딩한다.
    개발 실행 시에는 빌드 폴더의 오래된 복사본 대신 소스 CSS를 우선 사용하고,
    패키징된 배포 환경에서는 클래스패스 리소스로 자동 폴백한다.
--%>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String[] mypageCssResources = {
        "static/css/mypage/base.css",
        "static/css/mypage/components/layout.css",
        "static/css/mypage/components/reservation-list.css",
        "static/css/mypage/components/profile-form.css",
        "static/css/mypage/common.css",
        "static/css/mypage/user.css",
        "static/css/mypage/components/modal.css",
        "static/css/mypage/components/mobile.css",
        "static/css/mypage/components/dashboard-shell.css",
        "static/css/mypage/components/reservation-table.css",
        "static/css/mypage/components/dashboard-overrides.css",
        "static/css/mypage/components/responsive.css",
        "static/css/mypage/business.css"
    };

    for (String resourcePath : mypageCssResources) {
        Path sourcePath = Paths.get(
            System.getProperty("user.dir", "."),
            "src", "main", "resources",
            resourcePath.replace('/', java.io.File.separatorChar)
        ).normalize();

        String cssContent;
        long cssVersion;
        if (Files.isRegularFile(sourcePath)) {
            cssContent = Files.readString(sourcePath, StandardCharsets.UTF_8);
            cssVersion = Files.getLastModifiedTime(sourcePath).toMillis();
        } else {
            ClassPathResource cssResource = new ClassPathResource(resourcePath);
            try (java.io.InputStream cssStream = cssResource.getInputStream()) {
                cssContent = new String(cssStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            cssVersion = 0L;
        }

        out.write("<style data-mypage-css-version=\"" + cssVersion + "\">");
        out.write(cssContent);
        out.write("</style>");
    }
%>
