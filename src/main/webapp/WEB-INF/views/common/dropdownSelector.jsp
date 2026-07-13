<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
사용법 (jsp:param으로 전달하는 값):
- dropdownId   : 드롭다운 고유 id (한 페이지에 여러 개 쓸 때 중복 방지용, 필수 항목)
- listAttr     : Controller가 Model에 담은 리스트의 속성명 (예: "regionList", "areaList")
- selectedAttr : 현재 선택된 값이 담긴 속성명 (예: "areaCode", "shopCode")
- selectedNameAttr : 현재 선택된 값의 표시 텍스트가 담긴 속성명 (예: "areaName")
- targetUrl    : 클릭 시 이동할 URL (예: "/tour/list", "/domain/name")
- paramKey     : 쿼리 파라미터 키 이름 (예: "areaCode")
- defaultLabel : 아무것도 선택 안 됐을 때 버튼에 보일 기본 텍스트 (예: "지역 선택")
- width        : 버튼 너비 (px 단위 숫자만, 미입력 시 자동)
--%>

<c:set var="dropdownId" value="${param.dropdownId}" />
<c:set var="dropdownList" value="${requestScope[param.listAttr]}" />
<c:set var="selectedValue" value="${requestScope[param.selectedAttr]}" />
<c:set var="selectedName" value="${requestScope[param.selectedNameAttr]}" />

<div class="btn-group" role="group">

    <a href="${param.targetUrl}"
    class="btn ${empty selectedValue ? 'btn-secondary' : 'btn-outline-secondary'}">
    전체
</a>

<button type="button"
id="btn_${dropdownId}"
class="btn btn-outline-secondary dropdown-toggle"
data-bs-toggle="dropdown"
aria-expanded="false"
<c:if test="${not empty param.width}">style="width:${param.width}px;"</c:if>>
    ${empty selectedValue ? param.defaultLabel : selectedName}
</button>

<ul class="dropdown-menu" aria-labelledby="btn_${dropdownId}">
    <c:forEach var="item" items="${dropdownList}">
        <li>
            <a class="dropdown-item" href="${param.targetUrl}?${param.paramKey}=${item.code}">
                ${item.name}
            </a>
        </li>
    </c:forEach>
</ul>

</div>