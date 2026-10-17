# MOTO HUB 4.0.0-alpha6-t · 중단된 GPS 기록 안전 복구

기준: 실제 열어 확인한 `MOTO-HUB-4.0.0-alpha6-s-source.zip` (2026-10-17).

## 재현 가능한 문제

- 기존 GPS 서비스는 저장 오류, 위치 권한 해제, GPS 비활성화로 강제 종료돼도 `.done` 완료 표시를 남길 수 있었다. 중단된 기록과 정상 종료된 기록이 UI에서 혼동됐다.
- 비정상 종료로 마지막 GPS 행이 쓰다 만 상태가 되면 `TrackFile.read`는 그 행을 무시했지만, 사용자가 안전하게 파일을 정리하고 복구 상태를 기록하는 버튼은 없었다.

## 실제 수정

1. `RideService`: 사용자 종료(STOP 액션)에만 `.done` 표시. 오류/권한 해제/GPS 중지/비정상 서비스 종료는 미완료 상태로 보존. 앱 화면의 종료 버튼도 STOP 액션을 사용.
2. `TrackRecovery`: 중단된 트랙을 사용자가 명시적으로 선택했을 때만 복구. 유효한 줄바꿈 완료 좌표 전체를 먼저 검증하고, 마지막 미완성 행만 제거. 동일 폴더 임시 파일에 복사 및 디스크 동기화 후 원자적 파일 교체. 원본 유효 행을 만들거나 변경하지 않음. 유효 좌표가 없거나 완료·가져온 기록은 거부.
3. 라이딩 기록 목록: `중단된 기록 · 복구하고 분석하기` / `복구된 라이딩` 구분, 복구 확인창 및 복구 없이 분석 선택 제공. 주행 분석 화면에 복구 기록 경고 표시.
4. `.recovered` 표식을 전체 ZIP 백업/복원/기록 삭제까지 연결. 복구한 경로도 GPX 변환을 백업에 포함.
5. 위치 수집·그룹 공유·Supabase 스키마·인증·기존 정비·주유·매물 기능 변경 없음. 자동 GPS 재시작 및 위치 공유 재시작은 구현하지 않음.
6. `versionCode 425`, `versionName 4.0.0-alpha6-t`. CI 테스트 및 산출물명 갱신.

## 이 환경에서 실제 실행한 검증

- 신규 `tests/run-track-recovery-tests.sh`: 복구 20개 단언 및 Android 코드 연결 정적 검사 통과.
- `tests/run-ride-tests.sh`: GPX/주행 분석 16개 단언 통과.
- `tests/run-backup-tests.sh`: 복구 표시와 GPX가 포함된 ZIP 백업 검사 통과.
- `tests/run-restore-tests.sh`: 복구 표시 포함 ZIP 복원·롤백 및 손상 파일 거부 18개 검사 통과.
- 총 16개 `run-*.sh` 중 **15개 통과**. `run-model-tests.sh`는 `JSON_JAR`가 없어 실행 불가.
- `tests/test_catalog.py`: 49개 브랜드·522개 모델 검증 통과.
- Java 17 문법 파싱: 앱 Java 파일 39개 통과. Android SDK를 사용한 실제 컴파일 검사가 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패** (`services.gradle.org` DNS `UnknownHostException`). Android SDK 경로도 없음. APK 미생성, 실기기 미검증.

## 남은 확인

Android SDK 36 + Gradle 8.11.1 환경에서 `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` 실행. 실기기에서 정상 종료, GPS 중단, 앱 프로세스 종료, 마지막 불완전 행 복구, ZIP 백업→복원, 공유 비활성화 확인. **OS가 프로세스를 종료하는 순간까지의 마지막 GPS 좌표를 보장하지는 않음.**
