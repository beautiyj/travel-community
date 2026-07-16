## PR Type

- [x] 기능 개발
- [x] UI
- [ ] 버그 수정
- [ ] 리팩토링
- [ ] 기타:

## 요약

- 메인 화면(`index.jsp`)에서 여행지 카드 배치를 담당할 핵심 컴포넌트 3종(`cardComponent`, `tagButton`, `wishButton`)의 동적 데이터 바인딩 및 상호 결합 테스트 완료
- 정적 리소스(이미지/아이콘) 탐색 경로 최적화 및 태그 컴포넌트의 유동적 텍스트 너비 대응을 위한 CSS 규격 수정 완료

## 상세 내용

### 1. 테스트 검증 환경 구축 (`index.jsp`)
- `cardComponent.jsp`, `tagButton.jsp`, `wishButton.jsp` 3가지 컴포넌트를 단독 및 결합 형태로 호출하여 정상 작동 검증
- 조건별 가상 데이터 주입을 통해 다음 3가지 핵심 분기점 검수 완료
  - **Case 1 (숙박):** `place_type="stay"` 조건 시 파란색 배지 매핑 및 찜 비활성화(`isBookmarked=false`) 상태 렌더링 확인
  - **Case 2 (관광지):** `place_type="tour"` 조건 시 연녹색 배지 매핑 및 찜 활성화(`isBookmarked=true`) 상태 렌더링 확인
  - **Case 3 (기본/직접 입력):** `place_type` 공백 처리 및 `text` 파라미터 주입 시 회색 테마(`.type-default`) 적용 및 사용자 지정 텍스트(예: "서울 성수동 인기 카페" 등 유동적 너비) 정상 출력 확인

### 2. fix: tagButton.css (기본 태그 너비 유동성 확보)
- 기존 고정 너비(`width: 72px`)로 인해 발생하던 긴 커스텀 텍스트(Case 5, 6) 잘림 및 레이아웃 붕괴 현상 수정
- `width: max-content` 또는 최소 너비(`min-width: 72px`) 설정을 통해 텍스트 길이에 따라 배경 칩 자켓 너비가 부드럽게 늘어나도록 가변성 확보

### 3. chore: 이미지 아이콘 리소스 경로 정비
- 브라우저 깨짐 현상 및 404 에러 방지를 위해 기존 불명확했던 아이콘 경로 체계를 스프링 정적 자원 표준 경로로 일괄 이식
- 물리 파일 위치를 내부 `resources` 폴더에서 웹 앱 순정 스태틱 경로인 **`${pageContext.request.contextPath}/images/icons/`** 폴더 내부로 이동 완료 (`wish-on.png`, `wish-off.png` 등 정상 출력 확인)