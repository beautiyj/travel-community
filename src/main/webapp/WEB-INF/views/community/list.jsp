<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>여행 커뮤니티</title>
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div class="community-container">

  <!-- 헤더 -->
  <div class="community-header">
    <div>
      <h1>여행 커뮤니티</h1>
      <p>여행 경험을 나누고 함께 소통해보세요</p>
    </div>

    <%-- 로그인 시에만 글쓰기 버튼 노출 (React의 isLoggedIn 조건) --%>
    <c:if test="${not empty sessionScope.loginMember}">
      <a href="${cp}/community/write" class="btn-write">+ 글쓰기</a>
    </c:if>
  </div>

  <!-- 카테고리 필터 (React의 cat state → GET 파라미터) -->
  <div class="category-filter">
    <c:forEach var="c" items="전체,일반,모집,후기">
      <a href="${cp}/community/list?category=${c}&q=${param.q}"
         class="cat-btn ${param.category == c or (empty param.category and c == '전체') ? 'active' : ''}">
        <c:choose>
          <c:when test="${c == '모집'}">모집 (동행)</c:when>
          <c:otherwise>${c}</c:otherwise>
        </c:choose>
      </a>
    </c:forEach>
  </div>

  <!-- 검색 -->
  <form action="${cp}/community/list" method="get" class="search-box">
    <input type="hidden" name="category" value="${param.category}">
    <input type="text" name="q" value="${param.q}" placeholder="제목, 작성자 검색">
    <button type="submit">검색</button>
  </form>

  <!-- 목록 테이블 -->
  <div class="post-table">
    <div class="post-table-head">
      <span class="col-category">카테고리</span>
      <span class="col-title">제목</span>
      <span class="col-author">작성자</span>
      <span class="col-date">작성일</span>
      <span class="col-views">조회</span>
    </div>

    <c:choose>
      <%-- 게시글이 있을 때 --%>
      <c:when test="${not empty postList}">
        <c:forEach var="post" items="${postList}">
          <a href="${cp}/community/detail?postId=${post.postId}" class="post-row">
            <span class="col-category">
              <%-- React의 PostCategoryBadge --%>
              <span class="badge badge-${post.category}">${post.category}</span>
            </span>
            <span class="col-title">${post.title}</span>
            <span class="col-author">${post.memberName}</span>
            <span class="col-date">
              <fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd" />
            </span>
            <span class="col-views">
              <fmt:formatNumber value="${post.readcount}" pattern="#,##0" />
            </span>
          </a>
        </c:forEach>
      </c:when>

      <%-- 게시글이 없을 때 (React의 filtered.length === 0) --%>
      <c:otherwise>
        <div class="empty">게시글이 없습니다</div>
      </c:otherwise>
    </c:choose>
  </div>
</div>
</body>
</html>