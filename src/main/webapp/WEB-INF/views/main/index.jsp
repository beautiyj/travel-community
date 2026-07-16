<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>여행 커뮤니티 메인</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/cardComponent.css">
    </head>
    <body style="margin: 0; padding: 0;">
        <%@ include file="../common/navbar.jsp" %>

        <div style="padding: 20px; min-height: 400px;">
            <h2>컴포넌트추가_테스트인덱스파일</h2>
            <button onclick="location.href='/tour/test'">테스트 페이지</button>

            <!-- 위시버튼 컴포넌트 가상 데이터 테스트 -->
            <div style="display:flex; gap:16px; margin-top:20px;">
                <!-- 찜off isBookmarked=false -->
                <jsp:include page="../common/wishButton.jsp">
                    <jsp:param name="placeId" value="1001" />
                    <jsp:param name="isBookmarked" value="false" />
                </jsp:include>
                <!-- 찜on isBookmarked=true -->
                <jsp:include page="../common/wishButton.jsp">
                    <jsp:param name="placeId" value="1002" />
                    <jsp:param name="isBookmarked" value="true" />
                </jsp:include>
                <!-- isBookmarked 파라미터 자체를 안 준 경우 - 기본값 false 확인용-->
                <jsp:include page="../common/wishButton.jsp">
                    <jsp:param name="placeId" value="1003" />
                </jsp:include>
            </div>

            <!-- 태그버튼 컴포넌트 디자인 확인용 가상 데이터 테스트 -->
            <div style="display:flex; gap:12px; margin-top:20px; align-items:center;">
                <!-- place_type=food -->
                <jsp:include page="../common/tagButton.jsp">
                    <jsp:param name="place_type" value="food" />
                </jsp:include>
                <!-- place_type=stay -->
                <jsp:include page="../common/tagButton.jsp">
                    <jsp:param name="place_type" value="stay" />
                </jsp:include>
                <!-- place_type=tour -->
                <jsp:include page="../common/tagButton.jsp">
                    <jsp:param name="place_type" value="tour" />
                </jsp:include>
                <!-- place_type 미전달 + text 미전달 (기본값 "관광지" 텍스트 확인용) -->
                <jsp:include page="../common/tagButton.jsp" />
                <!-- place_type 미전달 + 사용자 지정 텍스트 (기본 태그, 유동적 사이즈 확인용 - 짧은 텍스트) -->
                <jsp:include page="../common/tagButton.jsp">
                    <jsp:param name="text" value="인기" />
                </jsp:include>
                <jsp:include page="../common/tagButton.jsp">
                    <jsp:param name="text" value="서울 성수동 인기 카페" />
                </jsp:include>

            </div>

            <!-- 카드 컴포넌트 가상 데이터 테스트 -->
            <div style="display:flex; gap:25px; margin-top:30px; flex-wrap:wrap;">
                <!-- 숙박 태그 + 찜 OFF + 가상 데이터 -->
                <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                    <jsp:param name="place_type" value="stay" />
                    <jsp:param name="placeId" value="2001" />
                    <jsp:param name="isBookmarked" value="false" />
                    <jsp:param name="name" value="게스트하우스 데이지" />
                    <jsp:param name="regionName" value="제주 애월" />
                    <jsp:param name="rating" value="4.5" />
                    <jsp:param name="reviewCount" value="128" />
                    <jsp:param name="price" value="1박 89,000원" />
                    <jsp:param name="hashTags" value="오션뷰,조용한동네" />
                </jsp:include>

                <!-- 관광지 태그 + 찜 ON + 가상 데이터 -->
                <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                    <jsp:param name="place_type" value="tour" />
                    <jsp:param name="placeId" value="2002" />
                    <jsp:param name="isBookmarked" value="true" />
                    <jsp:param name="name" value="성산일출봉" />
                    <jsp:param name="regionName" value="제주 서귀포" />
                    <jsp:param name="rating" value="4.8" />
                    <jsp:param name="reviewCount" value="1023" />
                    <jsp:param name="price" value="입장료 5,000원" />
                    <jsp:param name="hashTags" value="일출명소,자연경관" />
                </jsp:include>

                <!-- 기본 태그 + 기본 카드 데이터 + 찜 OFF -->
                <jsp:include page="/WEB-INF/views/common/cardComponent.jsp">
                    <jsp:param name="placeId" value="2003" />
                    <jsp:param name="isBookmarked" value="false" />
                </jsp:include>

            </div>
        </div>
        <%@ include file="../common/footer.jsp" %>

        <!-- common.js 함수 로딩(필수) -->
        <script src="${pageContext.request.contextPath}/js/common.js"></script>

    </body>
</html>