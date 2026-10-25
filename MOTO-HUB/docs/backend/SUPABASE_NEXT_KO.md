> Alpha4 시점의 설계 기록입니다. 최신 상태는 CURRENT_HANDOFF_KO.md와 CONNECT_ALPHA6_KO.md를 보세요.

# Supabase 연동 설계 초안 — 미구현/미배포
2026-10-16 확인: 연결 계정에는 chat(활성), game/PuttVision(비활성)만 있음. MOTO HUB용 프로젝트로 확정된 항목 없음. 기존 프로젝트는 변경하지 않음.
필요한 입력: MOTO HUB 프로젝트 지정(프로젝트 URL 또는 ref), 배포할 개발/운영 환경, 로그인 이메일 설정. 관리자키는 앱/ZIP/채팅에 넣지 말 것. 연결 도구를 통해 서버 변경하고 앱에는 publishable key만 사용.

## 구현 순서와 데이터 구조
- Supabase Auth 이메일 가입/로그인/로그아웃. 토큰은 Android Keystore 기반 암호화 저장. 로컬 기록은 로그인과 독립 유지.
- profiles(user_id, display_name, bike_summary), friend_codes(무작위 코드, 재발급). 코드 조회는 속도 제한한 서버 함수로 최소 정보만 반환. 전체 코드 목록 노출 금지.
- friend_requests(sender, recipient, state): 요청자만 생성/취소, 수신자만 수락/거절. 당사자 ID 수정 금지. 수락과 friendships 생성은 서버 트랜잭션.
- ride_groups(owner, name, rendezvous, destination), group_members(group,user,invited/accepted/removed). 방장 초대/승인, 초대받은 사용자만 수락. 무단 자체 가입 금지.
- sharing_sessions(owner, scope, group_id, enabled, ended_at): 매 라이딩 기본 OFF, 명시적 공유 동의 후 새 세션. 로컬 GPS 시작과 공유 시작을 분리.
- live_locations(session,user,lat,lon,bearing,status,received_at,expires_at): 최신 좌표 한 건만. 시간/TTL은 서버 계산. moving/stopped/fuel/rest 값 검증. 위도/경도/방향 범위와 송신 빈도 제한.

## 권한과 만료 계약
모든 노출 테이블 RLS+최소 GRANT. 익명 접근 금지. 소유자는 자신의 좌표만 송신, 조회자는 수락된 친구 또는 동일 승인 그룹원이며 활성 공유 범위와 만료 시각을 모두 만족해야 함. 요청중/삭제친구/퇴장그룹원 접근 거부. user_metadata로 권한 판단 금지.
Postgres Changes 구독과 초기 REST 조회에 동일한 RLS 적용. Realtime 삭제 이벤트의 권한 차이를 고려해 좌표 테이블에는 UPDATE 중심으로 만료 표시, 클라이언트도 TTL 타이머로 표식을 제거. GPS 좌표를 Broadcast 공개 채널에 올리지 않음.
공유 종료 시 클라이언트 전송 큐·재시도 취소 → 세션 비활성화/좌표 제거. 네트워크 단절 시 오래된 좌표 재전송 금지, 서버 TTL(예:90초)로 자동 비노출. 주기적 정리 작업으로 만료 좌표 삭제. 종료 요청 실패를 종료 성공으로 표시하지 않음.
방 권한 변경 이후 이미 연결된 구독에서도 신규 좌표가 더는 전달되지 않는지 두 계정으로 검증 필수.

## 실제 연동 완료 기준
A/B 가입·코드 요청·수락·삭제, C 무권한 접근 차단, 그룹 초대·가입 승인·퇴장, 위치 위조 소유자 거절, 공유 OFF/TTL/오프라인/재연결, 구독 권한 철회, 토큰 만료, 로그아웃 중 서비스 중단. DB 테스트와 두 Android 계정의 end-to-end 증빙을 별도 저장.
현재 위 테이블/RLS/Auth 클라이언트는 구현되지 않았고 서버 테스트도 실행하지 않음. Alpha5의 INTERNET 권한은 업데이트용이며 GPS 위치는 외부로 전송하지 않음.

참조: https://supabase.com/docs/guides/database/postgres/row-level-security
https://supabase.com/docs/guides/realtime/postgres-changes
https://supabase.com/docs/guides/realtime/authorization
https://supabase.com/changelog.md (2026-10-16 확인)
