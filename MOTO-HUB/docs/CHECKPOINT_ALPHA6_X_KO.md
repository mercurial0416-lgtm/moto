# MOTO HUB 4.0.0-alpha6-x · 라이딩 기록 상태 필터

기준 소스: `MOTO-HUB-4.0.0-alpha6-w-source.zip` (2026-10-17, ZIP 무결성 검사 통과).

## 변경 사항

- 라이딩 목록에 `전체 / 기록 중 / 완료 / GPX / 복구 / 중단` 상태별 가로 스크롤 필터 추가.
- 각 필터별 건수, 현재 표시 건수/전체 건수, 선택된 상태, 검색 결과 없음 안내 추가.
- 필터 선택 후 세로 스크롤 위치 유지 및 화면 회전 시 선택 필터 복원.
- 상태 판정은 기존 `.done`, `.imported`, `.recovered` 마커를 사용하고 원본 GPS 좌표는 읽거나 변경하지 않음.
- 필터 건수는 목록당 한 번의 상태 판정 순회로 계산.
- 앱 버전 `4.0.0-alpha6-x`, `versionCode 429`로 변경. 직전 Alpha6-W 소스의 실제 `versionCode`는 427(문서에는 428로 표기되어 불일치)이었으므로 이번 체크포인트에서 429로 증가시킴.

## 검증

- 신규 `tests/run-ride-filter-tests.sh`: JVM 단언 34개 및 UI 소스 연결 검사 통과.
- 기존 `tests/run-ride-history-tests.sh`: JVM 단언 20개 및 연결 검사 통과.
- 전체 20개 `tests/run-*-tests.sh` 중 19개 통과(마지막 복구 테스트는 별도 재실행으로 통과), 1개 `run-model-tests.sh`는 `JSON_JAR`가 없어 미실행.
- `tests/test_catalog.py`: 49개 브랜드, 522개 모델 통과.
- `tests/JavaSyntaxCheck.java`: 42개 Java 파일 구문 파싱 통과. Android 의존성 컴파일이 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1 배포본 캐시 부재 및 `services.gradle.org` DNS 연결 실패. Android SDK도 발견되지 않음.
- Android APK 생성 및 실제 단말 실행 **미검증**. Android UI 렌더링/상호작용 자동화 **미실행**.

## 보존 범위

정비·주유·매물·GPS 기록/복구·GPX·Supabase 친구/그룹·카탈로그·백업/복원 데이터 구조는 변경하지 않았음. 친구 위치 공유 권한, GPS 기록 동의, 위치 전송 코드는 변경하지 않았음. 이들 기능 전체의 실기기 회귀 검증을 수행했다는 의미는 아님.

## 단말 확인 필요

라이딩 기록 6종이 각각 있는 상태에서 칩별 건수·목록·빈 상태를 확인하고, 화면을 회전해 필터가 유지되는지 확인. 라이딩을 시작/종료하면 상태가 업데이트되는지, 지도·GPX·복구 버튼이 기존대로 동작하는지 확인.
