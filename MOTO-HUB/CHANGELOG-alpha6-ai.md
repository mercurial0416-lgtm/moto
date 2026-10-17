# MOTO HUB 4.0 Alpha6-AI

- 홈 화면에서 바이크가 2대 이상 등록된 경우, 대표 사진 좌측 상단의 `MY BIKE` 표시를 `⇄ 바이크 변경 ▾` 버튼으로 변경.
- 탭하면 기존 `HubScreens.chooseBike()` 선택창을 열고, 선택 직후 홈 화면을 다시 표시. 사진 영역 탭의 사용자 사진 변경 기능은 유지.
- 바이크 0~1대일 때는 불필요한 전환 버튼을 표시하지 않음.
- 기존 데이터 저장·주유·정비·매물·GPS/GPX·친구/그룹·카탈로그·백업/복원·위치 공유 동의 로직 변경 없음.
- `HomeBikeSwitchTest` 5개 검증 및 소스 연결 검사 실행. Gradle APK 빌드 시도 실패: `services.gradle.org` DNS 조회 실패(`UnknownHostException`). 원격 CI 갱신도 연결 도구의 안전 검사에 차단됨. APK/실기기 검증 미완료.
