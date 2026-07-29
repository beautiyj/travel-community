<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>찜 목록</title>
</head>
<body>

	<h1>찜 목록</h1>

	<c:choose>
		<c:when test="${empty wishlist}">
			<p>찜한 장소가 없습니다.</p>
		</c:when>

		<c:otherwise>
			<table border="1">
				<tr>
					<th>찜 번호</th>
					<th>회원 번호</th>
					<th>장소 번호</th>
					<th>찜한 날짜</th>
					<th>삭제</th>
				</tr>

				<c:forEach var="wish" items="${wishlist}">
					<tr>
						<td>${wish.wishlistId}</td>
						<td>${wish.memberId}</td>
						<td>${wish.placeId}</td>
						<td>${wish.createdAt}</td>

						<td>
							<form
			action="${pageContext.request.contextPath}/mypage/wishlist/delete"
				method="post">

			<input type="hidden" name="wishlistId"
				value="${wish.wishlistId}">

			<button type="submit"
				onclick="return confirm('찜 목록에서 삭제하시겠습니까?');">삭제</button>
			</form>
				</td>
					</tr>
			</c:forEach>
		</table>
	</c:otherwise>
</c:choose>

	<br>

	<a href="${pageContext.request.contextPath}/mypage"> 마이 페이지로 돌아가기 </a>

</body>
</html>