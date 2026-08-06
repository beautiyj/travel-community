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
        data-wish-btn="true"
        data-active="${isBookmarked}"
        aria-pressed="${isBookmarked}"
        aria-label="찜하기"
        onclick="event.preventDefault(); event.stopPropagation(); return false;">
    <svg class="wish-icon ${isBookmarked ? 'is-active' : ''}"
         viewBox="0 0 24 24"
         xmlns="http://www.w3.org/2000/svg"
         aria-hidden="true">
        <path d="M12.1 21.35c-.38.35-.94.35-1.32 0C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.9 12.85z"/>
    </svg>
</button>