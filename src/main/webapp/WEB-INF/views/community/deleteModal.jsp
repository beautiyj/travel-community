<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
  게시글 삭제 확인 모달 (조각 파일)
  - 다른 JSP에서 <jsp:include page="deleteModal.jsp" /> 로 불러 씀
  - post.postId 는 request scope 라 포함되는 페이지에서 그대로 사용 가능
  - cp(contextPath)는 page scope 라 여기서 다시 선언해야 함
--%>
<c:set var="cp" value="${pageContext.request.contextPath}" />

<div id="deleteModal" class="modal-overlay" style="display:none;">
  <div class="modal">
    <h2>게시글 삭제</h2>
    <p>삭제 후 복구할 수 없습니다. 정말 삭제하시겠습니까?</p>
    <form action="${cp}/community/delete" method="post">
      <input type="hidden" name="postId" value="${post.postId}">
      <div class="modal-buttons">
        <button type="button" onclick="closeDeleteModal()">취소</button>
        <button type="submit" class="btn-delete-confirm">삭제</button>
      </div>
    </form>
  </div>
</div>