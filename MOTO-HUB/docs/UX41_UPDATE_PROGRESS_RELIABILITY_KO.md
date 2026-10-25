# UX41 · 앱 내 업데이트 진행 상태와 다운로드 복구

## 개선 범위
- 파일 수신 진행률을 여러 개의 Toast 대신 화면의 진행 막대/진행 상태 문구로 표현.
- "배포 버전 확인", "APK 다운로드 중", "파일 무결성 확인", "APK 서명 확인", "설치 전 재검사" 단계 구분.
- 백그라운드 진행 동안 UI 스레드 차단 없음. 현재 보이는 업데이트 페이지가 없어져도 개인 기록은 수정하지 않음.
- 순수 Java `UpdateProgressPolicy`: 잘못된 숫자는 0%, 수신량이 전체보다 크면 100%, 수신 중은 0~99%, 완료 후 100%.
- 크기·해시 검사에 실패한 *캐시된 설치 파일*은 `SharedPreferences.updates.ready` 포인터만 제거하여 설치 가능 표시가 재노출되지 않도록 함. 데이터베이스·사진·GPS 기록 및 원본 백업에는 영향 없음.
- Android APK 파서 호환성을 위해 검증 전 임시 수신 파일을 `download.part.apk`로 저장. 앱 내부 전용 캐시에서 최종 검사 전에는 해시 파일명으로 노출하지 않음.

## 보호 조건 유지
- 현재 설치본의 고정 SHA-256 서명 확인 (`212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`)
- HTTP(S) 안전 리디렉션 제한, 최대 다운로드 바이트, SHA-256, 앱 패키지/버전, 동일 Android 서명 인증서 검증
- GPS 진행·OBD 자동 대기·백업/복원 중 설치 금지
- Android 시스템 설치 확인을 사용자에게 요청하고 앱 내 자동 무인 설치는 하지 않음
- 고정 서명 개인키와 public manifest 버전 517 변경 없음

## 검증
- Java 21이 설치된 작업 컨테이너에서 `UpdateProgressPolicy`와 대응 회귀 코드 독립 컴파일·실행: **13 assertions PASS**.
- GitHub에서 PR 소스 재조회 후 17개 정적 연결/보안/구조 체크: **17/17 PASS**.
- `tests/run-ux41-update-progress-tests.sh`를 저장소에 추가. 통합 소스에서 공식 스크립트를 다시 실행해야 함.
- Android Gradle/실제 다운로드/실기기 설치/기존 APK 서명 비교는 **미실행**. 인증서 실제 일치가 확인된 새 APK를 생성하거나 출시했다고 주장하지 말 것.

## 병합/배포
- stacked PR: UX38 → UX39 → UX40 → UX41 순서로 검토/CI, `main/.github/workflows/moto-fixed-signed.yml` release guard PR도 UX41까지 추가해 새 소스 누락 시 fail-closed.
- 빌드 전 signing secrets 존재와 설치 APK 고정 signer 일치 여부 검증할 것.
- 기존 설치 앱 삭제 금지.
