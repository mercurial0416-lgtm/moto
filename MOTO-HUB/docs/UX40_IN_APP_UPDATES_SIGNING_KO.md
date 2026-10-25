# UX40 · 앱 내 업데이트 안정성 및 서명 확인

## 현재 사실
- 공개 업데이트 manifest: `main/updates/manifest.json`, 게시 versionCode **517**
- 현재 개발 소스 versionCode: **518** (이 숫자가 설치된 사용자 휴대폰 버전인 것은 아님)
- 새 UX38/UX39/UX40은 아직 배포된 업데이트 APK가 아님.
- manifest · 앱 `UpdateManifest.PINNED_CERT_SHA256` · `main/.github/workflows/moto-fixed-signed.yml`의 고정 인증서 SHA-256 일치:
  `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`.
- **인증서 지문 일치 설정은 고정 서명용 비공개 키의 존재, 새 APK 실제 서명, 사용자 휴대폰 설치본 인증서가 같음을 증명하지 않음.**
- 새 APK는 Android CI와 고정 개인키로 빌드하여 `apksigner verify --print-certs`로 인증서를 검증해야 함.

## UX40 변경
- 설치 앱의 인증서가 고정된 서명과 다르거나 확인되지 않으면 업데이트 체크·다운로드·설치 조작을 표시하지 않음.
- 다운로드 완료된 캐시의 유효한 manifest·버전·파일 크기를 확인할 때만 설치 버튼 표시. 실제 설치 전에는 기존대로 SHA-256과 APK 패키지/서명을 **다시 검증**.
- 설치 소스 versionCode가 공개 배포판보다 클 때 잘못된 “최신 공개 버전” 안내 대신 “아직 새 업데이트가 배포되지 않았습니다” 명시.
- 배포 새 버전의 다운로드 크기를 MB 단위로 표시.
- `UpdateVersionPolicy` 순수 Java 회귀 및 소스 연결 정적 검사 추가.

## 검증
- `bash MOTO-HUB/tests/run-ux40-update-policy-tests.sh`
- 순수 Java 8개 체크 통과, GitHub 소스 정적 회귀 8개 조건 통과.
- Android Gradle / lint / 실기기 설치·서명 검증 **미완료**.

## 업데이트 출시 전 필수
1. UI PR #39(UX38)과 #40(UX39), 본 UX40 PR의 병합·Android 빌드 검증.
2. **중요:** `main/.github/workflows/moto-fixed-signed.yml`는 현재 `build/di518-ux4-prebuild-20261020` 브랜치만 checkout. 신규 UX 코드가 통합되기 전에 워크플로를 돌리면 옛 버전을 빌드할 수 있음. 보완된 release guard가 반드시 필요.
3. 운영 Release Workflow의 signing secrets 존재 및 실제 인증서 확인. 키 원본 공개 금지.
4. 설치된 사용자의 APK 서명 인증서가 위 고정값인지 앱 내 “더보기 → 앱 정보 → 설치 서명 SHA-256”에서 확인.
5. 인증서가 다르면 Android의 덮어쓰기 설치는 불가능. 데이터/기존 앱을 **절대 먼저 삭제하지 말 것**. 전체 ZIP 백업을 우선 확보.

## 배포 금지 상태
- 새 UI APK 출시 전, 배포 서버 manifest(517)를 임의로 versionCode 518 이상으로 올리지 말 것.
- 임시 debug APK의 서명은 고정 서명과 다르므로 기존 설치본에 덮어쓰지 말 것.
