# MOTO HUB 4.0.0-alpha6-q · 선택 날짜 초기화 버그 수정

기준: Alpha6-P 전체 소스 ZIP에서 이어서 수정. 2026-10-17.

## 재현된 문제

`MainActivity.helpers()`가 날짜 입력 필드를 `setFocusable(false)`로 만들고 탭하면 날짜 선택기만 열도록 설정했으나, 선택한 날짜를 다시 **미설정**으로 돌릴 방법이 없었다. 보험 만료일, 판매일, 다음 정비일, 구매일 등 선택 입력 항목에서 특히 문제가 되었다. 날짜를 한번 선택하면 기존 값 삭제가 불가능했다.

## 수정 내용

- 날짜 칩 UI에 선택 날짜를 비우는 `선택 안 함` 칩 추가. `@clear`는 화면 내부에서만 사용하는 신호이며 저장되는 값은 빈 문자열이다.
- 선택한 날짜가 없는 경우 `선택 안 함` 칩이 활성 상태로 표시되도록 처리.
- 필수 거래 날짜(정비·주유/비용의 `date`)에는 초기화 칩을 표시하지 않음. 기존 필수 날짜 검증 유지.
- 코스·보호장구의 선택 날짜도 초기화 가능.
- 날짜 입력 필드 자체를 탭하면 기존처럼 달력 열기 유지.
- `DateFieldChoices` 순수 Java 정책 클래스와 단위 검사 35개, UI 연결 검사 추가.
- GitHub Actions 빠른 JVM 테스트 단계에 날짜 검사를 추가.
- 앱 버전 4.0.0-alpha6-q / versionCode 422로 갱신.

기존 DB 스키마, 정비·주유·매물·GPS·GPX·친구/그룹·카탈로그·사진·백업/복원 및 공유 정책 변경 없음.

## 실제 실행한 검사

- `tests/run-date-field-tests.sh`: **35개 정책 검사 통과**, UI 소스 연결 검사 통과.
- `tests/run-catalog-year-tests.sh`: **18개 통과**.
- `tests/run-catalog-search-tests.sh`: **13개 통과**.
- `tests/run-backup-tests.sh`, `run-bike-visual-tests.sh`, `run-offline-tests.sh`, `run-photo-request-tests.sh`, `run-photo-title-tests.sh`, `run-quick-entry-tests.sh`, `run-restore-tests.sh`, `run-ride-tests.sh`, `run-server-settings-tests.sh`: **전부 통과**.
- `tests/test_catalog.py`: **49개 브랜드·522개 모델 데이터 검사 통과**.
- `JavaSyntaxCheck`: **Android Java 소스 36개 Java 17 구문 파싱 통과**. 구문 파싱은 실제 Android 컴파일과 다름.
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패**. Gradle 8.11.1을 다운로드하려 했으나 `services.gradle.org` DNS 해석 실패. 로컬 Android SDK/Gradle도 확인되지 않음.

## 검증 범위 제한

APK 생성 및 Android 실기기 실행은 하지 못함. 날짜 칩의 실제 터치 동작, 접근성 및 화면 크기별 표시 상태는 단말 검증 필요. 기존 모델 테스트는 별도 `org.json` 의존성이 필요하므로 이번 실행에서는 수행하지 않음.
