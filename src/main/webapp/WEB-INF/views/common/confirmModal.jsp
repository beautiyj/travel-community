<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />

<%-- 파라미터 기본값 처리 --%>
<c:set var="mId"      value="${empty param.modalId     ? 'confirmModal' : param.modalId}" />
<c:set var="mMethod"  value="${empty param.method      ? 'post'         : param.method}" />
<c:set var="mTitle"   value="${empty param.title       ? '확인'         : param.title}" />
<c:set var="mMessage" value="${empty param.message     ? '이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?' : param.message}" />
<c:set var="mOk"      value="${empty param.confirmText ? '확인'         : param.confirmText}" />
<c:set var="mCancel"  value="${empty param.cancelText  ? '취소'         : param.cancelText}" />
<c:set var="mTone"    value="${param.tone eq 'primary' ? 'primary'      : 'danger'}" />

<%-- hidden 이름이 하나도 없으면 JS 주입용으로 id 하나만 둠 --%>
<c:set var="hNames"  value="${empty paramValues.hiddenName ? null : paramValues.hiddenName}" />
<c:set var="hCount"  value="${empty hNames ? 0 : fn:length(hNames)}" />

<c:choose>
  <c:when test="${empty param.action}">
    <%-- action 누락은 조용히 넘어가면 디버깅이 어려우므로 화면에 표시 --%>
    <!-- confirmModal(${mId}): action 파라미터가 필요합니다 -->
  </c:when>
  <c:otherwise>
    <div id="${mId}" class="modal-overlay" data-modal
         role="dialog" aria-modal="true" aria-labelledby="${mId}-title">
      <div class="modal">
        <h2 class="modal-title" id="${mId}-title"><c:out value="${mTitle}" /></h2>
        <p class="modal-message"><c:out value="${mMessage}" /></p>

        <form action="${cp}${param.action}" method="${mMethod}">
          <c:choose>
            <c:when test="${hCount eq 0}">
              <input type="hidden" name="id" value="" data-modal-value>
            </c:when>
            <c:otherwise>
              <c:forEach var="i" begin="0" end="${hCount - 1}">
                <input type="hidden"
                       name="${hNames[i]}"
                       value="${paramValues.hiddenValue[i]}"
                       <c:if test="${i eq 0}">data-modal-value</c:if>>
              </c:forEach>
            </c:otherwise>
          </c:choose>

          <div class="modal-buttons">
            <%-- 취소: 흰 배경 + 검은 글자
                 - color 로 배경만 넘기고, 글자색은 confirmModal.css 에서 override
                 - 감싼 div 의 data-modal-close 로 닫힘 (클릭이 위로 전파됨) --%>
            <div class="modal-btn modal-btn-cancel" data-modal-close>
              <jsp:include page="buttonComponent.jsp">
                <jsp:param name="text"  value="${mCancel}" />
                <jsp:param name="color" value="var(--card)" />
                <jsp:param name="size"  value="var(--text-sm)" />
              </jsp:include>
            </div>

            <%-- 확정: tone=danger 면 빨강+흰 글자, primary 면 파랑 --%>
            <div class="modal-btn modal-btn-confirm">
              <jsp:include page="buttonComponent.jsp">
                <jsp:param name="text"  value="${mOk}" />
                <jsp:param name="color" value="${mTone eq 'primary' ? 'var(--primary)' : 'var(--destructive)'}" />
                <jsp:param name="size"  value="var(--text-sm)" />
              </jsp:include>
            </div>
          </div>
        </form>
      </div>
    </div>
  </c:otherwise>
</c:choose>
