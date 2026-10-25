# MOTO HUB Alpha6 C — 최종 개발 상태 / 2026-10-16
현재 버전: 4.0.0-alpha6-c / versionCode408 / com.motohub.next / minSdk26, compile·target36

완료 코드
- A: 이메일 가입/로그인/갱신/로그아웃, 이름·바이크 프로필, 친구코드 요청·수락·거절·목록·삭제.
- B: 비공개 그룹 생성·친구 초대·수락·거절·나가기·방 종료, 집결지·목적지 텍스트, 승인 그룹원 위치지도·상태·방향·마지막시각. HTTPS REST5초 주기 동기화이며 Supabase Realtime WebSocket은 미구현.
- 기존 GPS Foreground Service로 로컬 기록을 유지하며 별도 동의 후 그룹 공유. 매번 기본OFF. 정차 때에도5초/0m GPS요청. 모의·범위오류·오래된 좌표 배제.
- 중지 시 대기좌표 폐기, 예약송신 취소, 진행 HTTP disconnect, 서버 종료요청. 실패시 확인실패 표시·서버30초TTL 비노출. 앱 프로세스 종료 후 공유 자동복원 없음.
- C: 송신 직전 위치·알림 권한 확인, 위치권한 해제/GPS꺼짐/서비스종료 시 중단. 알림허용 해제 시 공유 중단(주기체크최대5초). 로그아웃 때 그룹 캐시 삭제, 지도 다크 타일 필터, 지도 pause시 좌표표시/조회 취소.
- 기존 개인 기록·GPS/GPX·업데이트 기능 재사용. 소스에 비밀키/관리자키 없음. Auth토큰은 메모리 전용, 프로세스 종료 후 재로그인.

이번 실행 검증
- B: Gradle assembleDebug/lintDebug 성공. JVM44개(기존37+Sharing7) 통과.
- C 최종: Gradle assembleDebug/lintDebug 성공. 위치·알림 철회, GPS꺼짐, 서비스종료, 취소 요청, 공유기본OFF/최신좌표폐기/TTL/입력/세션 등 집중 JVM18개 통과(SharingPermission5+Sharing7+SocialClient6). B/C 중복13개를 추가 합산하지 않음.
- Lint0오류/2경고(문자열 리소스 권고). APK v2서명/패키지 검사.
- A+B SQL을 로컬PostgreSQL WASM(PGlite0.3.14)에서 실제 실행, 권한18개 통과. 테스트 auth.uid 스텁과3계정: 친구요청 수신자만 수락, 무관계계정 요청/좌표조회 차단, 수락전공유거부, 수락멤버조회허용, 타인세션송신거부, 직접좌표수정거부, 탈퇴권한철회, 공유종료시삭제/재송신거부, 만료RLS, 익명차단.
- 원시로그 evidence/current/build-alpha6-b-final.txt, build-alpha6-c-final.txt, db-groups-test.txt. C최종XML은 evidence/current/alpha6-c-tests. 과거결과는 과거버전 증빙.

미완성/미검증
- 현재 APK는 Supabase 프로젝트 URL/key 없는 미연동 상태. 실제 로그인·이메일·두계정/두기기 공유·Supabase RLS/PostgREST 통합은 미실행. 로컬DB 결과를 운영서버 검증으로 주장하지 않음.
- GalaxyS25/Android16 실제 APK설치·실행, 실제지도타일/제스처/키보드/접근성, 화면OFF·배터리최적화·강제종료 GPS, 네트워크단절/토큰갱신 end-to-end, 업데이트 설치/데이터보존은 미검증. KVM·연결기기 없음.
- 물리적으로 만료좌표를 주기삭제하는 Cron은 미설정. RLS는30초 후 비노출. 강제퇴장·방정보수정·GPX가져오기·로그인유지 암호화 저장은 후속.
- 실제서버 없이 친구나 좌표를 가짜 데이터로 표시하지 않음. 기본지도 중심0,0은 사용자 위치가 아니며 실제 좌표가 올 때만 마커 생성.
- 지도SDK osmdroid6.1.20 고정. 상용화 시 유지관리되는 SDK/타일서비스 검토 필요.

빌드·서버연결
JDK17/Gradle8.11.1/AGP8.10.1/SDK36/BuildTools36.0.0
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
서버 빌드 옵션: -PsupabaseUrl=https://PROJECT.supabase.co -PsupabaseKey=sb_publishable_...
MOTO HUB용 프로젝트 URL/ref·publishable key와 이메일인증/SMTP/확인URL 필요. 관리자키는 보내거나 APK에 넣지 말 것.
A→B 순서 supabase/migrations 적용. moto_private를 Data API 노출스키마에 추가하지 말 것.
로컬DB 재현: npm ci --prefix tests/db && node tests/db/groups.mjs (프로젝트루트, 로컬전용).
자세한 연결절차: docs/backend/CONNECT_ALPHA6_KO.md

설치/데이터
서명 인증서 SHA256: 253dd5476624f64271dd80f3d6a9f253f89f5dfb33723adb6915885b5391f683
Alpha6 A/B/C는 동일 인증서. 실제 업데이트 설치는 미검증. Alpha5와는 서명달라 덮어쓰기 불가.
앱삭제 전 JSON외부백업 + GPS의 GPX 내보내기. schema3/motohub-v3.json 유지. GPS경로/초안/출발점검은 JSON백업 제외. 원래 서명키/사용자소유 운영키 없이 다음 환경에서 업데이트 호환을 보장할 수 없음. 서명키는 소스ZIP에서 제외.

다음 작업
1. MOTO HUB 서버 프로젝트 지정 후 A+B 배포와 Auth/두계정/제3계정 검증.
2. 실제S25에서 권한·화면OFF·알림·공유종료·위치TTL·지도 검증.
3. 결과 기반 오류수정. C파일이 저장됐으므로 신규대화는 이ZIP을 풀고 이문서부터 읽기.
