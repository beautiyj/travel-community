<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%-- 장소 검색 모달 (재사용 컴포넌트)
     - confirmModal.jsp 와는 별개의 새 모달 (수정 금지 규칙과 무관하게 자유롭게 손볼 수 있음)
     - 사용법: jsp:include 시 modalId 파라미터로 고유 id 지정
       <jsp:include page="../common/placeSearchModal.jsp">
         <jsp:param name="modalId" value="placeSearchModal" />
       </jsp:include>
     - 검색 결과를 클릭하면 상위 페이지에 정의된 window.selectPlaceTag(placeId, placeName)를 호출함
       (write.jsp / edit.jsp 에서 placeTag.js를 통해 이 함수를 정의함)
     - 열고 닫는 것도 이 파일 자체 스크립트로 처리 (common.js의 openModal/closeModal에 의존하지 않음) --%>
<div id="${param.modalId}" class="place-search-modal-overlay">
  <div class="place-search-modal">
    <div class="place-search-modal-head">
      <span class="place-search-modal-title">장소 검색</span>
      <button type="button" class="place-search-modal-close"
              onclick="document.getElementById('${param.modalId}').classList.remove('open')">✕</button>
    </div>

    <input type="text" class="place-search-input"
           placeholder="장소 이름을 입력하세요"
           oninput="searchPlaceTag(this)"
           autocomplete="off">

    <div class="place-search-results"></div>
  </div>
</div>
