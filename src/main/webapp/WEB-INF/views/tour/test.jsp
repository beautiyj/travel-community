<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>tour test</title>
    </head>
    <body style="margin: 0; padding: 0;">

        <%@ include file="../common/header.jsp" %>

        컴포넌트 매칭 테스트
        + 브랜치생성 yun

        <div style="padding: 20px; min-height: 400px;">

            <%-- 지역 선택 드롭다운: 너비 160px --%>
            <jsp:include page="../common/dropdownSelector.jsp">
                <jsp:param name="dropdownId" value="region" />
                <jsp:param name="listAttr" value="regionList" />
                <jsp:param name="selectedAttr" value="areaCode" />
                <jsp:param name="selectedNameAttr" value="areaName" />
                <jsp:param name="targetUrl" value="/tour/list" />
                <jsp:param name="paramKey" value="areaCode" />
                <jsp:param name="defaultLabel" value="지역 선택" />
                <jsp:param name="width" value="160" />
            </jsp:include>

            <%-- 예: 같은 페이지에 카테고리 드롭다운도 추가하고 싶다면, 너비만 다르게 해서 재사용 --%>
            <jsp:include page="../common/dropdownSelector.jsp">
                <jsp:param name="dropdownId" value="category" />
                <jsp:param name="listAttr" value="categoryList" />
                <jsp:param name="selectedAttr" value="categoryCode" />
                <jsp:param name="selectedNameAttr" value="categoryName" />
                <jsp:param name="targetUrl" value="/tour/list" />
                <jsp:param name="paramKey" value="categoryCode" />
                <jsp:param name="defaultLabel" value="카테고리 선택" />
                <jsp:param name="width" value="200" />
            </jsp:include>

            <c:forEach var="tour" items="${tourList}">
                <div class="tour-card">
                    <img src="${tour.firstimage}" alt="${tour.title}" />
                    <h3>${tour.title}</h3>
                    <p>${tour.addr1}</p>
                </div>
            </c:forEach>

            <%@ include file="../common/footer.jsp" %>

        </body>