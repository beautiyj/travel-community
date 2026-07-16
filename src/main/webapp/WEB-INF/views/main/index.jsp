<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>여행 커뮤니티 메인</title>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableButton.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableCardComponent.css">
    </head>
    <body style="margin: 0; padding: 0;">

        <%@ include file="../common/navbar.jsp" %>

        <div style="padding: 20px; min-height: 400px;">
            <h2>컴포넌트추가_테스트인덱스파일</h2>
            <button onclick="location.href='/tour/test'">테스트 페이지</button>

            <!-- ===================== 셀렉터블 버튼 테스트 ===================== -->
            <h3 style="margin-top:30px;">셀렉터블 버튼 테스트</h3>
            <div style="display:flex; flex-wrap:wrap; gap:12px; margin-top:12px;">

                <!-- Case 1: 파라미터 전부 미전달 (기본값 확인 - text="선택", theme-primary, 비활성 상태) -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp" />

                <!-- Case 2: danger 테마 + 최초 활성화 상태 -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="삭제" />
                    <jsp:param name="theme" value="danger" />
                    <jsp:param name="isActive" value="true" />
                </jsp:include>

                <!-- Case 3: primary 테마 + 최초 활성화 상태 + 커스텀 텍스트 -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="아침 포함" />
                    <jsp:param name="theme" value="primary" />
                    <jsp:param name="isActive" value="true" />
                </jsp:include>

                <!-- Case 5: onclick 커스텀 스크립트 지정 확인 -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="알림 받기" />
                    <jsp:param name="onclick" value="alert('커스텀 onclick 동작 확인')" />
                </jsp:include>

                <!-- 6 너비조정 + 고정형 길이 셀렉터블 -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="사이즈500px고정한긴셀렉터블버튼" />
                    <jsp:param name="width" value="500px" />
                </jsp:include>

                <!-- 7 너비조정 + 반응형 길이 셀렉터블 -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="사이즈100%반응형긴셀렉터블버튼" />
                    <jsp:param name="width" value="100%" />
                </jsp:include>

                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="나만 민트색" />
                    <jsp:param name="style" value="--primary: #12b886; --primary-soft: #e6fcf5;" />
                </jsp:include>

            </div>

            <div style="display:flex; flex-wrap:wrap; gap:12px; margin-top:12px;">
                <!-- Case 4: width 지정 + 긴 텍스트 (nowrap 처리 확인용, 글씨잘림) -->
                <jsp:include page="/WEB-INF/views/common/selectableButton.jsp">
                    <jsp:param name="text" value="반려동물 동반 가능 숙소" />
                    <jsp:param name="width" value="100px" />
                </jsp:include>
            </div>

            <!-- ===================== 셀렉터블 카드 테스트 ===================== -->
            <h3 style="margin-top:30px;">셀렉터블 카드 테스트 (회원가입 role 선택 시나리오)</h3>
            <div style="display:flex; gap:25px; margin-top:12px; flex-wrap:wrap;">

                <!-- Card 1: 일반 사용자 (기본 선택 상태 - isActive=true) -->
                <jsp:include page="/WEB-INF/views/common/selectableCardComponent.jsp">
                    <jsp:param name="id" value="ROLE_USER" />
                    <jsp:param name="group" value="role-group" />
                    <jsp:param name="theme" value="blue" />
                    <jsp:param name="isActive" value="true" />
                    <jsp:param name="emoji" value="🧳" />
                    <jsp:param name="title" value="일반 사용자" />
                    <jsp:param name="description" value="여행지를 둘러보고 리뷰를 남길 수 있어요." />
                    <jsp:param name="feat1" value="맛집/숙박/관광지 검색" />
                    <jsp:param name="feat2" value="찜하기 및 리뷰 작성" />
                    <jsp:param name="feat3" value="커뮤니티 게시글 작성" />
                </jsp:include>

                <!-- Card 2: 사업자 (미선택 상태) -->
                <jsp:include page="/WEB-INF/views/common/selectableCardComponent.jsp">
                    <jsp:param name="id" value="ROLE_BUSINESS" />
                    <jsp:param name="group" value="role-group" />
                    <jsp:param name="theme" value="amber" />
                    <jsp:param name="emoji" value="🏨" />
                    <jsp:param name="title" value="사업자" />
                    <jsp:param name="description" value="내 업체를 등록하고 예약을 관리할 수 있어요." />
                    <jsp:param name="feat1" value="업체 정보 등록" />
                    <jsp:param name="feat2" value="예약 및 매출 관리" />
                    <jsp:param name="feat3" value="리뷰 답글 작성" />
                </jsp:include>

                <!-- Card 3: 파라미터 전부 미전달 (기본값 확인용 - theme=blue, 미선택 상태) -->
                <jsp:include page="/WEB-INF/views/common/selectableCardComponent.jsp">
                    <jsp:param name="group" value="role-group-default-test" />
                </jsp:include>

            </div>

            <!-- 회원가입 폼 연동 확인용 hidden input -->
            <input type="hidden" id="memberRole" value="ROLE_USER" />
            <p style="margin-top:12px; color:var(--muted-foreground); font-size:var(--text-sm);">
                현재 선택된 memberRole 값: <span id="memberRoleDisplay">ROLE_USER</span>
                (Card 1/Card 2 클릭 시 아래 값이 실시간으로 바뀌는지 확인 — Card 3은 group이 달라서 별도 동작합니다)
            </p>

        </div>

        <%@ include file="../common/footer.jsp" %>

        <script src="${pageContext.request.contextPath}/js/common.js"></script>

        <!-- hidden input 값 변화를 화면에서 눈으로 확인하기 위한 테스트 전용 스크립트 -->
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