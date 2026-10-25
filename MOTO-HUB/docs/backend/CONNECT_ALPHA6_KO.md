# Alpha6 서버 연결 및 검증 순서
현재 실제 Supabase 프로젝트는 미지정·미연동. 별도 MOTO HUB 프로젝트에만 적용할 것.
1. 프로젝트URL/ref와 publishable key 확보. 이메일/비밀번호 로그인 활성화, 확인메일용SMTP 및 확인URL 설정. 앱은 확인메일 후 비밀번호로 다시 로그인하는 흐름이며 딥링크 자동로그인은 미구현.
2. A → B 순서로 supabase/migrations SQL 적용. moto_private는 Data API exposed schemas에 추가하지 말 것. 원격적용은 아직 실행하지 않음.
3. ./gradlew :app:assembleDebug -PsupabaseUrl=https://PROJECT.supabase.co -PsupabaseKey=sb_publishable_...
4. 두 계정 가입·확인·로그인·프로필 생성 → 코드 요청 → 수락 → 방 생성·초대·수락 → 각기기 GPS기록 시작 → 그룹 공유 동의 → 지도 확인.
5. 제3계정/초대미수락 계정 접근거부, 탈퇴/방종료, 위치권한/알림철회, 화면OFF·배터리제한, 네트워크단절·재연결·토큰만료·로그아웃을 실제 확인. 로컬DB테스트는 Supabase통합검증 대체가 아님.
기본 공유OFF. 프로세스 재시작 시 공유/세션 복원하지 않음. 공유 종료는 즉시 로컬 송신 취소, 서버 종료확인 실패를 표시. 서버 좌표30초 TTL은 SELECT시 비노출이고 물리적인 정기삭제는 별도 Cron 운영 필요.
운영정리 예: 관리자권한 Cron에서 delete from public.mh_positions where expires_at < now(); (현재 미설정)
WebSocket Realtime 대신5초 HTTPS REST snapshot 사용. Supabase RLS/RPC 권한이 매 요청에 적용됨.
OpenStreetMap 타일은 외부 네트워크 필요. osmdroid6.1.20 고정(보관된 라이브러리): 상용화 시 유지관리되는 지도SDK와 타일서비스 정책 재검토 필요.
개발용 Auth세션은 메모리 보관. 앱 완전종료 후 재로그인. 비밀번호/토큰은 로그/파일로 저장하지 않음.
