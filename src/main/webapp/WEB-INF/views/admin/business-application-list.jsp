<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>사업자 인증 신청 관리 - 갈래말래</title>
    <c:url var="commonCssUrl" value="/css/common.css"/>
    <c:url var="adminCssUrl" value="/css/admin/admin.css">
        <c:param name="v" value="auth-admin-css-split-20260806-r7"/>
    </c:url>
    <c:url var="businessApplicationListCssUrl" value="/css/admin/business-application-list.css">
        <c:param name="v" value="auth-admin-css-split-20260806-r7"/>
    </c:url>
    <c:url var="businessApplicationListJsUrl" value="/js/admin/business-application-list.js"/>
    <c:url var="adminBfcacheReloadJsUrl" value="/js/admin/admin-bfcache-reload.js"/>
    <c:url var="allApplicationsUrl" value="/admin/business-applications">
        <c:param name="status" value="ALL"/>
    </c:url>
    <c:url var="pendingApplicationsUrl" value="/admin/business-applications">
        <c:param name="status" value="PENDING"/>
    </c:url>
    <c:url var="rejectedApplicationsUrl" value="/admin/business-applications">
        <c:param name="status" value="REJECTED"/>
    </c:url>
    <c:url var="approvedApplicationsUrl" value="/admin/business-applications">
        <c:param name="status" value="APPROVED"/>
    </c:url>
    <link rel="stylesheet" href="${commonCssUrl}">
    <link rel="stylesheet" href="${adminCssUrl}">
    <link rel="stylesheet" href="${businessApplicationListCssUrl}">
</head>
<body>
<div class="admin-layout">
    <%-- 이 화면과 상세 URL은 모두 서버의 관리자 인가를 통과한 요청에만 제공되어야 한다. --%>
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="businessApplications"/>
    </jsp:include>

    <main class="admin-main">
        <header class="admin-topbar">
            <h1 class="admin-topbar__title">사업자 인증 신청 관리</h1>
            <span class="admin-topbar__caption">ADMIN</span>
        </header>

        <div class="admin-content admin-content--business-applications">
            <%--
              컨트롤러가 정규화한 selectedStatus와 해당 상태로 조회한 applications를 모델로 전달한다.
              선택된 상태에 맞춰 제목·안내·빈 목록 문구를 서버 렌더링 시 함께 바꾼다.
            --%>
            <c:choose>
                <c:when test="${selectedStatus eq 'PENDING'}">
                    <c:set var="filterTitle" value="심사 대기 신청"/>
                    <c:set var="filterDescription" value="심사 대기 중인 사업자 인증 신청을 최신순으로 표시합니다."/>
                    <c:set var="emptyMessage" value="심사 대기 중인 사업자 인증 신청이 없습니다."/>
                </c:when>
                <c:when test="${selectedStatus eq 'REJECTED'}">
                    <c:set var="filterTitle" value="반려 신청"/>
                    <c:set var="filterDescription" value="반려된 사업자 인증 신청을 최신순으로 표시합니다."/>
                    <c:set var="emptyMessage" value="반려된 사업자 인증 신청이 없습니다."/>
                </c:when>
                <c:when test="${selectedStatus eq 'APPROVED'}">
                    <c:set var="filterTitle" value="승인 신청"/>
                    <c:set var="filterDescription" value="승인된 사업자 인증 신청을 최신순으로 표시합니다."/>
                    <c:set var="emptyMessage" value="승인된 사업자 인증 신청이 없습니다."/>
                </c:when>
                <c:otherwise>
                    <c:set var="filterTitle" value="전체 신청"/>
                    <c:set var="filterDescription" value="모든 사업자 인증 신청을 최신순으로 표시합니다."/>
                    <c:set var="emptyMessage" value="접수된 사업자 인증 신청이 없습니다."/>
                </c:otherwise>
            </c:choose>
            <div class="admin-page-heading">
                <div>
                    <h2 class="admin-page-heading__title">인증 신청 내역</h2>
                    <p class="admin-page-heading__description">
                        사업자 회원의 신청 상태를 확인하고 상세 페이지에서 승인 또는 반려할 수 있습니다.
                    </p>
                </div>
            </div>

            <%-- 상태는 서버가 필터링하므로 탭은 GET 링크로 현재 조건을 URL에 남깁니다. --%>
            <nav class="admin-filter-tabs" aria-label="사업자 인증 신청 상태 필터">
                <a class="admin-btn admin-btn--outline admin-btn--sm admin-filter-tabs__link${selectedStatus eq 'ALL' ? ' is-active' : ''}"
                   href="${allApplicationsUrl}"
                   aria-current="${selectedStatus eq 'ALL' ? 'page' : 'false'}">전체</a>
                <a class="admin-btn admin-btn--outline admin-btn--sm admin-filter-tabs__link${selectedStatus eq 'PENDING' ? ' is-active' : ''}"
                   href="${pendingApplicationsUrl}"
                   aria-current="${selectedStatus eq 'PENDING' ? 'page' : 'false'}">심사 대기</a>
                <a class="admin-btn admin-btn--outline admin-btn--sm admin-filter-tabs__link${selectedStatus eq 'REJECTED' ? ' is-active' : ''}"
                   href="${rejectedApplicationsUrl}"
                   aria-current="${selectedStatus eq 'REJECTED' ? 'page' : 'false'}">반려</a>
                <a class="admin-btn admin-btn--outline admin-btn--sm admin-filter-tabs__link${selectedStatus eq 'APPROVED' ? ' is-active' : ''}"
                   href="${approvedApplicationsUrl}"
                   aria-current="${selectedStatus eq 'APPROVED' ? 'page' : 'false'}">승인</a>
            </nav>

            <%-- 심사 POST 후 PRG redirect가 붙인 결과 파라미터와 서버 오류 모델을 사용자 안내로 분기한다. --%>
            <c:if test="${param.reviewed eq 'approved'}">
                <div class="admin-alert admin-alert--success" role="status">
                    사업자 인증 신청을 승인했습니다.
                </div>
            </c:if>
            <c:if test="${param.reviewed eq 'rejected'}">
                <div class="admin-alert admin-alert--success" role="status">
                    사업자 인증 신청을 반려했습니다.
                </div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <div class="admin-alert admin-alert--error" role="alert">
                    <c:out value="${errorMessage}"/>
                </div>
            </c:if>

            <section class="admin-card" aria-labelledby="application-list-title">
                <div class="admin-card__header">
                    <div>
                        <h3 id="application-list-title" class="admin-card__title"><c:out value="${filterTitle}"/></h3>
                        <p class="admin-card__description"><c:out value="${filterDescription}"/></p>
                    </div>
                    <span class="admin-card__count">
                        <c:out value="${filterTitle}"/> <strong><c:out value="${fn:length(applications)}"/></strong>건
                    </span>
                </div>

                <c:choose>
                    <c:when test="${empty applications}">
                        <div class="admin-empty">
                            <p class="admin-empty__title"><c:out value="${emptyMessage}"/></p>
                            <p class="admin-empty__description">다른 상태의 신청은 위 탭에서 확인할 수 있습니다.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="admin-table-wrap"
                             tabindex="0"
                             aria-label="사업자 인증 신청 목록">
                            <table class="admin-table">
                                <caption class="admin-sr-only">사업자 인증 신청 목록</caption>
                                <thead>
                                <tr>
                                    <th scope="col">신청 번호</th>
                                    <th scope="col">회원 번호</th>
                                    <th scope="col">이름</th>
                                    <th scope="col">신청일</th>
                                    <th scope="col">상태</th>
                                    <th scope="col"><span class="admin-sr-only">상세 보기</span></th>
                                </tr>
                                </thead>
                                <%-- 서버가 선택한 상태의 목록을 전달하고, 화면에서는 해당 결과를 10건씩 나눠 보여 준다. --%>
                                <tbody data-business-application-rows>
                                <c:forEach var="item" items="${applications}">
                                    <%-- 상세 화면에서 같은 필터의 목록으로 돌아갈 수 있도록 현재 상태를 쿼리에 유지한다. --%>
                                    <c:url var="detailUrl"
                                           value="/admin/business-applications/${item.businessApplicationId}">
                                        <c:param name="status" value="${selectedStatus}"/>
                                    </c:url>
                                    <tr>
                                        <td data-label="신청 번호">
                                            <c:out value="${item.businessApplicationId}"/>
                                        </td>
                                        <td data-label="회원 번호">
                                            <c:out value="${item.memberId}"/>
                                        </td>
                                        <td data-label="이름" class="admin-table__name">
                                            <c:out value="${item.name}"/>
                                        </td>
                                        <td data-label="신청일">
                                            <c:out value="${item.appliedAtDisplay}"/>
                                        </td>
                                        <td data-label="상태">
                                            <c:choose>
                                                <c:when test="${item.applicationStatus eq 'PENDING'}">
                                                    <span class="admin-status admin-status--pending">심사 대기</span>
                                                </c:when>
                                                <c:when test="${item.applicationStatus eq 'APPROVED'}">
                                                    <span class="admin-status admin-status--approved">승인</span>
                                                </c:when>
                                                <c:when test="${item.applicationStatus eq 'REJECTED'}">
                                                    <span class="admin-status admin-status--rejected">반려</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="admin-status">
                                                        <c:out value="${item.applicationStatus}"/>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="admin-table__action">
                                            <a class="admin-btn admin-btn--outline admin-btn--sm"
                                               href="${detailUrl}">
                                                상세 보기
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
            <c:if test="${not empty applications}">
                <%-- 페이지 번호는 JavaScript가 현재 페이지 기준 5개 블록으로 채운다. 전체 데이터는 이미 DOM에 렌더링되어 있다. --%>
                <nav class="admin-pagination"
                     data-business-application-pagination
                     aria-label="사업자 인증 신청 목록 페이지">
                </nav>
            </c:if>
        </div>
    </main>
</div>
<script src="${adminBfcacheReloadJsUrl}" defer></script>
<script src="${businessApplicationListJsUrl}" defer></script>
</body>
</html>
