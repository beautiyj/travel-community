<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
  게시판 카테고리 배지 (재사용 조각 파일)
  위치: /WEB-INF/views/common/postCategoryTag.jsp

  - post.category(일반/모집/후기)를 tagButton.jsp 의 place_type(디자인)으로 매핑하고
    실제 텍스트는 카테고리 원문 그대로 보여줌
  - tagButton.jsp 자체는 관광지 분류(food/stay/tour) 전용으로 그대로 두고,
    "게시판 카테고리 → 색" 매핑 로직은 여기 한 곳에서만 관리
    (list.jsp / detail.jsp 양쪽에 똑같은 c:choose 를 복사해두지 않기 위함)

  파라미터
    category : 일반 / 모집 / 후기  (post.category 값 그대로)

  색 매핑 (tagButton.css 4가지 색 중 가장 가까운 톤으로 맞춘 것. 원래 배지 색과 완전히 같지는 않음)
    후기 → tour(초록) / 모집 → food(주황) / 일반 → default(회색)

  사용 예)
    <jsp:include page="../common/postCategoryTag.jsp">
      <jsp:param name="category" value="${post.category}" />
    </jsp:include>
--%>
<c:choose>
  <c:when test="${param.category eq '후기'}"><c:set var="placeType" value="tour" /></c:when>
  <c:when test="${param.category eq '모집'}"><c:set var="placeType" value="food" /></c:when>
  <c:otherwise><c:set var="placeType" value="" /></c:otherwise>
</c:choose>

<jsp:include page="tagButton.jsp">
  <jsp:param name="place_type" value="${placeType}" />
  <jsp:param name="text"       value="${param.category}" />
</jsp:include>
