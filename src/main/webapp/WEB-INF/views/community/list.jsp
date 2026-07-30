<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>여행 커뮤니티</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/selectableButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/searchbar.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community/community.css">
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div class="container">

  <!-- 헤더: 타이틀 + 글쓰기 버튼 (community.css 의 .list-topbar) -->
  <div class="list-topbar">
    <div>
      <h1>여행 커뮤니티</h1>
      <p>여행 경험을 나누고 함께 소통해보세요</p>
    </div>

    <%-- 로그인 시에만 글쓰기 버튼 노출 --%>
    <c:if test="${not empty sessionScope.loginMember}">
      <a href="${cp}/community/write" class="write-btn">+ 글쓰기</a>
    </c:if>
  </div>

  <!-- 카테고리 필터 + 검색 (한 줄 배치: 카테고리 왼쪽, 검색 오른쪽) -->
  <div class="list-filter-row">
    <!-- 카테고리 필터 (selectableButton 재사용: 선택 상태를 표현하는 전용 컴포넌트)
         ※ 값은 PostCategory enum 의 value 와 동일해야 함 (일반후기 / 방문자인증후기로 분리됨) -->
    <div class="tabs">
      <c:forEach var="c" items="전체,일반,모집,일반후기,방문자인증후기">
        <c:set var="label" value="${c}" />
        <c:if test="${c == '모집'}"><c:set var="label" value="모집(동행)" /></c:if>
        <c:set var="isActive" value="${param.category == c or (empty param.category and c == '전체')}" />

        <jsp:include page="../common/selectableButton.jsp">
          <jsp:param name="text"     value="${label}" />
          <jsp:param name="isActive" value="${isActive}" />
          <jsp:param name="onclick"  value="location.href='${cp}/community/list?category=${c}&q=${param.q}'" />
        </jsp:include>
      </c:forEach>
    </div>

    <!-- 검색 (searchbar 컴포넌트 재사용) -->
    <form action="/community/list" method="get" class="search-wrap">
      <input type="hidden" name="category" value="${param.category}">
    <jsp:include page="/WEB-INF/views/common/searchbar.jsp">
       	<jsp:param name="name"        value="q" />
        <jsp:param name="value"       value="${param.q}" />
        <jsp:param name="btnText"     value="조회" />
        <jsp:param name="placeholder" value="제목, 작성자 검색" />
        <jsp:param name="width"       value="100%" />
    </jsp:include>
</form>
  </div>

  <!-- 목록 테이블 (community.css 의 .table / .row / .table-head / .post-row)
       data-search-keyword: postSearchHighlight.js가 제목/작성자에서 이 검색어를 굵게 강조 표시할 때 사용 -->
  <div class="table" data-search-keyword="<c:out value="${param.q}" />">
    <div class="table-head row">
      <span>카테고리</span>
      <span>제목</span>
      <span>작성자</span>
      <span>작성일</span>
      <span>조회수</span>
    </div>

    <c:choose>
      <%-- 게시글이 있을 때 --%>
      <c:when test="${not empty postList}">
        <c:forEach var="post" items="${postList}">

          <a href="${cp}/community/detail?postId=${post.postId}" class="post-row row">
            <%-- postCategoryTag.jsp 컴포넌트 파일이 없어서 DTO getter로 직접 렌더링
                 (community.css 의 .badge.review/.recruit/.general/.verified 사용) --%>
            <span class="badge ${post.categoryCss}">${post.categoryLabel}</span>
            <span class="post-title js-highlight">${post.title}</span>
            <span class="cell-muted js-highlight">${post.nickname}</span>
            <span class="cell-muted">
              <fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd" />
            </span>
            <span class="cell-muted">
              <fmt:formatNumber value="${post.readcount}" pattern="#,##0" />
            </span>
          </a>
        </c:forEach>
      </c:when>


      <%-- 게시글이 없을 때 --%>
      <c:otherwise>
        <div class="empty">게시글이 없습니다</div>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- 페이지네이션: 결과가 한 페이지(10건)를 넘을 때만 표시.
       처음(«)/이전은 첫 블록(1~10페이지)일 때, 다음/마지막(»)은 마지막 블록일 때 아예 렌더링하지 않음(비활성 표시 아님).
       모든 링크는 category/q를 그대로 유지 (list.jsp 상단 카테고리 탭과 동일한 방식으로 쿼리스트링 구성) --%>
  <c:if test="${totalPages > 1}">
    <center>
      <div class="pagination">
        <c:if test="${startPage > 1}">
          <a class="page-nav" href="${cp}/community/list?category=${param.category}&q=${param.q}&page=1">&laquo;</a>
          <a class="page-nav" href="${cp}/community/list?category=${param.category}&q=${param.q}&page=${startPage - 1}">이전</a>
        </c:if>

        <c:forEach var="p" begin="${startPage}" end="${endPage}">
          <c:choose>
            <c:when test="${p == page}">
              <span class="page-num active">${p}</span>
            </c:when>
            <c:otherwise>
              <a class="page-num" href="${cp}/community/list?category=${param.category}&q=${param.q}&page=${p}">${p}</a>
            </c:otherwise>
          </c:choose>
        </c:forEach>

        <c:if test="${endPage < totalPages}">
          <a class="page-nav" href="${cp}/community/list?category=${param.category}&q=${param.q}&page=${endPage + 1}">다음</a>
          <a class="page-nav" href="${cp}/community/list?category=${param.category}&q=${param.q}&page=${totalPages}">&raquo;</a>
        </c:if>
      </div>
    </center>
  </c:if>
</div>

<script src="${cp}/js/common.js"></script>
<script src="${cp}/js/common/highlightKeyword.js"></script>
<script src="${cp}/js/community/postSearchHighlight.js"></script>
</body>
</html>
