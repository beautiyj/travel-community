<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<br><br>
<footer style="background-color: #f1f1f1; padding: 20px; text-align: center; font-size: 13px; color: #666;">
    <p>© 2026 choongang Travel Community. All rights reserved.</p>
    <p>Icons created by Flaticon - <a href="https://www.flaticon.com" target="_blank">www.flaticon.com</a></p>
    <p>본 서비스는 <strong>한국관광공사 TourAPI</strong>의 공공데이터를 활용하여 제공되며, 공공누리 제1유형(출처표시) 조건에 따라 이용하고 있습니다.</p>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // 서버에서 JSP로 전달되는 contextPath를 전역 변수로 노출
        window.__CONTEXT_PATH__ = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/js/common.js"></script>

</footer>