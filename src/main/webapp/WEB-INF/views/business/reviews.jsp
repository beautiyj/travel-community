<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>후기 확인 - 관리자 - 트립어라운드</title>
    <link rel="stylesheet" href="/css/business.css">
</head>
<body>

<div class="business-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="reviews" />
    </jsp:include>

    <div class="business-main">
        <div class="business-topbar">
            <h1 class="business-topbar__title">후기 확인</h1>
        </div>

        <div class="business-content">
            <div class="business-filter-row">
                <span class="business-filter-row__total">총 ${reviews.size()}건</span>
            </div>

            <c:choose>
                <c:when test="${empty reviews}">
                    <div class="business-card">
                        <p class="business-empty">아직 등록된 후기가 없습니다</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="business-review-list">
                        <c:forEach var="rv" items="${reviews}">
                            <a href="/community/detail?postId=${rv.postId}" class="business-card business-review-card">
                                <p class="business-review-card__title">${rv.title}</p>
                                <p class="business-review-card__content">${rv.content}</p>

                                <div class="business-review-card__meta">
                                    <span class="business-review-card__meta-item">
                                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 8 0 0 1 16 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                        </svg>
                                        ${rv.nickname}
                                    </span>
                                    <span class="business-review-card__meta-item">
                                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                                            <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                                        </svg>
                                        ${rv.createdAtLabel}
                                    </span>
                                    <span class="business-review-card__meta-item">
                                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                                            <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                                        </svg>
                                        ${rv.readcount}
                                    </span>
                                </div>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

</body>
</html>
