# MOTO HUB 4.0.0-alpha6-v · 홈 GPS 시작/종료 동작 수정

기준: `MOTO-HUB-4.0.0-alpha6-u-source.zip`를 실제 압축 해제한 소스.

## 재현 가능한 문제와 수정

1. 홈 화면 `라이딩 시작`은 `RideUi.requestStart()`만 호출했지만, 실제 시작 함수는 `tab == "라이딩"`일 때만 진행했다. 따라서 홈에서 동의 후에도 기록이 시작되지 않았다. **수정:** 홈 버튼 클릭 시 먼저 라이딩 탭으로 이동하고, 기존 GPS 기록 동의/권한 절차를 실행한다.
2. 홈 화면 `라이딩 종료`는 `stopService()`를 직접 호출했다. 서비스의 `onDestroy()`는 비정상 중단으로 `cleanup(false)`를 수행하여 `.done` 완료 마커를 남기지 않는다. **수정:** 라이딩 탭과 동일한 `RideService.STOP` 액션을 전달하여 `stopWith(...,true)`의 정상 종료 경로를 이용한다.

서비스의 GPS 좌표 수집, 파일 구조, 별도 친구/그룹 공유 동의 및 접근 제어는 변경하지 않았다. `versionCode=427`, `versionName=4.0.0-alpha6-v`.

## 검증 및 제한

- `tests/run-home-ride-tests.sh`: 홈/라이딩 화면과 서비스의 종료·동의 연결을 검증하는 **소스 정적 검사 11개 통과**. Android 실제 실행 테스트를 대체하지 않는다.
- `tests/run-ride-consent-tests.sh`: 순수 JVM 동의 정책 18개 통과 및 UI 연결 정적 검사 통과.
- `tests/run-ride-readiness-tests.sh`: 순수 JVM 준비 상태 25개 통과 및 UI 연결 정적 검사 통과.
- `tests/run-ride-tests.sh`: 라이딩·GPX 16개 단언 통과.
- `tests/run-track-recovery-tests.sh`: GPS 복구 20개 단언 및 연결 정적 검사 통과.
- `tests/JavaSyntaxCheck.java`: Java 17 소스 구문 검사 40개 파일 통과. 이는 Android 타입/링크 컴파일이 아니다.
- `tests/test_catalog.py`: 49개 브랜드, 522개 모델 데이터 검사 통과.
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패**. Gradle 8.11.1이 로컬 캐시에 없어 배포본 다운로드 시도 중 `services.gradle.org` DNS `UnknownHostException`. Android SDK도 설치되지 않아 APK 미생성.
- 실제 안드로이드 컴파일/실기기 시작·종료와 `.done` 마커 생성은 아직 미검증.

## 실기기 확인 시나리오

홈 → 라이딩 시작 → 동의 → 위치/알림 허용 → GPS 기록 → 홈 → 종료 → 저장된 라이딩에서 `주행 완료`로 표시되고 GPX 내보내기 가능한지 확인. 취소·권한 거절·GPS 꺼짐 상태에서는 기록이 시작되지 않아야 하며 그룹 공유는 기본 OFF여야 한다.
