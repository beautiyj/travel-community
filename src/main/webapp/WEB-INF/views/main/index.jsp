<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>여행 커뮤니티 메인</title>

        <%-- 공통 CSS 및 컴포넌트 CSS 호출 --%>
        <%-- <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css"> --%>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableCardComponent.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/dropdownSelector.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/searchbar.css">
    </head>
    <body style="margin: 0; padding: 0;">

        <%@ include file="../common/header.jsp" %>

        <div style="padding: 20px; min-height: 400px;">
            <h2>컴포넌트추가_테스트인덱스파일</h2>
            <button onclick="location.href='/tour/test'">테스트 페이지</button>

            <!-- ===================== 검색창 컴포넌트 테스트 ===================== -->
            <h3 style="margin-top:30px;">검색창 컴포넌트 테스트</h3>

            <%-- 1. 파라미터 미전달 (기본 설정값 적용) --%>
            <p style="margin-top:15px; color:#666;">1. 기본 검색창 (파라미터 미전달)</p>
            <form action="/tour/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp" />
            </form>

            <%-- 2. 커스텀 설정 (플레이스홀더, 버튼 텍스트, 파라미터명 변경) --%>
            <p style="margin-top:25px; color:#666;">2. 커스텀 검색창 (플레이스홀더, 버튼명, input name 변경)</p>
            <form action="/community/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                    <jsp:param name="placeholder" value="게시글 제목이나 내용을 입력하세요" />
                    <jsp:param name="name"        value="query" />
                    <jsp:param name="btnText"     value="조회" />
                </jsp:include>
            </form>

            <%-- 3. 검색어 유지 테스트 (value 전달) --%>
            <p style="margin-top:25px; color:#666;">3. 검색어 유지를 위한 value 값 전달 (기존 검색어: 제주도)</p>
            <form action="/tour/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                    <jsp:param name="value" value="제주도" />
                </jsp:include>
            </form>

            <%-- 4. 너비 확장, 드롭다운 포함 및 버튼 색상 변경 테스트 --%>
            <p style="margin-top:25px; color:#666;">4. 너비 지정(600px), 드롭다운 포함, 버튼 색상 변경(#dc2626)</p>
            <form action="/tour/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                    <jsp:param name="width"       value="600px" />
                    <jsp:param name="useDropdown"  value="true" />
                    <jsp:param name="btnText"      value="검색" />
                    <jsp:param name="btnColor"     value="#dc2626" />
                </jsp:include>
            </form>

            <%-- 5. 드롭다운 커스텀 데이터(regionList) 및 드롭다운 너비(140px) 지정 테스트 --%>
            <p style="margin-top:25px; color:#666;">5. 커스텀 데이터 드롭다운 (지역 선택 data: regionList, 너비: 140px)</p>
            <form action="/tour/list" method="get">
                <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
                    <jsp:param name="width"         value="600px" />
                    <jsp:param name="useDropdown"    value="true" />
                    <jsp:param name="listAttr"       value="regionList" />
                    <jsp:param name="defaultLabel"   value="지역 선택" />
                    <jsp:param name="dropdownWidth"  value="140px" />
                    <jsp:param name="btnText"        value="검색" />
                    <jsp:param name="btnColor"       value="#dc2626" />
                </jsp:include>
            </form>

        </div>

        <%@ include file="../common/footer.jsp" %>

        <!-- 💡 부트스트랩 JS (드롭다운 토글 제어용) -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

        <script src="${pageContext.request.contextPath}/js/dropdownSelector.js"></script>
        <script src="${pageContext.request.contextPath}/js/common.js"></script>

    </body>
</html>