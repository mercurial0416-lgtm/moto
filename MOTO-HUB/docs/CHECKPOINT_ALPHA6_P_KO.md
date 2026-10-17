# MOTO HUB 4.0.0-alpha6-p · 연식 선택 UX 개선

## 이번 체크포인트 (2026-10-17)

- **UI:** 브랜드 → 모델 → 연식 선택창에 기존 폼 연식을 미리 선택하고 해당 위치로 스크롤. `2024`가 입력돼 있으면 2024년식이 선택된 상태로 열림.
- **편의성:** 연식 직접 입력 버튼을 추가해 긴 목록을 스크롤하지 않아도 숫자만 입력해서 선택 가능. 1900~현재 연도+2 범위 검사, 잘못 입력하면 입력창을 유지하며 오류 표시.
- **버그 수정:** 기존 선택창은 1990년 이전 차량을 고를 수 없었고, 2028년식(2026년 기준)은 저장 허용 범위인데 선택 목록에는 없었음. 연식 목록과 저장 범위를 일치시킴.
- **데이터 안전:** 기존 바이크/매물 JSON 구조 변경 없음. 정비·주유·매물·GPS·GPX·친구/그룹·카탈로그·백업/복원 및 위치 공유 정책 변경 없음.
- **테스트/자동화:** `CatalogYearChoices` 순수 Java 헬퍼와 18개 단위 검사 및 Android 화면 소스 연결 검사 추가. GitHub Actions 테스트 단계 연결. 앱 버전 4.0.0-alpha6-p / versionCode 421.

## 실제 검증

- `tests/run-catalog-year-tests.sh`: 18개 JVM 검사 및 UI 소스 연결 검사 통과.
- `tests/run-catalog-search-tests.sh`: 13개 JVM 검사 및 UI 연결 검사 통과.
- `tests/run-backup-tests.sh`, `run-bike-visual-tests.sh`, `run-offline-tests.sh`, `run-photo-request-tests.sh`, `run-photo-title-tests.sh`, `run-quick-entry-tests.sh`, `run-restore-tests.sh`, `run-ride-tests.sh`, `run-server-settings-tests.sh`: 통과.
- `tests/test_catalog.py`: 49개 브랜드, 522개 모델, 데이터 무결성 검사 통과.
- `JavaSyntaxCheck`: Android Java 소스 35개 Java 17 문법 파싱 통과. Android 컴파일 검증은 아님.
- `tests/run-model-tests.sh`: org.json JAR 및 JSON_JAR 환경변수 없어 미실행.
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1 다운로드 시 services.gradle.org DNS 오류. Android SDK 및 Gradle 설치 확인되지 않음. **APK 미생성 / 단말 테스트 미실행.**

## 다음 확인 사항

Android SDK 및 Gradle을 사용할 수 있는 CI에서 APK 빌드 후 실제 단말에서 연식 선택, 키보드, 다이얼로그 중첩/취소, 기존 폼 데이터 유지 여부를 확인해야 함. 사진 후보는 실제 연식의 공식 인증 이미지가 아님.
