function toggleWishLocal(buttonElement) {
    // 현재 버튼의 활성화 상태 유무 (기본값 wish-off)
    const isActive = buttonElement.getAttribute('data-active') === 'true';
    const imgElement = buttonElement.querySelector('.wish-icon');
    
    // 프로젝트 Context Path 자동 계산
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/', 2));

    // 클릭할 때마다 상태 반전시키고 이미지 갈아끼우기
    if (isActive) {
        // ON -> OFF 상태로 전환
        imgElement.src = `${contextPath}/images/icons/wish-off.png`;
        buttonElement.setAttribute('data-active', 'false');
    } else {
        // OFF -> ON 상태로 전환
        imgElement.src = `${contextPath}/images/icons/wish-on.png`;
        buttonElement.setAttribute('data-active', 'true');
    }
}

/*  셀렉터블 버튼 클릭 인터랙션 핸들러
    화면 내의 모든 .btn-selectable 요소를 찾아 클릭 시 활성화 상태(is-active) 토글
 */
document.addEventListener("DOMContentLoaded", function () {
    document.addEventListener("click", function (event) {
        const button = event.target.closest(".btn-selectable");
        if (button) {
            button.classList.toggle("is-active");
        }
    });
});