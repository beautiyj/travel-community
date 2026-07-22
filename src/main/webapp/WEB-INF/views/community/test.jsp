<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.gnagnoohc.travel.auth.dto.LoginMemberDto" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    // ============================================================
    // ⚠ 테스트 전용 세팅 - 실제 배포 시 반드시 삭제할 것!
    // 로그인 담당자 세션 구조: loginMember = LoginMemberDto(memberId, nickname, memberRole)
    // ※ 실제 로그인(AuthController)이 세션에 넣는 것과 동일한 타입으로 맞춰야
    //    CommunityController.getMemberId()/isOwner() 의 (LoginMemberDto) 캐스팅이 안 깨진다.
    //    (예전엔 여기서 Long 하나만 넣어서 캐스팅 에러가 났었음)
    LoginMemberDto testLoginMember = new LoginMemberDto(1, "테스트유저", "USER");
    session.setAttribute("loginMember", testLoginMember);
%>
<c:set var="testMemberId" value="${sessionScope.loginMember.memberId}" />
<c:set var="testPlaceId" value="1" />
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>커뮤니티 게시판 통합 테스트</title>
<style>
    body { font-family: 'Malgun Gothic', sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; color: #1e293b; }
    h1 { font-size: 22px; border-bottom: 2px solid #0284c7; padding-bottom: 10px; }
    h2 { font-size: 16px; margin-top: 36px; background: #f0f9ff; padding: 8px 12px; border-left: 4px solid #0284c7; }
    .box { border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 12px; background: #fff; }
    .info { background: #fef9c3; border: 1px solid #eab308; padding: 10px 14px; border-radius: 6px; font-size: 13px; margin-bottom: 20px; }
    label { display: inline-block; width: 90px; font-size: 13px; }
    input[type=text], input[type=number], textarea, select { padding: 5px 8px; border: 1px solid #cbd5e1; border-radius: 4px; margin-bottom: 8px; width: 260px; }
    textarea { height: 60px; vertical-align: top; }
    button { background: #0284c7; color: #fff; border: none; padding: 6px 14px; border-radius: 4px; cursor: pointer; font-size: 13px; }
    button:hover { background: #026aa3; }
    .row { margin-bottom: 6px; }
    a.link-btn { display: inline-block; background: #e0f2fe; color: #0284c7; padding: 5px 10px; border-radius: 4px; text-decoration: none; font-size: 13px; margin-right: 6px; }
    code { background: #f1f5f9; padding: 2px 5px; border-radius: 3px; }
</style>
</head>
<body>

<h1>🧪 커뮤니티(TripAround) 게시판 통합 테스트 페이지</h1>

<div class="info">
    현재 세션 <code>loginMember</code> = <b>LoginMemberDto(memberId=${testMemberId})</b> 로 강제 세팅됨 (테스트용)<br>
    글쓰기 테스트 시 <code>placeId</code> = <b>${testPlaceId}</b> 로 하드코딩되어 전송됩니다.<br>
    ※ <code>CommunityDto</code>의 placeId 필드명이 다르면 아래 hidden input의 <code>name</code> 값을 실제 필드명에 맞게 바꿔주세요.
</div>

<!-- 1. 목록 조회 -->
<h2>1. 게시글 목록 조회 (GET /community/list)</h2>
<div class="box">
    <form action="${pageContext.request.contextPath}/community/list" method="get">
        <div class="row">
            <label>category</label>
            <input type="text" name="category" placeholder="예: 자유, 질문 (비우면 전체)">
        </div>
        <div class="row">
            <label>q (검색어)</label>
            <input type="text" name="q" placeholder="제목/내용 검색">
        </div>
        <button type="submit">목록 조회</button>
    </form>
    <a class="link-btn" href="${pageContext.request.contextPath}/community/list" target="_blank">전체 목록 바로가기</a>
</div>

<!-- 2. 글쓰기 -->
<h2>2. 글쓰기 (POST /community/write)</h2>
<div class="box">
    <form action="${pageContext.request.contextPath}/community/write" method="post" enctype="multipart/form-data">
        <input type="hidden" name="placeId" value="${testPlaceId}">
        <div class="row">
            <label>title</label>
            <input type="text" name="title" value="테스트 게시글 제목" required>
        </div>
        <div class="row">
            <label>category</label>
            <input type="text" name="category" value="자유">
        </div>
        <div class="row">
            <label>content</label><br>
            <textarea name="content" required>테스트 게시글 내용입니다.</textarea>
        </div>
        <div class="row">
            <label>images</label>
            <input type="file" name="images" multiple>
        </div>
        <button type="submit">등록</button>
    </form>
</div>

<!-- 3. 상세보기 -->
<h2>3. 게시글 상세 (GET /community/detail)</h2>
<div class="box">
    <form action="${pageContext.request.contextPath}/community/detail" method="get">
        <div class="row">
            <label>postId</label>
            <input type="number" name="postId" value="1" required>
        </div>
        <button type="submit">상세보기</button>
    </form>
</div>

<!-- 4. 수정 -->
<h2>4. 게시글 수정</h2>
<div class="box">
    <p style="font-size:13px; color:#64748b;">본인(memberId=${testMemberId}) 글만 수정 가능 (isOwner 체크)</p>
    <form action="${pageContext.request.contextPath}/community/edit" method="get">
        <div class="row">
            <label>postId</label>
            <input type="number" name="postId" value="1" required>
        </div>
        <button type="submit">수정 폼 열기</button>
    </form>
</div>

<!-- 5. 삭제 -->
<h2>5. 게시글 삭제 (POST /community/delete)</h2>
<div class="box">
    <form action="${pageContext.request.contextPath}/community/delete" method="post">
        <div class="row">
            <label>postId</label>
            <input type="number" name="postId" value="1" required>
        </div>
        <button type="submit" onclick="return confirm('정말 삭제하시겠습니까?');">삭제</button>
    </form>
</div>

<!-- 6. 댓글 작성 -->
<h2>6. 댓글/대댓글 작성 (POST /community/comment/write)</h2>
<div class="box">
    <form action="${pageContext.request.contextPath}/community/comment/write" method="post">
        <div class="row">
            <label>postId</label>
            <input type="number" name="postId" value="1" required>
        </div>
        <div class="row">
            <label>parentId</label>
            <input type="number" name="parentId" placeholder="대댓글이면 부모 댓글 ID, 아니면 비움">
        </div>
        <div class="row">
            <label>content</label><br>
            <textarea name="content">테스트 댓글입니다.</textarea>
        </div>
        <button type="submit">댓글 등록</button>
    </form>
</div>

<!-- 7. 댓글 삭제 -->
<h2>7. 댓글 삭제 (POST /community/comment/delete)</h2>
<div class="box">
    <form action="${pageContext.request.contextPath}/community/comment/delete" method="post">
        <div class="row">
            <label>commentId</label>
            <input type="number" name="commentId" required>
        </div>
        <div class="row">
            <label>postId</label>
            <input type="number" name="postId" value="1" required>
        </div>
        <button type="submit" onclick="return confirm('댓글을 삭제하시겠습니까?');">댓글 삭제</button>
    </form>
</div>

</body>
</html>
