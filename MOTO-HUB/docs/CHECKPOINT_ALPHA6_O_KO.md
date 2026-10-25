# MOTO HUB 4.0.0-alpha6-o · 모델 검색 편의성 개선

## 변경

- **UI:** 브랜드 → 모델 선택 검색에서 한글 별칭을 검색해도 실제 모델명을 결과로 표시. 검색 결과 개수 및 결과 없음 안내 추가.
- **편의성:** 검색창의 직접 입력 버튼을 빈 내용으로 누르면 대화상자를 닫지 않고 오류 표시. 카탈로그에 모델이 없는 브랜드에서 가짜 모델 `직접 입력`을 선택해 저장하는 문제 차단.
- **데이터:** `CatalogSearch` 순수 Java 검색 헬퍼 추가, `MotorcycleCatalog.modelAliases`로 오프라인 별칭을 UI에 연결. 브랜드/모델/연식 데이터 자체는 변경하지 않음.
- **유지:** 기존 정비·주유·매물·GPS·GPX·Supabase 친구/그룹·사진·백업/복원 기능 변경 없음. 실시간 위치 공유 정책 변경 없음.
- **빌드 자동화:** GitHub Actions에 신규 검색 테스트를 추가하고 Alpha6-O 이름으로 소스 및 APK 산출물 저장하도록 수정.

## 실제 검증 (2026-10-17)

- `tests/run-catalog-search-tests.sh`: 13개 JVM 검사 + UI 소스 연결/카탈로그 별칭 검사 통과.
- `tests/test_catalog.py`: 49개 브랜드, 522개 모델 통과.
- `tests/run-backup-tests.sh`, `run-bike-visual-tests.sh`, `run-offline-tests.sh`, `run-photo-request-tests.sh`, `run-photo-title-tests.sh`, `run-quick-entry-tests.sh`, `run-restore-tests.sh`, `run-ride-tests.sh`, `run-server-settings-tests.sh`: 모두 통과.
- `tests/run-model-tests.sh`: 실행되지 않음. 필요한 `JSON_JAR` 환경변수 및 `org.json` JAR 없음.
- `JavaSyntaxCheck`: Android 소스 34개 파일 Java 17 구문 검사 통과. **Android 컴파일 성공을 의미하지 않음.**
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1 다운로드 중 `services.gradle.org` DNS 조회 실패. Android SDK도 설치되어 있지 않음. **APK 없음, 실기기 테스트 없음.**

## 한계 및 다음 우선순위

- 온라인 NHTSA 모델 검색은 미국 등록 데이터 기준이며 국내 전 차종/연식 완전성을 보장하지 않음.
- 모델 사진의 연식은 Wikimedia 파일명 기준 검색 후보이며 제조사 공식 확인 이미지가 아님.
- Android SDK/Gradle 접근 가능한 CI에서 APK 빌드 및 실제 단말에서 검색창 동작·한글 IME를 검증해야 함.
