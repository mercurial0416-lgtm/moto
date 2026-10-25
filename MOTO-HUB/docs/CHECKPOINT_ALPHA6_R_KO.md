# MOTO HUB 4.0.0-alpha6-r · 작성 중인 초안 삭제 UI

기준: 이전에 검증된 Alpha6-Q 전체 소스 ZIP. 2026-10-17.

## 재현된 편의성 문제

홈/차고의 `작성 중인 기록 이어서 입력` 배너에는 이어서 작성 기능만 있었다. 사용자가 작성하지 않을 초안을 만들었거나 초안 복원에 실패한 경우, 배너를 앱 화면에서 제거하는 수단이 없었다.

## 실제 수정

- 기존 배너를 `작성 중인 [유형] 기록` 카드로 교체하고 `이어서 작성`과 `초안 삭제`를 분리.
- 초안 삭제 전에 확인 대화상자 제공. `취소` 시 변경 없음.
- 확인 시 `SharedPreferences("draft")`의 `active` 항목만 제거하고 즉시 화면 갱신. 기존 저장된 바이크/정비/주유/GPS/매물 데이터에는 손대지 않음.
- 바이크·정비·주유/비용·매물·코스·보호장구 초안의 유형을 읽기 쉽게 표시. 버튼의 접근성 설명 및 최소 터치 높이는 기존 공통 버튼 스타일 사용.
- 새 순수 Java 정책 클래스 `DraftBanner` 및 검증 스크립트 추가. GitHub Actions 테스트 단계에 연결.
- 버전: `versionCode 423`, `versionName 4.0.0-alpha6-r`.

## 실행한 검사

- `tests/run-draft-banner-tests.sh`: 정책 9개 + Android UI 소스 연결 검사 통과.
- `tests/run-date-field-tests.sh`: 35개 + UI 연결 검사 통과.
- `tests/run-catalog-search-tests.sh`: 13개 + UI 연결 검사 통과.
- `tests/run-backup-tests.sh`, `run-restore-tests.sh`, `run-offline-tests.sh`, `run-server-settings-tests.sh`, `run-quick-entry-tests.sh`, `run-bike-visual-tests.sh`, `run-photo-request-tests.sh`, `run-photo-title-tests.sh`, `run-catalog-year-tests.sh`, `run-ride-tests.sh`: 전부 통과.
- `tests/test_catalog.py`: 49개 브랜드, 522개 모델 검사 통과.
- `JavaSyntaxCheck`: Android Java 소스 37개 구문 파싱 통과. 실제 Android 컴파일 검사가 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1을 로컬에서 찾지 못해 `services.gradle.org`에 접속을 시도했으나 DNS 해석 실패. Android SDK도 설치되지 않음.

## 검증 한계

APK 미생성, 실제 안드로이드 단말의 터치/접근성/화면 크기 검증 미수행. `org.json` 의존성이 필요한 기존 모델 테스트 미수행. 온라인 서비스 연결 및 Supabase 권한 정책 미검증. 기존 데이터 스키마·위치 공유 로직 변경 없음.
