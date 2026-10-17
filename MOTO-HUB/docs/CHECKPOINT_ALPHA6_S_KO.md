# MOTO HUB 4.0.0-alpha6-s · GPS 시작 준비 상태 UI

기준: 검증된 Alpha6-R 전체 소스 ZIP (`MOTO-HUB-4.0.0-alpha6-r-source.zip`). 작업일: 2026-10-17.

## 재현 가능한 UX 문제

라이딩 탭에서 GPS나 앱 알림이 꺼져 있어도 `READY TO RIDE`와 시작 버튼만 보였다. 실제 시작은 서비스 내부에서 실패하고, 사용자는 실패 이유를 확인하기 어려웠다. 위치·알림 권한이 없을 때도 설정 진입 안내가 부족했다.

## 실제 변경

- 순수 Java 상태 정책 `RideReadiness` 추가: 기록 중 / 정확한 위치 권한 없음 / GPS 꺼짐 / 알림 권한 없음 / 앱 알림 차단 / 준비 완료.
- 라이딩 화면에 상황별 안내 카드와 권한·설정 진입 버튼 추가.
- GPS 꺼짐 → 시스템 위치 설정, 앱 알림 차단 → 해당 앱 설정으로 이동.
- 설정 화면에서 돌아오면 라이딩 준비 상태 자동 갱신.
- 최근 GPS 서비스 상태 표시: 시작 실패가 READY TO RIDE 뒤에 숨지 않도록 함.
- 기존 GPS 기록 동의와 별도 그룹 위치 공유 동의 절차 유지. 개인정보 공유 기능, 저장 데이터, API 및 백업 형식 변경 없음.
- 버전 `versionCode 424`, `versionName 4.0.0-alpha6-s`.

## 실제 실행 검사

- `tests/run-ride-readiness-tests.sh`: 상태·안내 25개 검사 통과, Android UI 연결 확인 통과.
- 기존 순수 JVM/정적 회귀 검사 스크립트 13개 모두 통과: 초안, 날짜, 카탈로그 검색/연식, 공유 링크, 사진 요청/제목/대체이미지, GPS·GPX, 백업·복원, 오프라인 정비, 서버 설정.
- `tests/test_catalog.py`: 49개 브랜드·522개 모델 검증 통과.
- `JavaSyntaxCheck`: Android Java 소스 38개 파일 파싱 통과. **실제 Android 컴파일 검사는 아님.**
- `./gradlew :app:assembleDebug --offline --no-daemon`: **실패**. 로컬 Gradle 배포본 부재로 `services.gradle.org` 다운로드 시도, DNS `UnknownHostException`. Android SDK도 현재 실행 환경에 없음.
- APK 생성 없음, 실제 안드로이드 단말 테스트 없음, Supabase 실서비스 테스트 없음.

## 다음 점검 권장

Android SDK + Gradle 8.11.1을 갖춘 빌드 환경에서 `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` 실행. 실제 기기에서 GPS 꺼짐/정확한 위치 거부/알림 거부/설정 복귀/시작 실패 메시지를 확인.
