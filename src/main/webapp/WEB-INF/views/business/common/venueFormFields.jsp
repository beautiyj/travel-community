<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%--
업소 등록/수정 폼 공통 입력 필드 (업소명 / 업종 / 주소 / 소개)

사용법 (jsp:param으로 전달하는 값):
- idPrefix    : input id 접두어 (등록: 미전달(빈 값) / 수정: "edit-")
- name        : 업소명 값 (등록은 미전달 → 빈 값, 수정은 기존 값 채움)
- placeType   : 업종 코드 값 ("stay"/"food"/"tour", 등록은 미전달)
- priceMode   : 가격 선택 ("FIXED"/"FREE", 등록은 미전달 → 가격입력이 기본 선택)
- minPrice    : 가격입력 선택 시의 금액 (등록은 미전달)
- address     : 주소 값 (등록은 미전달). region_id는 이 주소로 서버가 자동 매핑한다
- addressDetail : 상세주소 값 (등록은 미전달)
- description : 소개 값 (등록은 미전달)
- hashtags    : 해시태그 값, 콤마로 구분된 원문 그대로 (등록은 미전달)
--%>
<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}name">업소명</label>
    <input class="business-form-input" type="text" id="${param.idPrefix}name" name="name" value="${param.name}" required />
</div>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}placeType">업종</label>
    <select class="business-form-select" id="${param.idPrefix}placeType" name="placeType" required>
        <option value="stay" ${param.placeType == 'stay' ? 'selected' : ''}>숙박</option>
        <option value="food" ${param.placeType == 'food' ? 'selected' : ''}>맛집</option>
        <option value="tour" ${param.placeType == 'tour' ? 'selected' : ''}>관광지</option>
    </select>
</div>

<%--
가격 설정: 업종(숙박/맛집/관광지) 구분 없이 모든 업소에 동일하게 노출한다.
"무료"를 고르면 금액 input이 닫히고 0으로 저장되며, "가격입력"을 고른 경우에만 금액이 열린다.
(표시/숨김 토글은 business-venue.js)
--%>
<c:set var="priceMode" value="${empty param.priceMode ? 'FIXED' : param.priceMode}" />
<div class="business-form-group js-price-group" id="${param.idPrefix}priceGroup">
    <label class="business-form-label">가격</label>
    <div class="business-price-options">
        <label class="business-price-option">
            <input type="radio" name="priceMode" value="FIXED" ${priceMode == 'FIXED' ? 'checked' : ''} />
            <span>가격입력</span>
        </label>
        <label class="business-price-option">
            <input type="radio" name="priceMode" value="FREE" ${priceMode == 'FREE' ? 'checked' : ''} />
            <span>무료</span>
        </label>
    </div>

    <div class="business-price-input js-price-input${priceMode == 'FIXED' ? '' : ' is-hidden'}">
        <input class="business-form-input" type="number" id="${param.idPrefix}minPrice" name="minPrice"
               min="0" step="1000" placeholder="1인 기준 금액" value="${param.minPrice}" />
        <span class="business-price-input__unit">원</span>
    </div>
    <p class="business-price-hint">무료는 결제 없이 예약만 받습니다.</p>
</div>
<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}address">주소</label>
    <div class="business-form-row">
        <input class="business-form-input" type="text" id="${param.idPrefix}address" name="address" value="${param.address}" readonly required />
        <button type="button" class="business-btn business-btn--outline js-address-search" data-target-prefix="${param.idPrefix}">주소 검색</button>
    </div>
    <input class="business-form-input" type="text" id="${param.idPrefix}addressDetail" name="addressDetail" placeholder="상세주소 입력" value="${param.addressDetail}" />
</div>

<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}description">소개</label>
    <textarea class="business-form-textarea" id="${param.idPrefix}description" name="description" rows="4">${param.description}</textarea>
</div>

<%--
부가정보 (PLACE.extra_info)

2단계로 입력받는다.
 1) 업종(위의 placeType select)을 고르면 그 업종의 항목 칩 목록으로 갈아끼워진다.
 2) 칩을 고르면 그 항목의 입력칸만 아래에 나타난다. (business-venue.js)

세 업종 목록을 모두 그려두고 JS가 선택된 업종 것만 남긴다. 입력칸은 기본이 disabled이고
칩을 골라야 활성화된다. disabled 필드는 전송 대상에서 제외되므로
 - 고르지 않은 항목, 선택하지 않은 업종의 항목은 아예 서버로 가지 않고
 - JS가 동작하지 않는 환경에서도 엉뚱한 값이 저장되지 않는다.

각 줄은 "라벨 hidden + 값 text" 한 쌍으로 제출되고, 서버(BusinessExtraInfoCatalog.assemble)가
"[라벨] 값"을 줄바꿈으로 이어붙여 한 컬럼에 저장한다.

라벨 문구는 공공데이터 배치(TourApiHelper)와 표기를 맞춘 값이라 화면에서 임의로 바꾸지 않는다.
목록 자체는 컨트롤러가 BusinessExtraInfoCatalog에서 내려준다.
--%>
<c:set var="extraMap" value="${placeDetail.extraInfoMap}" />

<div class="business-form-group">
    <label class="business-form-label">부가정보</label>
    <p class="business-price-hint">
        업장에 해당하는 항목을 골라 입력하세요. 입력한 항목만 저장되고, 업소 상세 화면에 목록으로 표시됩니다.
    </p>

    <%-- id에 라벨을 쓸 수 없어("입/퇴실", "반려동물 관광정보"처럼 공백·슬래시가 들어간다) 순번으로 만든다.
         칩과 입력칸을 같은 순번 규칙으로 그려서 data-target으로 짝지운다. --%>
    <c:forEach var="typeEntry" items="${extraSectionsByType}">
        <div class="venue-extra-group js-extra-group is-hidden" data-place-type="${typeEntry.key}">

            <div class="venue-extra-picker">
                <c:forEach var="section" items="${typeEntry.value}" varStatus="sec">
                    <div class="venue-extra-picker__section">
                        <%-- 관광지만 세부유형(관광지/문화시설/레포츠) 소제목이 붙는다 --%>
                        <c:if test="${not empty section.title}">
                            <p class="venue-extra-picker__title">${section.title}</p>
                        </c:if>
                        <div class="venue-extra-chips">
                            <c:forEach var="opt" items="${section.options}" varStatus="row">
                                <label class="venue-extra-chip" title="${opt.description}">
                                    <input type="checkbox" class="js-extra-chip"
                                           data-target="${param.idPrefix}extra-${typeEntry.key}-${sec.index}-${row.index}" />
                                    <span>${opt.label}</span>
                                </label>
                            </c:forEach>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <p class="venue-extra-empty js-extra-empty">위에서 항목을 고르면 입력칸이 나타납니다.</p>

            <div class="venue-extra-rows">
                <c:forEach var="section" items="${typeEntry.value}" varStatus="sec">
                    <c:forEach var="opt" items="${section.options}" varStatus="row">
                        <c:set var="rowId" value="${param.idPrefix}extra-${typeEntry.key}-${sec.index}-${row.index}" />
                        <div class="venue-extra-row is-hidden" id="${rowId}">
                            <label class="venue-extra-row__label" for="${rowId}-value">${opt.label}</label>
                            <div class="venue-extra-row__field">
                                <input type="hidden" name="extraLabels" value="${opt.label}" disabled />
                                <input class="business-form-input" type="text" id="${rowId}-value" name="extraValues"
                                       value="${fn:escapeXml(extraMap[opt.label])}"
                                       placeholder="예) ${opt.sample}" disabled />
                                <span class="venue-extra-row__desc">${opt.description}</span>
                            </div>
                            <button type="button" class="venue-extra-row__remove js-extra-row-remove"
                                    aria-label="${opt.label} 항목 빼기">×</button>
                        </div>
                    </c:forEach>
                </c:forEach>
            </div>
        </div>
    </c:forEach>
</div>

<%-- 반려동물 동반 정보는 업종과 무관한 공통 항목이라 별도 섹션으로 분리하고, 체크했을 때만 연다.
     (관광지의 [애완동물동반] 항목은 동반 가능 여부만 적는 자리라 업종별 목록에 그대로 둔다) --%>
<div class="business-form-group">
    <label class="venue-extra-toggle">
        <input type="checkbox" id="${param.idPrefix}petToggle" class="js-extra-pet-toggle" />
        <span>반려동물 동반 가능</span>
    </label>
    <p class="business-price-hint">체크하면 반려동물 동반 관련 안내를 추가로 입력할 수 있습니다.</p>

    <div class="venue-extra-group js-extra-pet-group is-hidden">
        <div class="venue-extra-picker">
            <div class="venue-extra-chips">
                <c:forEach var="opt" items="${petExtraOptions}" varStatus="row">
                    <label class="venue-extra-chip" title="${opt.description}">
                        <input type="checkbox" class="js-extra-chip"
                               data-target="${param.idPrefix}extra-pet-${row.index}" />
                        <span>${opt.label}</span>
                    </label>
                </c:forEach>
            </div>
        </div>

        <p class="venue-extra-empty js-extra-empty">위에서 항목을 고르면 입력칸이 나타납니다.</p>

        <div class="venue-extra-rows">
            <c:forEach var="opt" items="${petExtraOptions}" varStatus="row">
                <c:set var="rowId" value="${param.idPrefix}extra-pet-${row.index}" />
                <div class="venue-extra-row is-hidden" id="${rowId}">
                    <label class="venue-extra-row__label" for="${rowId}-value">${opt.label}</label>
                    <div class="venue-extra-row__field">
                        <input type="hidden" name="extraLabels" value="${opt.label}" disabled />
                        <input class="business-form-input" type="text" id="${rowId}-value" name="extraValues"
                               value="${fn:escapeXml(extraMap[opt.label])}"
                               placeholder="예) ${opt.sample}" disabled />
                        <span class="venue-extra-row__desc">${opt.description}</span>
                    </div>
                    <button type="button" class="venue-extra-row__remove js-extra-row-remove"
                            aria-label="${opt.label} 항목 빼기">×</button>
                </div>
            </c:forEach>
        </div>
    </div>
</div>

<%-- 티스토리 태그 입력처럼 콤마/Enter를 누르면 칩으로 바뀐다 (business-venue.js). name="hashtags"인
     hidden input이 실제 제출값(콤마로 합친 문자열)을 들고 있고, 보이는 text input은 입력용일 뿐이다 --%>
<div class="business-form-group">
    <label class="business-form-label" for="${param.idPrefix}hashtagInput">해시태그</label>
    <div class="venue-tag-input">
        <input type="hidden" name="hashtags" value="${param.hashtags}" />
        <input class="venue-tag-input__field" type="text" id="${param.idPrefix}hashtagInput"
              placeholder="#태그입력" />
    </div>
    <p class="business-price-hint">콤마(,)나 Enter를 누르면 태그로 등록됩니다.</p>
</div>