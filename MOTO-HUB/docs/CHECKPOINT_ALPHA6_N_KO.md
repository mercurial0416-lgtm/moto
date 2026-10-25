# MOTO HUB 4.0.0-alpha6-n · 모델 사진 오인식 방지

## 변경 (UI 정확성 우선)

- 기존 Wikimedia 사진 후보 필터는 `String.contains()`로 모델 코드를 찾았기 때문에 R3→R30, Duke 390→3900, Ninja 400→400R 같은 다른 모델을 같은 차종으로 오인할 수 있었습니다.
- `PhotoTitleMatcher`를 추가해 제조사·모델 토큰을 단어 단위로 검증합니다. 하이픈/공백으로 나뉜 모델명과 붙여 쓴 모델 코드(CBR500R 등)는 지원하되 숫자 코드의 부분 문자열은 허용하지 않습니다.
- 연식도 파일명에서 독립 토큰으로 확인합니다. 파일명에 연도가 있어도 **해당 연식의 실차임을 보증하지는 않으며** UI의 '연식 검색 후보' 표기는 유지합니다.
- `BikeVisual`의 슈퍼커브 110 전용 일러스트는 모델명에 110이 명시된 경우만 표시합니다. C125·50·연식/배기량 미상은 중립 플레이스홀더로 표시합니다.
- 기존 정비/주유/GPS/GPX/친구/그룹/카탈로그/백업 복원 기능은 변경하지 않았습니다.

## 검증

- `tests/run-photo-title-tests.sh`: 18개 사진 파일명 필터 검사 통과.
- `tests/run-bike-visual-tests.sh`: 110 전용 일러스트 및 다른 모델 제외 검사 통과.
- `tests/run-photo-request-tests.sh`: 기존 비동기 사진 조회 경합 10개 검사 통과.
- `tests/run-offline-tests.sh`: 19개 검사 통과.
- `tests/run-server-settings-tests.sh`: 16개 검사 통과.
- `tests/run-quick-entry-tests.sh`: 12개 검사 통과.
- `tests/run-ride-tests.sh`: 16개 검사 통과.
- `tests/run-backup-tests.sh`, `tests/run-restore-tests.sh`: 통과(복원 17개).
- `tests/test_catalog.py`: 49개 브랜드 / 522개 모델 데이터 검사 통과.
- Java 17 구문 파싱: 33개 소스 파일 통과. 이는 Android 타입 검사나 컴파일 성공을 의미하지 않습니다.

## 한계

- Commons 파일명은 촬영 연도나 제목의 잘못된 기재를 포함할 수 있습니다. 정확한 연식·트림의 제조사 공식 이미지 보장은 아직 구현되지 않았습니다.
- APK 빌드 시도(`./gradlew :app:assembleDebug --offline --no-daemon`) 실패: `services.gradle.org` DNS 조회 불가로 Gradle 8.11.1 배포 파일을 받지 못함. Android SDK도 발견되지 않음. APK 및 실기기 검증 미완료.
