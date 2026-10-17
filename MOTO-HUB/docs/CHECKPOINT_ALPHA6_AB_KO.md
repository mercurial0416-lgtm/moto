# MOTO HUB 4.0.0-alpha6-ab · 라이딩 기록 즉시 검색

기준: 2026-10-17, 실제 로컬에 존재한 `MOTO-HUB-4.0.0-alpha6-aa-source.zip` 무결성 검사 후 압축 해제하여 수정. `versionCode 433`, `versionName 4.0.0-alpha6-ab`.

## 실제 수정
- 라이딩 기록 화면에 **날짜·시간·상태 즉시 검색** 입력창과 지우기 버튼 추가. 예: `2026-10-17`, `10/17`, `21:40`, `GPX`, `복구`.
- 검색 시 화면 전체를 다시 그리지 않고 기록 목록과 결과 건수만 갱신해 키보드 입력이 끊기지 않도록 함. 검색 결과 없음 안내 추가.
- 기존 상태 필터와 날짜 정렬을 적용한 뒤 검색. 필터·정렬 변경, 화면 회전 중 검색어 보존(앱 완전 종료 후 검색어는 저장하지 않음).
- 기존 `HubScreens.line` 렌더링을 특정 목록 컨테이너에도 사용할 수 있게 확장. 다른 화면은 기존 경로 유지.
- `RideHistorySearch` 순수 JVM 검색 도우미와 26개 단언 테스트 및 UI 정적 연결 검사 추가. 검색은 이미 화면에 표시되는 날짜·상태 문자열만 검사하고 GPS 원본을 수정·전송하지 않음.
- 정비·주유·매물·GPS·GPX·Supabase 친구/그룹·카탈로그·백업/복원 로직과 데이터 스키마는 변경하지 않음. 위치 공유 명시적 동의 및 접근 제어 코드도 그대로 유지.

## 실제 검증
- `tests/run-ride-search-tests.sh`: JVM 26개 단언 및 검색 UI 연결 정적 검사 통과.
- `tests/run-ride-filter-tests.sh`: 40개 단언 및 변경된 UI 연결 검사 통과.
- `tests/run-ride-sort-tests.sh`: 19개 단언 및 UI 연결 검사 통과.
- `tests/run-ride-history-tests.sh`: 20개 단언 및 UI 연결 검사 통과.
- `tests/run-*-tests.sh`: 21개 스크립트 통과, 1개(`run-model-tests.sh`) JSON_JAR 부재로 미실행.
- `python3 tests/test_catalog.py`: 49개 브랜드·522개 모델 검사 통과.
- `JavaSyntaxCheck.java`: 앱 Java 44개 파일 Java 17 구문 파싱 통과. Android 타입 검사·APK 컴파일 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1 다운로드 시 `services.gradle.org` DNS 조회 오류. Android SDK 36 및 `sdkmanager`도 확인되지 않음.
- APK 미생성. 실제 단말의 검색 UI·GPS·Supabase 연결은 미검증.

## 체크포인트
- 완전한 소스: `MOTO-HUB-4.0.0-alpha6-ab-source.zip`.
- 실제 로그: `evidence/alpha6-ab/tests.log`, `evidence/alpha6-ab/syntax.log`, `evidence/alpha6-ab/apk-build.log`.
- 이전 체크포인트의 모든 소스 파일 경로 유지. 이 ZIP은 개발 소스이며 설치용 APK가 아님.
