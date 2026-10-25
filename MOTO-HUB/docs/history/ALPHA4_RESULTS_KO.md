# MOTO HUB 4.0 Alpha4 GPS — 최신 검증·인수인계
2026-10-16 / 기준 소스: 첨부 Alpha1 전체 소스와 코드 분석 보고서

## 제공 버전
- Alpha3 UI: UI 구조 개선 후 먼저 저장·전달한 별도 체크포인트.
- Alpha4 GPS: Alpha3 UI + 네이티브 로컬 GPS·GPX. versionCode404, versionName4.0.0-alpha4-gps.
- 패키지 com.motohub.next, minSdk26, compile/targetSdk36, Java17, AGP8.10.1, Gradle8.11.1.

## 구현된 변경
1. 홈을 선택 바이크·주행거리·점검·최근 기록 중심으로 재구성. 빠른 주유·정비 버튼 하단 고정.
2. 기록을 차량·종류·월별 목록으로 변경. 차량관리/설정을 별도 화면으로 분리.
3. 매물 검색·필터·정렬·가격 중심 목록과 한 번에 여는 비교. 하단 홈/기록/매물/라이딩/친구 탭. 라이트/다크, 필수/추가 폼 분리.
4. RideService: 사용자 동의와 정확한 위치/알림 권한 후 명시적 시작. location foreground service, 알림 종료, 앱 종료 버튼, START_NOT_STICKY. 자동 시작/재부팅 복구 없음.
5. LocationManager GPS 요청 5초/5m, 실제 전달 주기는 OS/GPS에 따라 다름. 오래된/정확도100m초과/범위오류/모의위치/역행시간 좌표 제외. 위치 권한 해제·GPS 꺼짐·쓰기 실패 시 중단. 중단 시 listener 제거.
6. TrackFile: 앱 내부 rides/*.track append/flush/fsync, 경로당25MB 제한. 비정상 종료의 미완성 마지막 행 제외, 정상 저장 좌표만 복구. 완성된 행 손상은 오류 표시.
7. 종료한 기록을 SAF 문서 생성으로 GPX1.1 내보내기. 경위도와 UTC시각. 기록중 내보내기 금지, 빈 기록에 좌표 생성하지 않음. GPX 가져오기는 미구현.
8. 기존 Store, schema3, motohub-v3.json 유지. 서버 전송 코드/INTERNET 권한 없음.

## 실행 검증
- Gradle assembleDebug + lintDebug + testDebugUnitTest 성공. Lint 0 errors/1 warning(SetTextI18n).
- 순수 Java 기존 테스트54개 통과: Store25, Fuel10, Offline19. 해당 클래스는 GPS 추가 중 변경하지 않음.
- Gradle JUnit19개 통과: NativeUi8 + TrackFile7 + RideService4. 총73개(중복 실행 횟수 합산 아님).
- NativeUi8: 저장/재생성, 가격검증, 초안복구, 복원/되돌리기, 비교 진입, 계산기 진입, 탭/뒤로가기, 화면 렌더링.
- TrackFile7: 좌표 범위, XML GPX 생성/값 보존, 미완성 끝행 복구, 손상 완성행/역행시각/빈파일 거부.
- RideService4: 권한없음 시작거부, 종료명령, 중지상태 좌표무시, 명시적 시작→테스트 좌표 기록→종료→listener제거→추가좌표 미기록.
- 서비스 테스트는 Robolectric 가상 LocationManager와 테스트 좌표를 사용. 실제 GPS 수신/실외 주행 테스트 아님. 테스트 좌표는 APK 초기 데이터에 포함되지 않음.
- 화면 이미지20장: Robolectric API35 native rendering,320dp/font130%,393dp/font100%,light/dark,5탭. 에뮬레이터 스크린샷이 아님. 홈/기록/매물 및 라이딩 이미지를 직접 검토.
- APK apksigner 서명 검사와 aapt2 패키지 확인. 원시 증빙 evidence/current 및 TEST-*.xml 참조.

## 확인하지 못한 사항
- 실제 APK 설치/시작, Galaxy S25/Android16 런타임, 화면OFF GPS 지속, 배터리최적화/강제종료, 실제 GPS 권한팝업, 실제 SAF 파일앱 GPX/JSON 왕복, 네트워크 단절과 친구 위치 공유, 시스템키보드/제스처 뒤로가기, 업데이트 설치 데이터 보존.
- API35 JVM 검증은 API36 실기기 검증을 대체하지 않음. 현재 환경 KVM/에뮬레이터 없음.
- 상용 배포 완료판이 아닌 디버그 개발판. 알림 꺼짐/세부채널 상태, 제조사 배터리 제한은 실기기에서 추가 확인 필요.

## 설치 및 백업
현재 APK 인증서 SHA256: 9fcdd9d295b2878e5eeee6a1723f316cf2990d86300304e1b82c26d4ebe18642
기존 Alpha1 인증서: 17b9af6db8d61ee2676936ebf3be29a4782be49d46d7d34d1165c70357f0b2e5
기존 Alpha1과 서명이 다르므로 덮어쓰기 불가. 기존 앱에서 JSON 백업을 외부에 저장한 뒤 원래 키로 빌드하거나 재설치/복원 필요. 앱 삭제 시 내부 데이터가 삭제됨. 비밀 서명키는 전달 ZIP에 포함하지 않음.
이번 Alpha3/Alpha4는 같은 인증서지만 실제 업데이트 설치/보존은 미검증. 다음 세션 재빌드 환경이 바뀌면 키가 다시 달라질 수 있으므로 운영 서명키를 사용자 측에서 관리해야 함.
JSON 백업에 저장된 차량/정비/주유/매물/코스 등은 포함. 초안/출발점검/별도GPS파일은 제외. GPS는 앱 삭제 전 GPX로 별도 내보내기.

## 미완료 및 다음 작업 순서
1. 실제 갤럭시에서 설치→차량/정비/주유/매물 저장→백업/복원→GPS 권한거부/허용→화면OFF 주행→종료→GPX를 검증하고 증빙 저장.
2. MOTO HUB용 Supabase 프로젝트 지정 필요. 현재 연결 계정의 chat/game/PuttVision은 임의 사용하지 않았음. 프로젝트 URL/ref와 개발·운영 환경을 지정하면 Auth/RLS/Realtime 구현 가능. 관리자키를 소스에 넣지 말 것.
3. docs/backend/SUPABASE_NEXT_KO.md에 데이터 구조, 친구/그룹 승인, 공유 OFF·TTL·권한철회와 두 계정 검증 설계. 설계만 완료, 테이블/RLS/Auth/지도/두 계정 연동은 미구현·미실행.
4. 앱 내 업데이트는 미구현. 실제 HTTPS 업데이트 메타데이터/APK 서버와 운영 서명 전략 필요. 패키지/높은버전/인증서/SHA256 검증, 다운로드 실패, 사용자 승인 설치, 데이터 보존을 구현·검증할 것.
5. GPS 파일 삭제/통합백업/지도경로 보기, UI 서비스 상태 자동 갱신 개선은 후속. 현재 상태 새로고침 버튼 제공.

## 다음 세션에서 먼저 읽을 파일
README_KO.md, 이 문서, TEST_RESULTS_KO.md, docs/backend/SUPABASE_NEXT_KO.md.
현재 소스: HubScreens.java, MainActivity.java, RideService.java, RideUi.java, TrackFile.java.
docs/history 및 evidence/current 이외 오래된 문서는 해당 과거 버전 기록이며 현재 완료 상태로 사용하지 말 것.

참고 문서
https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
https://developer.android.com/training/permissions/requesting
https://supabase.com/docs/guides/database/postgres/row-level-security
https://supabase.com/docs/guides/realtime/postgres-changes
