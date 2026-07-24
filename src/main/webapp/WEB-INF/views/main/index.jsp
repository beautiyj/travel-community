<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%
    // =========================================================================
    // 드롭다운 컴포넌트 테스트용 더미 데이터 생성 (request 영역 저장)
    // =========================================================================
    
    // 1. 지역 리스트 더미
    java.util.List<java.util.Map<String, String>> regionList = new java.util.ArrayList<>();
    java.util.Map<String, String> r1 = new java.util.HashMap<>(); r1.put("code", "1"); r1.put("name", "서울"); regionList.add(r1);
    java.util.Map<String, String> r2 = new java.util.HashMap<>(); r2.put("code", "6"); r2.put("name", "부산"); regionList.add(r2);
    java.util.Map<String, String> r3 = new java.util.HashMap<>(); r3.put("code", "39"); r3.put("name", "제주"); regionList.add(r3);
    java.util.Map<String, String> r4 = new java.util.HashMap<>(); r4.put("code", "32"); r4.put("name", "강원"); regionList.add(r4);
    request.setAttribute("regionList", regionList);
    
    // 2. 카테고리 리스트 더미
    java.util.List<java.util.Map<String, String>> categoryList = new java.util.ArrayList<>();
    java.util.Map<String, String> c1 = new java.util.HashMap<>(); c1.put("code", "12"); c1.put("name", "관광지"); categoryList.add(c1);
    java.util.Map<String, String> c2 = new java.util.HashMap<>(); c2.put("code", "39"); c2.put("name", "맛집"); categoryList.add(c2);
    java.util.Map<String, String> c3 = new java.util.HashMap<>(); c3.put("code", "32"); c3.put("name", "숙박"); categoryList.add(c3);
    request.setAttribute("categoryList", categoryList);
    
    // 3. 정렬 리스트 더미
    java.util.List<java.util.Map<String, String>> sortList = new java.util.ArrayList<>();
    java.util.Map<String, String> s1 = new java.util.HashMap<>(); s1.put("code", "latest"); s1.put("name", "최신순"); sortList.add(s1);
    java.util.Map<String, String> s2 = new java.util.HashMap<>(); s2.put("code", "popular"); s2.put("name", "인기순"); sortList.add(s2);
    java.util.Map<String, String> s3 = new java.util.HashMap<>(); s3.put("code", "rating"); s3.put("name", "평점 높은 순"); sortList.add(s3);
    request.setAttribute("sortList", sortList);
    
    // 4. 개수별 데이터 테스트용 리스트 (2개, 4개, 8개)
    java.util.List<java.util.Map<String, String>> twoItemList = new java.util.ArrayList<>();
    java.util.Map<String, String> t1 = new java.util.HashMap<>(); t1.put("code", "Y"); t1.put("name", "공개"); twoItemList.add(t1);
    java.util.Map<String, String> t2 = new java.util.HashMap<>(); t2.put("code", "N"); t2.put("name", "비공개"); twoItemList.add(t2);
    request.setAttribute("twoItemList", twoItemList);
    
    java.util.List<java.util.Map<String, String>> fourItemList = new java.util.ArrayList<>();
    java.util.Map<String, String> f1 = new java.util.HashMap<>(); f1.put("code", "SPRING"); f1.put("name", "봄 여행"); fourItemList.add(f1);
    java.util.Map<String, String> f2 = new java.util.HashMap<>(); f2.put("code", "SUMMER"); f2.put("name", "여름 여행"); fourItemList.add(f2);
    java.util.Map<String, String> f3 = new java.util.HashMap<>(); f3.put("code", "AUTUMN"); f3.put("name", "가을 여행"); fourItemList.add(f3);
    java.util.Map<String, String> f4 = new java.util.HashMap<>(); f4.put("code", "WINTER"); f4.put("name", "겨울 여행"); fourItemList.add(f4);
    request.setAttribute("fourItemList", fourItemList);
    
    java.util.List<java.util.Map<String, String>> eightItemList = new java.util.ArrayList<>();
    for(int i = 1; i <= 8; i++) {
        java.util.Map<String, String> item = new java.util.HashMap<>();
        item.put("code", "OPT_" + i);
        item.put("name", "옵션 항목 " + i);
        eightItemList.add(item);
    }
    request.setAttribute("eightItemList", eightItemList);
    
    // 5. 기본 선택값 테스트용 데이터
    request.setAttribute("selectedType", "SUMMER");
    request.setAttribute("selectedTypeName", "여름 여행");
%>

<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>여행 커뮤니티 메인</title>

        <%-- 공통 CSS 및 컴포넌트 CSS 호출 --%>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableCardComponent.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/dropdownSelector.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/searchbar.css">
    </head>
    <body style="margin: 0; padding: 0;">

        <jsp:include page="/WEB-INF/views/common/header.jsp" />

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