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

/* 셀렉터블 컴포넌트 인터랙션 핸들러 */
document.addEventListener("DOMContentLoaded", function () {
    
    document.addEventListener("click", function (event) {
        
        // 셀렉터블 버튼 감지 (.btn-selectable)
        const button = event.target.closest(".btn-selectable");
        if (button) {
            button.classList.toggle("is-active");
        }

        // 셀렉터블 역할 카드 감지 ([class^='sel-card-col-'])
        const roleCard = event.target.closest("[class^='sel-card-col-']");
        if (roleCard) {
            const groupName = roleCard.getAttribute("data-card-group");
            
            // 동일한 단일선택 그룹에 묶여 있는 카드들을 전부 불러와 초기화 (라디오 버튼 스위칭 메커니즘)
            const siblingCards = document.querySelectorAll(`[data-card-group="${groupName}"]`);
            siblingCards.forEach(card => {
                card.classList.remove("active");
            });

            // 클릭한 카드만 활성화
            roleCard.classList.add("active");

            // 비즈니스 로직 연동 - 회원가입 폼의 hidden input(id="memberRole") 값 동적 변경
            // BUSINESS 설정된 셀렉터블 카드 클릭 후 회원가입 -> 서버로 전송될 hidden input의 value=BUSINESS로 변경 역할
            const selectedRoleId = roleCard.getAttribute("data-card-id"); // ROLE_USER 또는 ROLE_BUSINESS 등
            const roleHiddenInput = document.getElementById("memberRole");
            if (roleHiddenInput) {
                roleHiddenInput.value = selectedRoleId;
            }
        }
    });
});