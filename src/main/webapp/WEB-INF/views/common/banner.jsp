<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="cp" value="${pageContext.request.contextPath}" />
<c:set var="bannerId" value="${empty param.bannerId ? 'mainBanner' : param.bannerId}" />

<%--
  ── 테스트 데이터 ──────────────────────────────────────────────
  슬라이드 1장 = 배경이미지 ^ 링크 ^ place_type ^ 태그텍스트 ^ 제목 ^ 부제 ^ 버튼텍스트
  슬라이드끼리는 | 로 구분

    place_type  : tour / food / stay, 비우면 기본 태그 디자인
    제목        : ~ 를 넣으면 줄바꿈  (예: 부산 해운대 패키지~2인 특별 혜택)

  ※ 실제 데이터를 붙일 때는 이 c:set 을 지우고
     아래 c:forEach 를 items="${bannerList}" 로 바꾸면 됩니다.
--%>
<c:set var="testSlides" value="
https://picsum.photos/id/1036/1600/500^/tour/list?area=부산^stay^해변 여행^부산 해운대 패키지~2인 특별 혜택^숙박 + 레스토랑 결합 시 10% 추가 할인^패키지 보기|
https://picsum.photos/id/1015/1600/500^/tour/list?area=경주^tour^^천년 고도 경주~가을 야경 투어^첨성대·동궁과 월지 야간 개장 중^일정 보러 가기|
https://picsum.photos/id/292/1600/500^/tour/list?area=제주^food^^제주 미식 로드~현지인 추천 코스^흑돼지부터 해물뚝배기까지 한 번에^맛집 보기|
https://picsum.photos/id/1039/1600/500^/community/list^^추천^여행자들의 이야기^다녀온 후기와 동행 모집을 한곳에서^커뮤니티 가기
" />

<div id="${bannerId}" class="banner" data-banner>

  <%-- 슬라이드 트랙: JS 가 --banner-index 를 바꾸면 옆으로 밀림 --%>
  <div class="banner-track" data-banner-track>
    <c:forEach var="row" items="${fn:split(testSlides, '|')}">
      <c:set var="col" value="${fn:split(row, '^')}" />

      <div class="banner-slide">
        <img class="banner-bg" src="${fn:trim(col[0])}" alt="">

        <div class="banner-caption">
          <%-- 태그 컴포넌트 재사용 --%>
          <jsp:include page="tagButton.jsp">
            <jsp:param name="place_type" value="${fn:trim(col[2])}" />
            <jsp:param name="text"       value="${fn:trim(col[3])}" />
          </jsp:include>

          <%-- 제목: ~ 를 줄바꿈으로 --%>
          <h2 class="banner-title">
            <c:forEach var="line" items="${fn:split(col[4], '~')}" varStatus="ls">
              ${fn:trim(line)}<c:if test="${not ls.last}"><br></c:if>
            </c:forEach>
          </h2>

          <p class="banner-desc">${fn:trim(col[5])}</p>

          <%-- CTA 버튼: smallButton 재사용 (흰 배경으로 보이도록 banner.css 에서 override) --%>
          <div class="banner-cta-wrap">
            <jsp:include page="smallButton.jsp">
              <jsp:param name="text"    value="${fn:trim(col[6])}" />
              <jsp:param name="onclick" value="location.href='${cp}${fn:trim(col[1])}'" />
            </jsp:include>
          </div>
        </div>
      </div>
    </c:forEach>
  </div>

  <%-- 좌우 꺽쇠: wishButton 의 원형 버튼(.btn-wish-trigger, 48x48 흰 원)을 재사용하고
       아이콘만 하트 대신 화살표 SVG 로 교체. 위치(prev/next)만 banner.css 에서 따로 지정 --%>
  <button type="button" class="btn-wish-trigger banner-arrow-prev"
          data-banner-prev aria-label="이전 배너">
    <svg class="banner-chevron" viewBox="0 0 24 24" aria-hidden="true">
      <polyline points="15 5 8 12 15 19" />
    </svg>
  </button>

  <button type="button" class="btn-wish-trigger banner-arrow-next"
          data-banner-next aria-label="다음 배너">
    <svg class="banner-chevron" viewBox="0 0 24 24" aria-hidden="true">
      <polyline points="9 5 16 12 9 19" />
    </svg>
  </button>

  <%-- 인디케이터: 클릭하면 해당 슬라이드로 이동 --%>
  <div class="banner-dots" data-banner-dots>
    <c:forEach var="row" items="${fn:split(testSlides, '|')}" varStatus="st">
      <button type="button"
              class="banner-dot ${st.first ? 'is-active' : ''}"
              data-banner-dot="${st.index}"
              aria-label="${st.count}번째 배너로 이동"></button>
    </c:forEach>
  </div>

</div>
