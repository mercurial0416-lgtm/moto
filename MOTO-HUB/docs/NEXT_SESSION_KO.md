# 최신 소스 체크포인트: MOTO HUB 4.0.0-alpha6-aa (2026-10-17)

기준 소스: `MOTO-HUB-4.0.0-alpha6-z-source.zip`.
이번 수정: 라이딩 기록 상태 필터를 로컬에 저장해 앱 재실행 후에도 유지. 회전 상태 우선·손상된 값은 '전체'로 복구.
검사: 전체 20개 테스트 스크립트 통과 / 1개 JSON_JAR 부재로 미실행. Java 43개 구문 검사 통과. Android APK 빌드 실패(DNS·SDK 부재).
다음 우선순위: SDK 36/Gradle 8.11.1 환경에서 APK 빌드 후 실기기 라이딩 기록 화면 검증.

# 최신 소스 체크포인트: MOTO HUB 4.0.0-alpha6-u (2026-10-17)

기준 소스: `MOTO-HUB-4.0.0-alpha6-u-source.zip`.
이번 수정: GPS 시작 동의 후 위치/알림 권한을 연달아 승인하면 재탭 없이 시작 요청. 거절·탭 이탈·설정 차단 시 자동 시작 금지.
기존 Alpha6-T의 정비·주유·매물·GPS·GPX·Supabase 친구/그룹·카탈로그·백업/복원 보존.
검사: 신규 동의 흐름 18개, 전체 17개 스크립트 중 16개 통과. `run-model-tests.sh`에는 `JSON_JAR` 필요. 카탈로그 49브랜드·522모델 통과.
Android SDK 및 Gradle 배포본 부재, DNS 연결 실패로 APK는 생성하지 못함. 실기기 테스트 미실시.
다음 우선순위: SDK/Gradle 환경에서 Android 빌드, 첫 실행 위치/알림 권한 및 실기기 GPS 시작 검증.
