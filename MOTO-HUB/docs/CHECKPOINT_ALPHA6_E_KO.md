# MOTO HUB 4.0 Alpha6-E — 서버 연결 체크포인트

기반: Alpha6-D (versionCode 409). 신규 소스: Alpha6-E (versionCode 410).

## 실제 서버 구성 완료 (2026-10-16)

- 새로운 전용 Supabase 프로젝트 **MOTO HUB** 생성 (`ap-northeast-2`, 서울). 기존 `chat`·`game`·`PuttVision`은 변경하지 않음.
- API URL `https://paraeadvvdqyztqqhxpu.supabase.co`.
- 프로젝트별 **publishable 키**를 `gradle.properties`에 추가. 공개키이며 **service_role/secret 관리자 키는 포함되지 않음**. 실제 APK를 다시 빌드하면 기본값으로 서버에 접속하도록 빌드됨. 기존 Alpha6-C/Alpha6-D APK는 별개이며 새 연결 설정을 자동 수신하지 않음.
- 서버 DDL 마이그레이션 적용 확인: `checkpoint_a_friends`, `checkpoint_b_groups`, `location_cleanup`, `access_indexes`.
- Auth: Supabase의 기본 이메일·비밀번호 인증. 이메일 확인/발송 설정과 실제 가입 성공 여부는 아직 검증하지 않음.
- RLS 활성화: 프로필, 친구 코드·요청, 그룹·멤버·공유 세션·좌표. 익명 계정의 좌표 직접 조회/쓰기 및 좌표 발행 RPC 권한이 모두 없음을 SQL 권한 검사로 확인.
- 위치 좌표: SQL RLS로 30초 이후 조회 불가; pg_cron 작업 `moto-hub-expire-gps`를 5분 주기로 생성했고 active=true 확인. 예약 작업 `active=true` 확인. 최초 실제 실행 결과는 검증 전.
- FK 인덱스와 프로필 읽기 정책 병합으로 성능 진단 일부 개선.

## 이번 로컬 검증

- `bash tests/run-offline-tests.sh` 19/19 통과.
- `bash tests/run-server-settings-tests.sh` 16/16 통과.
- Supabase migration API 적용 성공; 서버 공개 스키마 7개 테이블 및 private 1개에 RLS 활성화 확인.
- **실제 Supabase DB 트랜잭션 검증 통과:** 테스트용 auth.users 3명을 생성해 프로필 RLS, 친구 코드 요청, 친구 수락, 그룹 방 생성·초대·수락, 위치 발행, 그룹원 조회, 제3자 위치조회 거절, 공유 종료 후 좌표 삭제를 확인. 트랜잭션 `ROLLBACK` 후 임시 auth.users/프로필/좌표/그룹이 **0건**임을 별도 쿼리로 확인. (실제 안드로이드 앱·회원가입 이메일 경로는 아직 테스트하지 못함)
- Supabase security advisor: private `request_limits` 테이블에서 정책 없음(INFO) 1건. 이 테이블은 앱 역할에 테이블 접근 권한을 주지 않아 의도한 상태.

## 미완료 및 정확한 제약

- **이번 환경에는 Android SDK가 없어 Alpha6-E APK를 생성·설치·실행 테스트하지 못함.** ZIP은 실제 수정된 소스임. 이전 APK를 Alpha6-E로 표시하면 안 됨.
- 실제 두 계정 가입, 친구 요청·수락, 그룹 초대, 두 대의 휴대폰에서 GPS 교환, 알림·백그라운드 동작은 미검증.
- 사용자 위치/친구 데이터는 아직 없음. 실제 라이더 2명은 각자 앱에서 회원가입이 필요함.
- Supabase 무료 플랜도 사용량·이메일 전송량 제한이 있을 수 있음. 비용이 늘어나는 업그레이드는 실행하지 않음.
- 서버 주소와 공개키는 클라이언트 측 공개값이며 보안은 RLS와 서버 권한 정책으로 유지해야 함. 이 키로 누구나 회원가입을 시도할 수 있으니 장기 공개 서비스로 전환하기 전 스팸 방지 설정 필요.
- 기존 debug APK와 새 CI 빌드 서명은 다를 수 있음. 설치 전 JSON 백업하고, 패키지/서명 검증 없이 덮어쓰기 불가.

## APK 빌드 방법

Java 17 + Android SDK API 36 환경에서 `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`. APK는 `app/build/outputs/apk/debug/app-debug.apk`.
별도 GitHub 저장소에 프로젝트 루트를 업로드하고 GitHub Actions `MOTO HUB Android checkpoint APK + source`를 실행하면 APK·소스 ZIP·검사값 아티팩트를 받도록 워크플로 설정이 포함됨. 실제 Actions 빌드는 아직 수행하지 않았음.

## 첫 실사용 시험

1. APK를 새로 빌드·설치 후 친구 탭에서 `연결 서버: https://paraeadvvdqyztqqhxpu.supabase.co` 확인.
2. 서로 다른 2명 각자 이메일로 가입 → 이메일 확인(요구 시) → 로그인 → 프로필 등록.
3. 친구 코드 공유 → 요청 → 수락 → 비공개 방 생성 → 친구 초대 → 참가 수락.
4. 두 휴대폰 모두 정확한 위치·알림 권한 허용 후 라이딩 탭에서 GPS 기록 시작.
5. 각자 그룹에서 위치 공유에 명시적으로 동의, 지도에 두 명의 좌표 표시 확인.
6. 제3자 계정 비공개, 공유 중단·방 퇴장·화면 OFF·네트워크 차단 시 마커 제거 및 좌표 재노출 방지 검증.

실제 검증 전까지 친구 GPS를 완성/실사용 가능이라고 주장하지 않는다.
