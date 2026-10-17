# MOTO HUB 4.0.0-alpha6-u · GPS 권한 승인 후 원터치 시작

기준 소스: `MOTO-HUB-4.0.0-alpha6-t-source.zip`를 실제 압축 해제한 파일. 날짜: 2026-10-17.

## 해결한 편의성 문제

기존에는 사용자가 라이딩 시작 확인창에서 동의하고 위치/알림 권한을 허용해도, `MainActivity.onRequestPermissionsResult`가 무조건 "다시 눌러 주세요"라고 안내했다. 사용자는 시작 버튼과 동의창을 반복해야 했다.

## 변경

- `RideStartConsent` 추가: 사용자 확인창에서 동의한 경우에만 일회성 시작 승인을 보유한다.
- 정확한 위치 권한 승인 후 Android 13+ 알림 권한까지 필요한 경우 순서대로 요청한다. 모든 권한과 GPS 준비 상태가 충족되면 확인창을 다시 띄우지 않고 기록 시작을 요청한다.
- 권한 거절, 라이딩 탭 이탈, GPS/알림 설정 차단, 액티비티 종료에서는 자동 시작하지 않는다. 일회성 승인은 시작 요청 직전에 소모해 중복 시작을 막는다.
- 권한 대화상자 중 화면 회전 시 상태를 복원할 수 있도록 저장 상태에 승인 플래그만 기록한다. GPS 좌표/공유 동의/토큰은 저장하지 않는다.
- 위치 공유 기능의 별도 동의·접근 제어, 정비·주유·매물·GPX·카탈로그·백업/복원 기능은 변경하지 않았다.
- 앱 버전 `versionCode 426`, `versionName 4.0.0-alpha6-u`. CI 아티팩트 및 JVM 검사 추가.

## 실제 실행한 검증

- `tests/run-ride-consent-tests.sh`: 신규 18개 순수 JVM 단언 및 Android UI/콜백 연결 정적 검사 통과.
- `tests/run-ride-readiness-tests.sh`: 기존 준비 상태 25개 단언 및 연결 검사 통과.
- `tests/run-track-recovery-tests.sh`: GPS 복구 20개 단언 및 연결 검사 통과.
- `tests/run-*.sh` 17개 중 `run-model-tests.sh`(JSON_JAR 없음)를 제외한 **16개 스크립트 통과**.
- `python3 tests/test_catalog.py`: 49개 브랜드, 522개 모델 통과.
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패**. Gradle 8.11.1 배포본이 캐시에 없어 `services.gradle.org` 다운로드를 시도했으나 DNS `UnknownHostException`. Android SDK 36도 설치돼 있지 않음. **APK 미생성, Android 컴파일/실기기 테스트 미검증**.

## 후속 확인

Android SDK 36 + Gradle 8.11.1 환경에서 `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` 실행 후, 실기기에서 처음 위치/알림 허용, 부분 위치 허용, 알림 거절, GPS 꺼짐, 화면 회전, 중복 실행 방지와 위치 공유 기본 OFF 확인.
