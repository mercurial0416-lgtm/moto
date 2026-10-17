# MOTO HUB 4.0.0-alpha6-y · 라이딩 기록 날짜 정렬

기준: `MOTO-HUB-4.0.0-alpha6-x-source.zip` 실제 압축 해제 후 수정. 2026-10-17.

## 실제 변경

- 라이딩 목록에 접근성 설명이 있는 `정렬: 최신순/오래된순` 선택 UI 추가. 선택 시 기존 상태 필터를 유지하고 스크롤 위치를 복원.
- GPX를 나중에 가져왔더라도 **가져온 시각이 아닌 첫 번째 유효한 기록 좌표 시각**으로 정렬. 일반 GPS 기록은 파일명의 시작 시각을 사용.
- 시간 정보가 없거나 첫 좌표가 잘못된 기록은 양쪽 정렬에서 맨 아래 표시. 동일 시각은 기존 상대 순서 유지.
- 화면 회전 시 선택한 정렬 기준 복원. 정렬은 로컬 목록에만 적용하고 GPS 좌표/마커/저장 DB/서버 공유를 변경하지 않음.
- `RideHistorySort.java`, `RideHistorySortTest.java`, `run-ride-sort-tests.sh` 추가. `RideHistoryLabel.startTime()`으로 표시 날짜와 정렬 날짜를 일치시킴.
- 버전 `4.0.0-alpha6-y`, `versionCode 430`.

## 실제 검증

- `tests/run-ride-sort-tests.sh`: JVM 단언 13개 및 UI 소스 연결 검사 통과.
- 기존 `run-ride-filter-tests.sh` 34개 단언, `run-ride-history-tests.sh` 20개 단언 통과.
- 전체 `run-*-tests.sh`: **20개 통과, 0개 실패, 1개 미실행**. `run-model-tests.sh`는 JSON_JAR 의존성이 없어 미실행.
- `tests/test_catalog.py`: **49개 브랜드·522개 모델** 통과.
- Java 17 구문 검사: **43개 Java 파일 파싱 성공**. Android 타입 검사/컴파일을 의미하지 않음.
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패**. Gradle 8.11.1 다운로드 DNS (`services.gradle.org`) 실패, SDK 설치 확인되지 않음. APK 미생성, 실기기·UI 상호작용 미검증.
- 세부 로그: `evidence/alpha6-y/`.

## 보존/제한

정비·주유·매물·GPS·GPX·Supabase 친구/그룹·카탈로그·백업/복원 소스의 데이터 모델 및 공유 제어 로직은 수정하지 않았음. 전체 기능의 Android 실기기 회귀 검증을 의미하지 않음. 기록 목록 정렬은 앱 프로세스 재시작 시 최신순으로 초기화되며 화면 회전 중에는 유지됨.

## 단말 검증 대기

라이딩 탭에서 최신순/오래된순을 바꾸고, 실제 주행 시각이 오래된 GPX를 오늘 가져온 뒤 실제 주행 시각으로 정렬되는지 확인. 회전·상태 필터·스크롤·GPS 기록/종료도 확인.
