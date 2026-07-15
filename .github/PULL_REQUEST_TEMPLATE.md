## PR Type

- [x] 기능 개발
- [x] UI
- [ ] 버그 수정
- [x] 리팩토링
- [ ] 기타:

## 요약

- 선택 여부에 따라 온오프 스타일 스위칭이 가능한 선택형 버튼 컴포넌트(`selectableButton.jsp`) 설계 및 구현 완료
- `common.js` 내에 동적 엘리먼트 감지 및 디바이스 선택 방식에 대응하는 클릭 인터랙션 핸들러 구현 완료
- 외부 스타일 주입(너비 설정 등) 및 2종 컬러 테마(Primary / Danger) 완벽 지원

## 상세 내용

### 1. feat: selectableButton.jsp & selectableButton.css (선택형 버튼 컴포넌트)
- **2종 컬러 테마 스위칭 지원:** 기본 계통인 `theme-primary`와 댄저 계통인 `theme-danger` 컬러셋을 파라미터(`param.theme`)로 선택 적용할 수 있도록 설계
- **서버-클라이언트 상태 동기화:** 최초 로딩 시 서버 단에서 활성화 여부(`param.isActive`)를 판별하여 클래스(`is-active`)를 미리 구워내도록 구현
- **유연한 너비 커스텀 제어:** 부모 컴포넌트에서 구체적인 너비값(`param.width`)을 주입할 경우 `fn:concat` 함수를 통해 인라인 스타일에 안전하게 동적 바인딩되도록 구성

### 2. feat: common.js 클릭 핸들러 연동
- **모던 이벤트 위임(Event Delegation) 패턴 적용:** 개별 버튼마다 리스너를 중복 등록하지 않고 최상단 `document`에서 클릭 이벤트를 감시하여 동적으로 추가되는 버튼까지 유연하게 대응
- **자식 요소 클릭 예외 방어:** `event.target.closest(".btn-selectable")` 구조를 적용하여 버튼 내부의 텍스트(`<span>`) 영역을 클릭하더라도 예외 없이 상위 버튼 전체의 `is-active` 클래스가 실시간 토글되도록 인터랙션 로직 구현