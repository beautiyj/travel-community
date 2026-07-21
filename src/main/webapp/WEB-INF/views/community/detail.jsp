<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${post.title}</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/buttonComponent.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/smallButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/tagButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/confirmModal.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/components/wishButton.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/community.css">
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<%-- 로그인 회원 (로그인 담당자의 세션 구조에 맞춰 key 확인) --%>
<c:set var="loginMember" value="${sessionScope.loginMember}" />
<c:set var="isLoggedIn" value="${not empty loginMember}" />

<%-- 본인 글 여부: 세션의 loginMember 는 memberId(Long) 자체
     ※ 로그인 담당자가 MemberDto 를 담기로 하면 loginMember.memberId 로 바꿀 것 --%>
<c:set var="isOwner" value="${isLoggedIn and loginMember eq post.memberId}" />

<div class="container">

  <a href="${cp}/community/list" class="back-link">&lt; 목록으로</a>

  <!-- ───────── 게시글 본문 (community.css 의 .detail-card) ───────── -->
  <div class="detail-card">

    <!-- 배지+제목 / 수정·삭제 버튼 (list.jsp 헤더와 같은 .list-topbar 재사용) -->
    <div class="list-topbar">
      <div>
        <jsp:include page="../common/postCategoryTag.jsp">
          <jsp:param name="category" value="${post.category}" />
        </jsp:include>
        <h2>${post.title}</h2>
      </div>

      <!-- 수정/삭제: 본인 글일 때만 (smallButton 재사용) -->
      <c:if test="${isOwner}">
        <div class="post-actions">
          <jsp:include page="../common/smallButton.jsp">
            <jsp:param name="text"    value="수정" />
            <jsp:param name="onclick" value="location.href='${cp}/community/edit?postId=${post.postId}'" />
          </jsp:include>

          <%-- 삭제는 바로 지우지 않고 confirmModal 을 먼저 띄움 --%>
          <jsp:include page="../common/smallButton.jsp">
            <jsp:param name="text"    value="삭제" />
            <jsp:param name="theme"   value="danger" />
            <jsp:param name="onclick" value="openModal('postDeleteModal')" />
          </jsp:include>
        </div>
      </c:if>
    </div>

    <!-- 작성자 / 작성일 / 조회수 (community.css 의 .detail-meta) -->
    <div class="detail-meta">
      <span>작성자 ${post.nickname}</span>
      <span><fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
      <span>조회 <fmt:formatNumber value="${post.readcount}" pattern="#,##0" /></span>
    </div>

    <!-- 본문 (community.css 의 .detail-body, 줄바꿈 유지) -->
    <p class="detail-body">${post.content}</p>

    <!-- 이미지: sort_order 순으로 저장되어 있다고 가정 (0번이 대표/썸네일 이미지)
         1장뿐이면 화살표 없이 사진만, 여러 장이면 배너처럼 좌우 화살표로 넘김
         ※ 본문 아래로 위치 이동 (원래는 본문 위였음) -->
    <c:if test="${not empty post.imageList}">
      <div class="post-gallery" data-gallery>
        <div class="post-gallery-track" data-gallery-track>
          <c:forEach var="img" items="${post.imageList}">
            <div class="post-gallery-slide">
              <img src="${cp}/upload/${img.imageUrl}" alt="첨부 이미지">
            </div>
          </c:forEach>
        </div>

        <c:if test="${fn:length(post.imageList) gt 1}">
          <button type="button" class="post-gallery-arrow post-gallery-arrow-prev"
                  data-gallery-prev aria-label="이전 사진">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <polyline points="15 5 8 12 15 19"></polyline>
            </svg>
          </button>
          <button type="button" class="post-gallery-arrow post-gallery-arrow-next"
                  data-gallery-next aria-label="다음 사진">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <polyline points="9 5 16 12 9 19"></polyline>
            </svg>
          </button>
          <div class="post-gallery-counter" data-gallery-counter>1 / ${fn:length(post.imageList)}</div>
        </c:if>
      </div>
    </c:if>
  </div>


  <!-- ───────── 댓글 영역 (community.css 의 .comments) ───────── -->
  <div class="comments">

    <div class="comments-head">
      <span class="title">댓글</span>
      <span class="count">${fn:length(post.commentList)}</span>
    </div>

    <c:choose>
      <c:when test="${empty post.commentList}">
        <p class="empty">첫 댓글을 남겨보세요!</p>
      </c:when>

      <c:otherwise>
        <%-- community.css 는 .comment 가 .comments 의 평평한 형제(sibling)로 이어지는 구조를 전제로 함
             (.comment.reply 는 별도 래퍼 없이 margin-left 로만 들여쓰기) --%>
        <c:forEach var="comment" items="${post.commentList}">
          <c:if test="${empty comment.parentId}">

            <!-- 원댓글 -->
            <div class="comment">
              <div class="avatar"></div>
              <div>
                <div class="comment-head">
                  <span class="comment-author">${comment.memberName}</span>
                  <span class="comment-date">
                    <fmt:formatDate value="${comment.createdAt}" pattern="yyyy-MM-dd HH:mm" />
                  </span>
                </div>
                <p class="comment-text">${comment.content}</p>

                <!-- 답글 달기 (로그인 시) -->
                <c:if test="${isLoggedIn}">
                  <span class="reply-link" role="button" tabindex="0"
                        onclick="toggleReply(${comment.commentId})">답글 달기</span>
                </c:if>
              </div>
            </div>

            <!-- 답글 입력창 (기본 숨김, community.css 대상 아님) -->
            <c:if test="${isLoggedIn}">
              <div id="reply-form-${comment.commentId}" class="reply-form" style="display:none;">
                <form action="${cp}/community/comment/write" method="post" class="reply-form-row">
                  <input type="hidden" name="postId" value="${post.postId}">
                  <input type="hidden" name="parentId" value="${comment.commentId}">
                  <input type="hidden" name="depth" value="1">
                  <input type="text" name="content" class="reply-input"
                         placeholder="${comment.memberName}님에게 답글..." required>

                  <jsp:include page="../common/smallButton.jsp">
                    <jsp:param name="text" value="등록" />
                  </jsp:include>

                  <button type="button" class="reply-cancel-btn" onclick="toggleReply(${comment.commentId})">취소</button>
                </form>
              </div>
            </c:if>

            <!-- 이 원댓글에 달린 대댓글들: .comment.reply 로 평평하게 이어짐 -->
            <c:forEach var="reply" items="${post.commentList}">
              <c:if test="${reply.parentId == comment.commentId}">
                <div class="comment reply">
                  <div class="avatar sm"></div>
                  <div>
                    <div class="comment-head">
                      <span class="comment-author">${reply.memberName}</span>
                      <span class="comment-date">
                        <fmt:formatDate value="${reply.createdAt}" pattern="yyyy-MM-dd HH:mm" />
                      </span>
                    </div>
                    <p class="comment-text">${reply.content}</p>
                  </div>
                </div>
              </c:if>
            </c:forEach>

          </c:if>
        </c:forEach>
      </c:otherwise>
    </c:choose>

    <!-- 새 댓글 작성 (community.css 의 .comment-form + smallButton 등록 버튼) -->
    <c:choose>
      <c:when test="${isLoggedIn}">
        <form action="${cp}/community/comment/write" method="post" class="comment-form">
          <input type="hidden" name="postId" value="${post.postId}">
          <input type="hidden" name="depth" value="0">
          <textarea name="content" rows="2" placeholder="댓글을 입력하세요..." required></textarea>

          <jsp:include page="../common/smallButton.jsp">
            <jsp:param name="text" value="등록" />
          </jsp:include>
        </form>
      </c:when>

      <%-- 비로그인: 안내 문구 + buttonComponent 로 만든 "로그인하기" 버튼 --%>
      <c:otherwise>
        <div class="comment-login">
          <p>댓글을 작성하려면 로그인이 필요합니다</p>

          <div class="btn-nav-wrap" data-btn-nav="${cp}/member/login">
            <jsp:include page="../common/buttonComponent.jsp">
              <jsp:param name="text" value="로그인하기" />
            </jsp:include>
          </div>
        </div>
      </c:otherwise>
    </c:choose>

  </div>
</div>


<!-- ───────── 삭제 확인 모달 (조각 파일) ───────── -->
<c:if test="${isOwner}">
  <jsp:include page="../common/confirmModal.jsp">
    <jsp:param name="modalId"     value="postDeleteModal" />
    <jsp:param name="action"      value="/community/delete" />
    <jsp:param name="title"       value="게시글 삭제" />
    <jsp:param name="message"     value="삭제 후 복구할 수 없습니다. 정말 삭제하시겠습니까?" />
    <jsp:param name="confirmText" value="삭제" />
    <jsp:param name="hiddenName"  value="postId" />
    <jsp:param name="hiddenValue" value="${post.postId}" />
  </jsp:include>
</c:if>


<script src="${cp}/js/common.js"></script>
<script src="${cp}/js/community/postDetail.js"></script>
</body>
</html>
