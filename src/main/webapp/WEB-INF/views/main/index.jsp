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
    </head>
    <body style="margin: 0; padding: 0;">

        <%@ include file="../common/navbar.jsp" %>

        <div style="padding: 20px; min-height: 400px;">
            <h2>컴포넌트추가_테스트인덱스파일</h2>
            <button onclick="location.href='/tour/test'">테스트 페이지</button>

            <!-- ===================== 드롭다운 셀렉터 테스트 ===================== -->
            <h3 style="margin-top:30px;">드롭다운 셀렉터 테스트</h3>

            <%--
            - dropdownId       : (필수) 화면 내 고유 식별자 (JS/HTML id 중복 방지)
            ex) "dropdown_basic", "regionSelect"
            - listAttr         : (필수) Controller가 request.setAttribute("이름", list)로 전달한 데이터 변수명
            ex) "sortList", "regionList"
            - defaultLabel     : (선택) 선택 전 기본 노출 문구 (미지정 시 '선택')
            ex) "지역 선택", "카테고리"
            - iconSrc          : (선택) 버튼 좌측 이미지 경로 (PNG/SVG 모두 가능)
            ex) "${pageContext.request.contextPath}/images/icons/search.png"
            - width            : (선택) 너비 커스텀 지정 (미지정 시 기본 CSS max-width: 200px)
            ex) "120px", "240px", "50%", "100%"
            - selectedAttr     : (선택) 초기 선택되어 있을 값(Code) 변수명
            - selectedNameAttr : (선택) 초기 선택되어 있을 라벨(Name) 변수명
            - targetUrl        : (선택) 선택 시 이동할 URL 경로
            - paramKey         : (선택) URL 전달용 파라미터 키 이름
            --%>

            <%-- 1. 필수값인 아이디(dropdownId), 데이터리스트(listAttr)만 전달한 기본 드롭다운 --%>
            <p style="margin-top:15px; color:#666;">1. 필수값 기본 드롭다운</p>
            <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
                <jsp:param name="dropdownId" value="dropdown_basic" />
                <jsp:param name="listAttr"   value="sortList" />
            </jsp:include>

            <%-- 2. png/svg 아이콘 포함된 드롭다운 --%>
            <p style="margin-top:25px; color:#666;">2. 아이콘 포함 드롭다운</p>
            <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
                <jsp:param name="dropdownId"   value="dropdown_icon_region" />
                <jsp:param name="listAttr"     value="regionList" />
                <jsp:param name="defaultLabel" value="지역 선택" />
                <jsp:param name="iconSrc"      value="${pageContext.request.contextPath}/images/icons/search.png" />
                <jsp:param name="targetUrl"    value="/tour/list" />
                <jsp:param name="paramKey"     value="region" />
            </jsp:include>

            <%-- 3. 드롭다운 기본 너비/사이즈 설정 --%>
            <p style="margin-top:25px; color:#666;">3. 드롭다운 사이즈 설정</p>
            <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
                <jsp:param name="dropdownId"   value="dropdown_size_default" />
                <jsp:param name="listAttr"     value="sortList" />
                <jsp:param name="defaultLabel" value="너비 미지정(기본값)" />
                <jsp:param name="targetUrl"    value="/tour/list" />
                <jsp:param name="paramKey"     value="sort" />
            </jsp:include>

            <%-- 4. 드롭다운 컬러 변경 --%>
            <p style="margin-top:25px; color:#666;">4. 드롭다운 컬러 변경</p>
            <div style="display: flex; gap: 15px;">
                <!-- --drop-bg와 --drop-text 지정 -->
                <div style="--drop-border:#12b886; --drop-bg:#e6fcf5; --drop-text:#0ca678; --drop-active-bg:#c3fae8; --drop-active-text:#087f5b;">
                    <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
                        <jsp:param name="dropdownId"   value="dropdown_color_green" />
                        <jsp:param name="listAttr"     value="sortList" />
                        <jsp:param name="defaultLabel" value="그린 컬러" />
                    </jsp:include>
                </div>
                <!-- --drop-bg와 --drop-text 지정 -->
                <div style="--drop-border:#fa5252; --drop-active-bg:#fff5f5; --drop-active-text:#fa5252;">
                    <jsp:include page="/WEB-INF/views/common/dropdownSelector.jsp">
                        <jsp:param name="dropdownId"   value="dropdown_color_red" />
                        <jsp:param name="listAttr"     value="sortList" />
                        <jsp:param name="defaultLabel" value="레드 컬러" />
                        <jsp:param name="targetUrl"    value="/tour/list" />
                        <jsp:param name="paramKey"     value="sort" />
                    </jsp:include>
                </div>
            </div>

            <!-- ===================== 드롭다운 셀렉터 테스트 끝 ===================== -->

        </div>

        <%@ include file="../common/footer.jsp" %>

        <!-- 💡 부트스트랩 JS (드롭다운 토글 제어용) -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

        <script src="${pageContext.request.contextPath}/js/dropdownSelector.js"></script>

        <script>
            document.addEventListener("click", function () {
                const roleInput = document.getElementById("memberRole");
                const display = document.getElementById("memberRoleDisplay");
                if (roleInput && display) {
                    display.textContent = roleInput.value;
                }
            });
        </script>

    </body>
</html>