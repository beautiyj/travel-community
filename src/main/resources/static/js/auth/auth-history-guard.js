(() => {
	"use strict";

	// 로그인 후 뒤로가기로 인증 폼이 다시 노출되는 문제를 막기 위한 페이지 수명주기 상태다.
	// 한 번의 복원에서 세션 확인은 하나만 실행하고, 페이지가 사라지면 진행 중 요청을 취소한다.
	let restoreCheckStarted = false;
	let sessionStatusRequest = null;

	function isBackForwardNavigation() {
		const navigationEntries = window.performance.getEntriesByType("navigation");
		return navigationEntries.length > 0 && navigationEntries[0].type === "back_forward";
	}

	// 브라우저가 인증 화면을 뒤로가기 기록에서 복원한 경우에만 세션을 다시 확인한다.
	function isBackForwardRestore(event) {
		return event.persisted || isBackForwardNavigation();
	}

	function hidePageUntilSessionCheck() {
		// 이전 폼이 잠깐 보이거나 보조기기가 읽는 것을 막고 세션 확인 중임을 표시한다.
		document.body.hidden = true;
		document.body.setAttribute("aria-busy", "true");
	}

	function showPageAfterSessionCheck() {
		document.body.hidden = false;
		document.body.removeAttribute("aria-busy");
	}

	async function checkSessionAfterRestore() {
		// JSP가 현재 컨텍스트 경로를 반영한 세션 상태 URL을 body의 data 속성으로 전달한다.
		const sessionStatusUrl = document.body.dataset.sessionStatusUrl;
		if (!sessionStatusUrl) {
			window.location.reload();
			return;
		}

		sessionStatusRequest = new AbortController();

		try {
			const response = await fetch(sessionStatusUrl, {
				method: "GET",
				cache: "no-store",
				credentials: "same-origin",
				headers: {
					Accept: "application/json"
				},
				signal: sessionStatusRequest.signal
			});

			if (!response.ok) {
				throw new Error("세션 상태 확인 요청에 실패했습니다.");
			}

			const result = await response.json();
			if (result.authenticated === true) {
				// 현재 인증 URL을 다시 요청해 서버가 세션 역할별 목적지를 결정하게 한다.
				window.location.replace(window.location.href);
				return;
			}

			if (result.authenticated !== false) {
				throw new Error("세션 상태 응답 형식이 올바르지 않습니다.");
			}

			// 비로그인 상태가 확인된 경우에만 인증 폼을 다시 표시한다.
			showPageAfterSessionCheck();
		} catch (error) {
			if (error.name === "AbortError") {
				return;
			}

			// 확인 실패 시 서버의 일반 GET 접근 제어를 다시 거치며, reload에서는 이 검사를 반복하지 않는다.
			window.location.reload();
		}
	}

	// BFCache가 아닌 history 복원도 첫 화면을 그리기 전에 폼을 숨긴다.
	if (isBackForwardNavigation()) {
		hidePageUntilSessionCheck();
	}

	window.addEventListener("pageshow", (event) => {
		// pageshow는 일반 진입에도 발생하므로 뒤로/앞으로 복원인 경우만 서버 상태를 재확인한다.
		if (!isBackForwardRestore(event) || restoreCheckStarted) {
			return;
		}

		hidePageUntilSessionCheck();
		restoreCheckStarted = true;
		void checkSessionAfterRestore();
	});

	window.addEventListener("pagehide", () => {
		// BFCache가 인증 폼을 노출한 상태로 저장하지 않도록 페이지를 먼저 숨긴다.
		hidePageUntilSessionCheck();

		if (sessionStatusRequest) {
			sessionStatusRequest.abort();
			sessionStatusRequest = null;
		}

		// 같은 문서가 다시 복원될 때 새 세션 상태를 검사할 수 있도록 초기화한다.
		restoreCheckStarted = false;
	});
})();
