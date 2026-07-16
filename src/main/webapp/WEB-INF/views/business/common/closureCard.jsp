<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
즉시 예약 마감 토글 카드 (마감 관리 탭 공용 컴포넌트)

사용법 (jsp:param으로 전달하는 값):
- placeId, memberId : 토글 API(/api/business/place/closed) 호출에 필요 (필수)
- isClosed          : 현재 마감 상태 (boolean, 필수)

실제 상태 변경은 /js/business-closure.js 가 이벤트 위임으로 처리하므로,
이 카드가 한 페이지에 여러 개 있어도(향후 재사용 시) 동일하게 동작한다.
--%>
<div class="business-card business-closure-card" data-place-id="${param.placeId}" data-member-id="${param.memberId}">
    <div class="business-closure-card__head">
        <div>
            <h2 class="business-card__title" style="margin-bottom: 4px;">즉시 예약 마감</h2>
            <p class="business-closure-card__desc">토글 ON 시 즉시 예약을 받지 않습니다</p>
        </div>
        <button type="button" class="business-toggle${param.isClosed ? ' is-on' : ''}" aria-pressed="${param.isClosed}">
            <span class="business-toggle__thumb"></span>
        </button>
    </div>
    <div class="business-closure-banner${param.isClosed ? ' business-closure-banner--closed' : ' business-closure-banner--open'}">
        <span class="business-status-dot business-status-dot--${param.isClosed ? 'closed' : 'open'}"></span>
        <span class="business-closure-banner__text"><c:choose>
            <c:when test="${param.isClosed}">현재 예약 마감 상태입니다. 신규 예약이 차단됩니다.</c:when>
            <c:otherwise>예약을 정상적으로 받고 있습니다.</c:otherwise>
        </c:choose></span>
    </div>
</div>
