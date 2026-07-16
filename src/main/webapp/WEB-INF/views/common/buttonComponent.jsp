<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>

<c:set var="btnWidth" value="${empty param.width ? 'auto' : param.width}" />

<button class="btn-main" style="width: ${btnWidth};" onclick="alert('Pressed!')">
	<span class="btn-main-text">
		${param.text}
	</span>
</button>

<%-- 컴포넌트 사용하는 방법
1. 기본적인 설정값(디자인 변수명 --primary 등)은 common.css에
2. common.css의 변수명을 참고하여 buttonComponent.css 코드 작성(디자인 요소만)
3. buttonComponent.jsp는 순수 html+jsp로

3-1. JSP 파라미터 기본값 예외 처리
기본값 너비 - <c:set var="btnWidth" 부분은 사용자의 임의 변수(직관적인 변수명으로 지정)
<c:set 에서 width가 비어있으면 기본값 'auto' 적용 (buttonComponent.css 디자인을 그대로 사용)

만약 <c:set var="btnColor" value="${empty param.color ? 'var(--primary)' : param.color}" />
로 적혀있을 경우, 컬러 기본값을 buttonComponent.css 디자인 설정한 걸로 쓰겠다는 의미

3-2. 각 view.jsp에서 넣는 동적 데이터는 ${param.text} 처럼 주입하기
(ex) 삭제 / 수정 / 숙박 이라는 텍스트 데이터가 들어있는 버튼

4. view.jsp에서 컴포넌트 사용
먼저 common.css 등 css 선언
<head>
<link rel="stylesheet" href="/css/common.css">
<link rel="stylesheet" href="/css/components/buttonComponent.css">
</head>

<body>

<div class="button-wrapper">
<jsp:include page="/WEB-INF/views/components/buttonComponent.jsp">
<jsp:param name="text" value="이메일로 로그인" />
width와 color는 기본값 사용
</jsp:include>
</div>

<div class="button-wrapper">
<jsp:include page="/WEB-INF/views/components/buttonComponent.jsp">
<jsp:param name="text" value="카카오 로그인" />
<jsp:param name="width" value="100%" />       너비 커스텀 주입(css가 아닌 개인너비로 사용 )
<jsp:param name="color" value="#FEE500" />  컬러 커스텀 주입
</jsp:include>
</div>

</body>

--%>



<%-- 일반 html코드는
<button class="button"
onclick="alert('Pressed!')"}>
<span class="text" >
이메일로 로그인
</span>
</button> --%>

<%-- view에서 사용할 때

<head>
<link rel="stylesheet" href="/css/common.css">
<link rel="stylesheet" href="/css/components/buttonComponent.css">
</head>
<body>

<jsp:include page="buttonComponent.jsp"><jsp:param name="text" value="로그인" /></jsp:include>
<jsp:include page="buttonComponent.jsp"><jsp:param name="text" value="회원가입" /></jsp:include>

</body>
--%>
