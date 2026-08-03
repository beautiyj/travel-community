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
    <c:url var="adminCssUrl" value="/css/admin/admin.css"/>
    <link rel="stylesheet" href="${commonCssUrl}">
    <link rel="stylesheet" href="${adminCssUrl}">
</head>
<body>
<div class="admin-layout">
    <jsp:include page="common/sidebar.jsp">
        <jsp:param name="activeTab" value="businessApplications"/>
    </jsp:include>

    <main class="admin-main">
        <header class="admin-topbar">
            <h1 class="admin-topbar__title">사업자 인증 신청 관리</h1>
            <span class="admin-topbar__caption">ADMIN</span>
        </header>

        <div class="admin-content">
            <div class="admin-page-heading">
                <div>
                    <h2 class="admin-page-heading__title">인증 신청 내역</h2>
                    <p class="admin-page-heading__description">
                        사업자 회원의 신청 상태를 확인하고 상세 페이지에서 승인 또는 반려할 수 있습니다.
                    </p>
                </div>
            </div>

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
                        <h3 id="application-list-title" class="admin-card__title">전체 신청</h3>
                        <p class="admin-card__description">처리가 필요한 신청은 ‘심사 대기’ 상태로 표시됩니다.</p>
                    </div>
                    <span class="admin-card__count">
                        총 <strong><c:out value="${fn:length(applications)}"/></strong>건
                    </span>
                </div>

                <c:choose>
                    <c:when test="${empty applications}">
                        <div class="admin-empty">
                            <p class="admin-empty__title">접수된 인증 신청이 없습니다.</p>
                            <p class="admin-empty__description">새 신청이 접수되면 이 목록에 표시됩니다.</p>
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
                                <tbody>
                                <c:forEach var="item" items="${applications}">
                                    <c:url var="detailUrl"
                                           value="/admin/business-applications/${item.businessApplicationId}"/>
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
        </div>
    </main>
</div>
</body>
</html>
