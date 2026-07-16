<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>여행 커뮤니티 메인</title>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
    </head>
    <body style="margin: 0; padding: 0;">

        <%@ include file="../common/navbar.jsp" %>

        <div style="padding: 20px; min-height: 400px;">
            <h2>컴포넌트추가_테스트인덱스파일</h2>
            <button onclick="location.href='/tour/test'">테스트 페이지</button>

            <div style="display:flex; flex-direction:column; gap:16px; margin-top:20px; align-items:flex-start;">

                <!-- Case 1: 파라미터 전부 미전달 (기본값 확인 - primary 색상, auto 너비, base 폰트사이즈) -->
                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="기본 버튼" />
                </jsp:include>

                <!-- Case 2: 로그인 폼처럼 부모 폭 100% 채우는 CTA 버튼 -->
                <div style="width:320px; border:1px dashed var(--border); padding:8px;">
                    <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                        <jsp:param name="text" value="이메일로 로그인" />
                        <jsp:param name="width" value="100%" />
                    </jsp:include>
                </div>

                <!-- Case 3: color만 지정 (빨간색 - 회원탈퇴/삭제 등 destructive 액션 가정) -->
                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="회원 탈퇴" />
                    <jsp:param name="color" value="#DC2626" />
                </jsp:include>

                <!-- Case 4: 모달 안에서 쓸 법한 고정 너비 + 색상 조합 -->
                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="확인" />
                    <jsp:param name="width" value="240px" />
                    <jsp:param name="color" value="#007A55" />
                </jsp:include>

                <!-- Case 5: size(폰트 크기)까지 조합한 예약/결제성 버튼 -->
                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="예약하기" />
                    <jsp:param name="width" value="200px" />
                    <jsp:param name="color" value="#0284C7" />
                    <jsp:param name="size" value="18px" />
                </jsp:include>

                <!-- Case 6: 좁은 너비 + 긴 텍스트 (padding 수정 이후 정상 표시 확인용) -->
                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="좁은너비테스트버튼입니다" />
                    <jsp:param name="width" value="100px" />
                </jsp:include>

                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="텍스트길이길이길이길이rlfdlrlfdlrldldsajbdwiubjk " />
                    <jsp:param name="width" value="100%" />
                </jsp:include>

                <jsp:include page="/WEB-INF/views/common/buttonComponent.jsp">
                    <jsp:param name="text" value="텍스트길이길이길이길이rlfdlrlfdlrldldsajbdwiubjk " />
                    <jsp:param name="width" value="500px" />
                </jsp:include>

            </div>
        </div>

        <%@ include file="../common/footer.jsp" %>

        <script src="${pageContext.request.contextPath}/js/common.js"></script>

    </body>
</html>