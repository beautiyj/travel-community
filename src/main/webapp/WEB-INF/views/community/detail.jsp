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
<link rel="stylesheet" href="${cp}/css/common.css">
<link rel="stylesheet" href="${cp}/css/components/confirmModal.css">
<link rel="stylesheet" href="${cp}/css/community.css">
</head>
<body>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<%-- 로그인 회원 (로그인 담당자의 세션 구조에 맞춰 key 확인) --%>
<c:set var="loginMember" value="${sessionScope.loginMember}" />
<c:set var="isLoggedIn" value="${not empty loginMember}" />

<%-- 본인 글 여부: 세션의 loginMember 는 memberId(Long) 자체
     ※ 로그인 담당자가 MemberDto 를 담기로 하면 loginMember.memberId 로 바꿀 것 --%>
<c:set var="isOwner" value="${isLoggedIn and loginMember eq post.memberId}" />

<div class="detail-container">

  <a href="${cp}/community/list" class="back-link">&lt; 목록으로</a>

  <!-- ───────── 게시글 본문 ───────── -->
  <div class="post-box">

    <div class="post-head">
      <div class="post-head-left">
        <span class="badge badge-${post.category}">${post.category}</span>
        <h1 class="post-title">${post.title}</h1>
      </div>

      <!-- 수정/삭제: 본인 글일 때만 -->
      <c:if test="${isOwner}">
        <div class="post-actions">
          <a href="${cp}/community/edit?postId=${post.postId}" class="btn-edit">수정</a>
          <button type="button" class="btn-delete" onclick="openModal('postDeleteModal')">삭제</button>
        </div>
      </c:if>
    </div>

    <!-- 작성자 / 작성일 / 조회수 -->
    <div class="post-meta">
      <span>작성자 ${post.nickname}</span>
      <span><fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
      <span>조회 <fmt:formatNumber value="${post.readcount}" pattern="#,##0" /></span>
    </div>

    <!-- 이미지 (있을 때만) -->
    <c:if test="${not empty post.imageList}">
      <div class="post-images">
        <c:forEach var="img" items="${post.imageList}">
          <img src="${cp}/upload/${img.imageUrl}" alt="첨부 이미지">
        </c:forEach>
      </div>
    </c:if>

    <!-- 본문 (줄바꿈 유지) -->
    <p class="post-content">${post.content}</p>
  </div>


  <!-- ───────── 댓글 영역 ───────── -->
  <div class="comment-box">

    <h2 class="comment-title">
      댓글 <span class="comment-count">${fn:length(post.commentList)}</span>
    </h2>

    <!-- 댓글 목록 -->
    <div class="comment-list">

      <c:choose>
        <c:when test="${empty post.commentList}">
          <p class="comment-empty">첫 댓글을 남겨보세요!</p>
        </c:when>

        <c:otherwise>
          <%-- 원댓글만 먼저 출력 (parentId 가 없는 것) --%>
          <c:forEach var="comment" items="${post.commentList}">
            <c:if test="${empty comment.parentId}">

              <!-- 원댓글 -->
              <div class="comment">
                <div class="comment-head">
                  <span class="comment-author">${comment.memberName}</span>
                  <span class="comment-date">
                    <fmt:formatDate value="${comment.createdAt}" pattern="yyyy-MM-dd HH:mm" />
                  </span>
                </div>
                <p class="comment-content">${comment.content}</p>

                <!-- 답글 달기 (로그인 시) -->
                <c:if test="${isLoggedIn}">
                  <button type="button" class="btn-reply"
                          onclick="toggleReply(${comment.commentId})">답글 달기</button>

                  <!-- 답글 입력창 (기본 숨김) -->
                  <div id="reply-form-${comment.commentId}" class="reply-form" style="display:none;">
                    <form action="${cp}/community/comment/write" method="post">
                      <input type="hidden" name="postId" value="${post.postId}">
                      <input type="hidden" name="parentId" value="${comment.commentId}">
                      <input type="hidden" name="depth" value="1">
                      <input type="text" name="content"
                             placeholder="${comment.memberName}님에게 답글..." required>
                      <button type="submit">등록</button>
                      <button type="button" onclick="toggleReply(${comment.commentId})">취소</button>
                    </form>
                  </div>
                </c:if>

                <!-- 이 원댓글에 달린 대댓글들 -->
                <div class="reply-list">
                  <c:forEach var="reply" items="${post.commentList}">
                    <c:if test="${reply.parentId == comment.commentId}">
                      <div class="reply">
                        <div class="comment-head">
                          <span class="comment-author">${reply.memberName}</span>
                          <span class="comment-date">
                            <fmt:formatDate value="${reply.createdAt}" pattern="yyyy-MM-dd HH:mm" />
                          </span>
                        </div>
                        <p class="comment-content">${reply.content}</p>
                      </div>
                    </c:if>
                  </c:forEach>
                </div>

              </div>
            </c:if>
          </c:forEach>
        </c:otherwise>
      </c:choose>

    </div>

    <!-- 새 댓글 작성 -->
    <c:choose>
      <c:when test="${isLoggedIn}">
        <form action="${cp}/community/comment/write" method="post" class="comment-form">
          <input type="hidden" name="postId" value="${post.postId}">
          <input type="hidden" name="depth" value="0">
          <textarea name="content" rows="2" placeholder="댓글을 입력하세요..." required></textarea>
          <button type="submit">등록</button>
        </form>
      </c:when>
      <c:otherwise>
        <div class="comment-login">
          <p>댓글을 작성하려면 로그인이 필요합니다</p>
          <a href="${cp}/member/login">로그인하기</a>
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
<script>
  // 답글 입력창 열고/닫기 (React의 replyTo 토글)
  function toggleReply(commentId) {
    const form = document.getElementById('reply-form-' + commentId);
    if (form) {
      form.style.display = (form.style.display === 'none') ? 'block' : 'none';
    }
  }
  // 모달 열기/닫기(openModal/closeModal)는 common.js 에 있음
</script>
</body>
</html>