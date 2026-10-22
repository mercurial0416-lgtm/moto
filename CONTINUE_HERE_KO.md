# MOTO HUB 최신 작업 재개 — Alpha6 DI518 UX21 (2026-10-22)

## 최신 기준
- GitHub: https://github.com/mercurial0416-lgtm/moto
- 소스: `build/di518-ux4-prebuild-20261020` 브랜치의 `MOTO-HUB/`
- UX21 통합 커밋: `fdfebbaee7cb0dd8793e413dc310044b99c6b9fe`
- Android 패키지 `com.motohub.next`, 일반 빌드 `versionCode 518`.
- `main`은 release/update manifest와 workflow 중심이며 **완전한 최신 Android 소스는 위 통합 브랜치에 있음**. 이전 ZIP을 다시 풀어 소스를 덮어쓰지 말 것.
- 기존 로컬 데이터, 백업, GPS 라이딩, GPX, 정비/주유, 사진, 위젯, OBD 읽기 전용 개발자 기능을 유지.

## 반영 완료
1. UX18: 같은 날짜/거리에서 정비 완료 기록 선택 통일, 알림 OFF/ON 상태 초기화.
2. UX19: 최근 주유·주유 단가 UI, 가득 주유 계기판 표시, 갤러리 선택/미리보기 분리, 비트맵 정리.
3. UX20: 자동 계산 금액의 오래된 값 제거 및 수동 입력 보호, JobService 중단/스냅샷 경쟁 완화, 공개 업데이트 버전에 따라 고정 서명 릴리스 버전 자동 선택. [PR #19](https://github.com/mercurial0416-lgtm/moto/pull/19), [Android 빌드 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39759995384).
4. UX20 main: 고정 서명 APK 검증 (패키지명·인증서 SHA-256·버전·SHA256), 소스 ZIP과 체크섬 아티팩트, 명시적 수동 publish [PR #20](https://github.com/mercurial0416-lgtm/moto/pull/20) 병합.
5. UX21: 지난 주유 불러오기 중 계산기가 실제 결제액을 덮어쓰는 오류 수정, 가득 주유 계기판 편집칸 유지, 추적 상태 초기화. [PR #21](https://github.com/mercurial0416-lgtm/moto/pull/21), [Android 빌드 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39760731453).

## 미완료·검증 필요
- 고정 서명 private keystore 및 Secrets 설정 여부는 확인하지 못함. `main` 서명 workflow는 아직 실행·검증하지 않음.
- 사용자가 현재 설치 중인 앱의 인증서 SHA-256이 고정 서명 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`와 일치하는지 확인 필요.
- 갤럭시 실기기에서 온라인 바이크 사진, 라이딩 GPS 백그라운드 장시간 기록, 정비 알림 예약, 고정 서명 업데이트 설치·데이터 보존 테스트 미실행.
- 향후 개선 후보는 실제 기기 문제 로그 기반 사진 로드 폴백, 접근성·대형 데이터 UI 검증, 백업/복원 중단 복구의 계측 테스트 등.

## 절대 하지 말 것
- 서명키/비밀번호를 공개 저장소에 올리기.
- GitHub CI debug APK를 기존 설치본의 고정 서명 업데이트로 주장하기.
- 이전 ZIP에서 프로젝트를 새로 만들어 UX20/21 코드를 지우기.
- 사진 매칭/업데이트/GPS가 실기기에서 검증됐다고 주장하기.

## 새 대화에서 바로 시작
`mercurial0416-lgtm/moto`의 `build/di518-ux4-prebuild-20261020` 최신 HEAD에서 `CONTINUE_HERE_KO.md`를 읽고 UX21 이후 개발을 이어가. CI Java/Android 빌드/Lint/유닛 테스트는 성공했고, 고정 서명 릴리스 및 실기기 검증은 별도야. 변경 후 GitHub 커밋·검증·PR 병합으로 결과를 보존해.
