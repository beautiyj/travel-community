/**
 * 아이디 찾기 폼의 제출 직전에 이름과 이메일 형식을 검사한다.
 * 이 검사는 잘못된 입력을 빠르게 안내하기 위한 화면 검증일 뿐이며,
 * 실제 회원 조회와 입력값 검증은 POST 요청을 받는 서버가 다시 수행해야 한다.
 */
window.onload = function() {



	const form = document.getElementById("findIdForm");
	form.addEventListener("submit", (e) => {

		const name = document.getElementById("findIdName")
		const email = document.getElementById("findIdEmail")

		// 검증에 실패한 첫 입력란에서 제출을 멈추고 사용자가 바로 수정할 수 있게 포커스를 옮긴다.
		if (name.value === "" || name.value.length > 20 || name.value.length < 2 ||  /\s/.test(name.value)) {
			e.preventDefault();
			alert('이름을 확인하세요.');
			name.focus();
			return false;
		}
		if (email.value === "" || /\s/.test(email.value) || !email.validity.valid ) {
			e.preventDefault();
			alert('이메일을 확인하세요.');
			email.focus();
			return false;
		}
	});

}
