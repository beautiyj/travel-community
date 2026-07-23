/**
 * 
 */
window.onload = function() {



	const form = document.getElementById("findIdForm");
	form.addEventListener("submit", (e) => {

		const name = document.getElementById("findIdName")
		const email = document.getElementById("findIdEmail")

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