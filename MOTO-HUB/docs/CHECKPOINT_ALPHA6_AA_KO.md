# MOTO HUB 4.0.0-alpha6-aa · 라이딩 상태 필터 유지

날짜: 2026-10-17. 기준: 로컬에 존재한 `MOTO-HUB-4.0.0-alpha6-z-source.zip`을 ZIP 무결성 검사 후 압축 해제하여 수정. `versionCode 432`, `versionName 4.0.0-alpha6-aa`.

## 실제 수정
- `MainActivity`: 앱 실행 시 `ride_history_ui/filter`에서 마지막 필터 복원. 회전 저장 상태가 있으면 그 값을 우선 적용.
- `MainActivity`: 필터 버튼 선택 시 정상화된 값을 로컬 SharedPreferences에 즉시 저장하고 기존 목록·스크롤 갱신 경로 사용.
- `RideHistoryFilter`: 저장값과 회전값을 통합 복원하는 순수 JVM 함수 추가. null/미지원 값은 '전체'로 복구.
- `RideHistoryFilterTest`: 재실행·회전·잘못된 값·최초 실행 복원 정책 6개 검사 추가. `run-ride-filter-tests.sh`에 Android 코드 연결 정적 검사 추가.
- GitHub Actions 아티팩트 이름과 앱 버전 갱신. 정비·주유·매물·GPS·GPX·친구/그룹·카탈로그·백업/복원 데이터 스키마·권한·공유 코드 변경 없음.

## 이번 실행의 실제 검증
- `tests/run-ride-filter-tests.sh`: JVM 단언 40개 통과 및 UI 연결 정적 검사 통과.
- `tests/run-ride-sort-tests.sh`: JVM 단언 19개 통과 및 UI 연결 정적 검사 통과.
- `tests/run-*-tests.sh`: 20개 통과 / 0개 실패 / 1개 미실행. 미실행 `run-model-tests.sh`는 `JSON_JAR` 미제공.
- `python3 tests/test_catalog.py`: 49개 브랜드·522개 모델 검사 통과.
- `tests/JavaSyntaxCheck.java` Java 17 구문 파싱: 43개 Java 파일 통과. Android 실제 컴파일 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1 배포본 다운로드 시 `services.gradle.org` DNS 실패. Android SDK 경로/`sdkmanager`도 부재.
- APK 없음. 실제 단말 설치, Android SharedPreferences 동작, GPS·Supabase 연동은 미검증.

## 체크포인트
- 소스 ZIP: `MOTO-HUB-4.0.0-alpha6-aa-source.zip` (완전한 소스 트리, APK 제외).
- 검증 로그: `evidence/alpha6-aa/tests.log`, `catalog.log`, `syntax.log`, `build.log`.
- 기존 소스 ZIP과 비교하여 기존 파일 경로를 제거하지 않고 변경 파일만 갱신.
