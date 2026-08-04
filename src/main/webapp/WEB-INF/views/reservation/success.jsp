<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>결제 완료</title>
<link rel="stylesheet" href="/css/common.css">
<link rel="stylesheet" href="/css/reservation/reservation.css">
<link rel="stylesheet" href="/css/components/smallButton.css">
<%-- 예약취소 모달(cancelModal)이 이 스타일로 숨겨져 있다가 openModal() 호출 시에만 보임 --%>
<link rel="stylesheet" href="/css/components/confirmModal.css">
</head>
<body>
	<jsp:include page="/WEB-INF/views/common/header.jsp" />
	<div class="result-card">
		<div class="result-icon ok">&#10003;</div>
		<h2>결제가 완료되었습니다</h2>

		<div class="result-info">
			<div>
				<span>예약번호</span><span>${payment.reservationId}</span>
			</div>
			<div>
				<span>주문번호</span><span>${payment.orderId}</span>
			</div>
			<div>
				<span>결제수단</span><span>${method}</span>
			</div>
			<div>
				<span>결제금액</span> <span><fmt:formatNumber
						value="${payment.amount}" pattern="#,###" />원</span>
			</div>
		</div>

		<div class="result-actions">
			<jsp:include page="/WEB-INF/views/common/smallButton.jsp">
				<jsp:param name="text" value="내 예약 확인하기" />
				<jsp:param name="width" value="100%" />
				<jsp:param name="onclick"
					value="location.href='/mypage/reservations'" />
			</jsp:include>
			<%-- 예약 취소 요청은 마이페이지 예약 내역에서 처리한다(구현 예정).
             아래 트리거 버튼만 제거하고, 모달·JS·API는 그대로 남겨 마이페이지에서 버튼만 추가하면 바로 연결되게 둔다. --%>
		</div>
	</div>

	<%-- ───────── 예약 취소 요청 모달 (공용 confirmModal 스타일·openModal/closeModal 재사용) ─────────
     공용 confirmModal.jsp는 form 전송형이라, AJAX(fetch)로 보내는 취소 흐름에는
     스타일/열고닫기 인프라만 재사용하고 마크업은 여기 직접 둔다. select 항목은 추후 추가. --%>
	<div id="cancelModal" class="modal-overlay" data-modal role="dialog"
		aria-modal="true" aria-labelledby="cancelModal-title">
		<div class="modal">
			<h2 class="modal-title" id="cancelModal-title">예약 취소 요청</h2>
			<p class="modal-message">취소 사유를 선택해 주세요. 관리자 확인 후 환불됩니다.</p>

			<div class="modal-field" style="margin-bottom: 24px;">
				<label for="cancelReason"
					style="display: block; margin-bottom: 8px; font-weight: 600; font-size: var(--text-sm);">취소
					사유</label> <select id="cancelReason"
					style="width: 100%; padding: 10px; box-sizing: border-box; border: 1px solid var(--border); border-radius: var(--radius-md);">
					<option value="">사유를 선택하세요</option>
					<option value="단순 변심">단순 변심</option>
					<option value="일정 변경">일정 변경</option>
					<option value="중복 예약">중복 예약</option>
					<option value="기타">기타</option>
				</select>
				<%-- "기타" 선택 시에만 노출되는 직접입력. cancel_reason 컬럼 길이(100자)에 맞춰 maxlength 제한 --%>
				<textarea id="cancelReasonEtc" maxlength="100" placeholder="사유를 직접 입력해 주세요"
					style="display: none; width: 100%; margin-top: 8px; padding: 10px; box-sizing: border-box; border: 1px solid var(--border); border-radius: var(--radius-md); resize: vertical; min-height: 70px; font-family: inherit;"></textarea>
			</div>

			<div class="modal-buttons">
				<button type="button" class="btn btn-outline" data-modal-close>닫기</button>
				<button type="button" class="btn btn-danger" id="cancelConfirmBtn">취소 요청</button>
			</div>
		</div>
	</div>

	<%-- 서버값 주입 후 외부 JS 로드 --%>
	<script>
		var paymentId = "${payment.paymentId}";
		var reservationId = "${payment.reservationId}";

		// 결제가 완료됐으므로 예약폼 임시 입력값(sessionStorage) 제거 — 다음 예약은 빈 폼으로
		sessionStorage.removeItem('reservationDraft');
	</script>
	<%-- 공용 모달 열고닫기(openModal/closeModal, 오버레이·ESC 닫힘) 재사용 --%>
	<script src="/js/common.js"></script>
	<script src="/js/reservation/payment-success.js"></script>
</body>
</html>
