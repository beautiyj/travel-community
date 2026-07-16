<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 부모 컴포넌트가 던져줄 식별 ID와 찜 상태값
isBookmarked 파라미터 null이거나 empty 상태면 기본 false(wish-off) 상태=ture, false면 넘겨준 값 그대로 사용하기
placeId는 널 비허용(어떤 숙박/맛집/여행지에 wish 체크했는지) --%>
<c:set var="isBookmarked" value="${empty param.isBookmarked ? false : param.isBookmarked}" />
<c:set var="placeId" value="${param.placeId}" />

<button type="button"
class="btn-wish-trigger"
data-place-id="${placeId}"
data-active="${isBookmarked}"
onclick="toggleWishLocal(this)">

<%-- 클릭했을 때 화면에서 하트 이미지만 바꾸는 로컬 자바스크립트 함수(toggleWishLocal) 호출 --%>
<img src="${pageContext.request.contextPath}/images/icons/${isBookmarked ? 'wish-on.png' : 'wish-off.png'}"
class="wish-icon"
alt="찜하기" />
</button>