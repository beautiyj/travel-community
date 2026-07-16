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
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/smallButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/searchbar.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community.css">
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

  <!-- 카테고리 필터 (smallButton 재사용: 선택된 탭 = primary, 나머지 = secondary) -->
  <div class="tabs">
    <c:forEach var="c" items="전체,일반,모집,후기">
      <c:set var="label" value="${c}" />
      <c:if test="${c == '모집'}"><c:set var="label" value="모집 (동행)" /></c:if>
      <c:set var="isActive" value="${param.category == c or (empty param.category and c == '전체')}" />

      <jsp:include page="../common/smallButton.jsp">
        <jsp:param name="text"    value="${label}" />
        <jsp:param name="theme"   value="${isActive ? 'primary' : 'secondary'}" />
        <jsp:param name="onclick" value="location.href='${cp}/community/list?category=${c}&q=${param.q}'" />
      </jsp:include>
    </c:forEach>
  </div>

  <!-- 검색 (searchbar 컴포넌트 재사용) -->
  <form action="${cp}/community/list" method="get" class="search-wrap">
    <input type="hidden" name="category" value="${param.category}">
    <jsp:include page="../common/searchbar.jsp">
      <jsp:param name="name"        value="q" />
      <jsp:param name="value"       value="${param.q}" />
      <jsp:param name="placeholder" value="제목, 작성자 검색" />
    </jsp:include>
  </form>

  <!-- 목록 테이블 (community.css 의 .table / .row / .table-head / .post-row) -->
  <div class="table">
    <div class="table-head row">
      <span>카테고리</span>
      <span></span>
      <span>제목</span>
      <span>작성자</span>
      <span>작성일</span>
      <span>조회수</span>
    </div>

    <c:choose>
      <%-- 게시글이 있을 때 --%>
      <c:when test="${not empty postList}">
        <c:forEach var="post" items="${postList}">

          <%-- 카테고리(한글) → tagButton 의 place_type(영문) 매핑
               tagButton 은 원래 장소 분류(food/stay/tour)용이라 완전히 같은 색은 아니지만
               가장 가까운 톤으로 맞춤: 후기→tour(초록) / 모집→food(주황) / 일반→default(회색) --%>
          <c:choose>
            <c:when test="${post.category eq '후기'}"><c:set var="placeType" value="tour" /></c:when>
            <c:when test="${post.category eq '모집'}"><c:set var="placeType" value="food" /></c:when>
            <c:otherwise><c:set var="placeType" value="" /></c:otherwise>
          </c:choose>

          <a href="${cp}/community/detail?postId=${post.postId}" class="post-row row">
            <jsp:include page="../common/tagButton.jsp">
              <jsp:param name="place_type" value="${placeType}" />
              <jsp:param name="text"       value="${post.category}" />
            </jsp:include>
            <%-- 썸네일: 이 글의 sort_order=0 인 이미지. 없으면 빈 회색 박스 --%>
            <span class="thumb">
              <c:if test="${not empty post.thumbnailUrl}">
                <img src="${cp}/upload/${post.thumbnailUrl}" alt="">
              </c:if>
            </span>
            <span class="post-title">${post.title}</span>
            <span class="cell-muted">${post.nickname}</span>
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
</div>

<script src="${cp}/js/common.js"></script>
</body>
</html>
