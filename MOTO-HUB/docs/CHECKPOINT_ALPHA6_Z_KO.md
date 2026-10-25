# MOTO HUB 4.0.0-alpha6-z · 라이딩 기록 정렬 설정 유지

기준: `MOTO-HUB-4.0.0-alpha6-y-source.zip`을 실제 압축 해제해 수정. 날짜: 2026-10-17.

## 변경

- 라이딩 목록에서 최신순/오래된순을 고르면 앱 전용 로컬 SharedPreferences(`ride_history_ui`, `sort`)에 저장. 앱을 완전히 종료하고 다시 실행해도 선택 유지.
- 화면 회전으로 복원한 상태가 있으면 저장된 로컬 설정보다 우선 적용. 저장값이 없거나 유효하지 않으면 안전하게 최신순 사용.
- 정렬·필터·스크롤 기존 UI 유지. 정렬 설정은 바이크 기록 JSON이나 클라우드에 기록하지 않음. GPS, GPX, Supabase 위치 공유·접근 제어 코드 수정 없음.
- GitHub Actions의 소스/APK 아티팩트 이름을 alpha6-z로 갱신하고 정렬/필터 회귀 테스트를 빌드 전에 포함.
- 버전: `4.0.0-alpha6-z`, `versionCode 431`.

## 실행한 검증

- `tests/run-ride-sort-tests.sh`: **19개 JVM 단언 통과**(저장값/화면 회전값 선택·손상된 값 기본값 포함), Android 연결 소스 정적 검사 통과. 실제 Android SharedPreferences 읽기/쓰기 실행은 미검증.
- `tests/run-ride-filter-tests.sh`: 34개 단언 통과.
- `tests/run-ride-history-tests.sh`: 20개 단언 통과.
- 전체 `tests/run-*-tests.sh`: **20개 통과 / 0개 실패 / 1개 미실행**. 미실행 `run-model-tests.sh`: `JSON_JAR` 환경변수 및 JSON 라이브러리 미제공.
- `python3 tests/test_catalog.py`: **49개 브랜드, 522개 모델** 검사 통과.
- `tests/JavaSyntaxCheck.java`를 통한 Java 17 구문 파싱: **43개 파일 성공**. Android 의존성에 대한 실제 컴파일 검사는 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패**. Gradle 8.11.1 배포본 다운로드 시 `services.gradle.org` DNS 실패. 현재 환경에서 Android SDK도 확인되지 않음.
- APK 없음. 실제 Android 단말 설치/동작, GPS/친구/그룹 연동 테스트 미실행.

검증 로그: `evidence/alpha6-z/`. 이번 수정으로 기존 기능의 완전한 단말 회귀 검증을 주장하지 않음.
