# MOTO HUB 4.0 Alpha6-D 체크포인트

기반: 사용자가 업로드한 Alpha6-C 소스, `com.motohub.next`, 409 / 4.0.0-alpha6-d.

## 이번 수정

- **앱 안에서 Supabase 공개 연결 정보 설정:** 친구 탭의 `친구 서버 연결 설정` 화면에서 `https://<ref>.supabase.co` URL과 `sb_publishable_...` 키를 저장한다. APK 재빌드 없이 서버 주소를 변경할 수 있다. 주소/키는 앱 전용 SharedPreferences에 기록되며 **비밀번호, 사용자 토큰, 비밀 관리자키는 저장하지 않는다.**
- HTTPS, 공식 `*.supabase.co` 호스트, 포트/쿼리/추가 경로 금지, 공개키 형식 검증. 서드파티 주소·관리자키 거부. 로그인·공유 중 서버 변경 금지.
- 미연동 UI에서 서버 설정을 찾기 쉽게 개선하고 현재 서버 주소를 표시한다.
- GitHub Actions APK+소스 ZIP+SHA-256 동시 산출물 체크포인트. 이전 파이프라인의 미설치 `JSON_JAR`에 의존하지 않도록 순수 Java 테스트로 대체하고 Gradle 테스트도 수행하도록 변경.

## 검증

- `bash tests/run-server-settings-tests.sh` : **16검사 통과** (2026-10-16, 이 환경에서 직접 실행)
- `bash tests/run-offline-tests.sh` : **19검사 통과** (2026-10-16, 이 환경에서 직접 실행)
- **Android SDK/Gradle 캐시가 이 실행 환경에는 없어 새 APK를 생성하지 못했다.** 기존 Alpha6-C 빌드 기록을 새 Alpha6-D의 빌드 성공 증빙으로 사용하지 않는다.
- 실제 Supabase 프로젝트 연결, 이메일 인증, 두 기기 GPS 동기화, UI 실기기 테스트는 **미검증**.

## 바로 빌드하는 방법

Android SDK 36, Java 17, Gradle 래퍼 실행이 가능한 PC/CI에서:

`./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`

또는 별도 GitHub 저장소에 소스 루트를 업로드한 다음 **Actions → MOTO HUB Android checkpoint APK + source → Run workflow**를 실행한다. 완료 후 `moto-hub-alpha6-d-checkpoint` 아티팩트에서 APK, 소스, 체크섬을 함께 다운로드할 수 있다. **이 작업공간에서 직접 GitHub 액션을 실행한 것은 아니다.**

빌드된 Debug APK는 서명 인증서가 빌드 환경마다 달라질 수 있다. 기존 앱에 덮어쓸 수 있다고 보장하지 않으며, 업데이트 전 앱 데이터 백업이 필요하다. 서명키/비밀키는 저장소에 업로드하지 않는다.

## 실제 친구 실시간 GPS 연결까지 남은 것

- 별도의 **MOTO HUB 전용** Supabase 프로젝트를 선택/생성하고 첫 두 SQL 마이그레이션(`supabase/migrations`)을 서버에 적용한다. 기존의 unrelated 프로젝트는 임의 수정하지 않는다.
- 앱 친구 탭에 프로젝트 URL과 publishable key를 입력한다.
- 이메일 로그인, 친구 수락, 방 입장, 그룹 위치 공유를 **두 대의 Android 기기**에서 테스트한다.
- 위치 공유 취소·로그아웃·오프라인·권한 철회·화면 OFF에서 타 사용자 접근 여부, 30초 TTL 및 RLS 서버 동작을 확인한다.
- 데이터 모델 유지, GPS/GPX/정비·매물 등 기존 Alpha6-C 기능은 변경하지 않았다.
