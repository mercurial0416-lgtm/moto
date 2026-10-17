# MOTO HUB 4.0.0-alpha6-w · 라이딩 목록 날짜/시각 수정

- 기준: 실제 접근·무결성 검사한 `MOTO-HUB-4.0.0-alpha6-v-source.zip`.
- versionCode 428 / versionName 4.0.0-alpha6-w.

## 재현된 문제

라이딩 목록은 `파일명 앞의 타임스탬프`로 날짜만 표시했음. 같은 날 여러 기록이 구분되지 않으며, GPX 가져오기는 `가져온 시각`을 파일명에 저장하므로 과거 라이딩도 가져온 날짜로 잘못 표시됨.

## 수정

- 라이딩 목록에 `yyyy.MM.dd (요일) HH:mm` 형식으로 시작 시각 표시.
- 일반 GPS 기록은 기존 파일명의 시작 시각을 사용.
- GPX 가져온 기록(`.imported` 마커)은 첫 번째 개행으로 확정된 GPS 좌표의 기록 시각을 사용. 파일의 첫 행만 최대 256바이트 읽으며 잘못된 좌표·시간·미완성 행은 `날짜 정보 없음`으로 표시.
- 시스템 시간대 사용. 원본 트랙·좌표·GPX·DB·백업/복원·공유 권한 로직은 수정하지 않음.

## 이번 실행에서 확인한 검증

- `tests/run-ride-history-tests.sh`: 순수 JVM 단언 20개 및 UI 소스 연결 검사 통과.
- 기존 `tests/run-*-tests.sh`: 총 19개 중 18개 통과, 1개(`run-model-tests.sh`)는 `JSON_JAR` 미설정으로 실행 불가.
- `tests/test_catalog.py`: 49개 브랜드, 522개 모델 검사 통과.
- `tests/JavaSyntaxCheck.java`: 41개 Java 소스 Java 17 구문 파싱 통과. Android 타입 컴파일은 아님.
- `./gradlew :app:assembleDebug --offline --no-daemon`: 실패. Gradle 8.11.1 배포본 캐시 부재로 `services.gradle.org` DNS UnknownHostException. Android SDK 경로도 확인되지 않음.
- Android APK **미생성**. 실제 기기 실행 **미검증**. Android UI 자동화 검증 **미실행**.

## 기기 검증 시나리오

2024년 주행 GPX를 2026년에 가져와 라이딩 목록에 `2024년 기록 시작 시각`이 표시되는지 확인. 같은 날짜에 여러 GPS 기록을 만들었을 때 각각의 시각이 구분되는지 확인. GPX 상세·복구·공유 기본 OFF 동작을 회귀 확인.
