# MOTO HUB — 최신 개발 재개 지점 (2026-10-22)

## 반드시 이 기준 사용
- 저장소: https://github.com/mercurial0416-lgtm/moto
- **실제 최신 소스 브랜치:** `build/di518-ux4-prebuild-20261020`, 디렉터리 `MOTO-HUB/`.
- 최신 통합 커밋: `f7b05136fefd38c3fc0b1085a5a480f3f68dfcb4` (UX19).
- Android: `com.motohub.next`, `versionCode 518`.
- `main`은 아직 역사적 ZIP + 업데이트 manifest + GitHub Actions 위주이고 최신 앱 소스 전체가 없음. `main`이나 루트 ZIP 기준으로 앱을 새로 만들지 말 것.
- UX17/UX18/UX19와 앞선 변경이 통합 브랜치에 누적됨. 특히 로그인 제거, 로컬 데이터, 정비주기·알림, 사진, GPS, GPX, 백업 및 기존 코드 보호.
- **고정 서명 개인키는 GitHub 공개 저장소에 업로드하지 말 것.** 고정 서명 SHA-256: `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`.

## 가장 최근 완료
1. UX18: 정비 기록 등록·완료 동률 선택, 알림 OFF/ON 중복 방지 초기화 — PR #16 병합, 전체 CI 성공.
2. UX19: 가장 최근 유효한 주유 기록만 불러오기, 입력 금액/리터 기반 단가 표시, 만땅 주유 시 계기판 표시.
3. UX19: 사진 선택 작업과 미리보기 다운로드 분리, 중복 선택 방지, 오래된 비동기 이미지 폐기, 메모리 정리, Wikimedia 요청 식별 정보.
4. UX19: 신규 회귀 테스트 및 기존 BZ 검사 업데이트 — PR #18 병합, 성공한 CI: https://github.com/mercurial0416-lgtm/moto/actions/runs/39755731137 (Java, Android build, Lint, unit tests).
5. GitHub `main`의 고정 서명 빌드 workflow가 최신 통합 브랜치 소스를 체크아웃하도록 PR #17 병합. 서명키가 구성돼야 고정 서명 빌드 가능.

## 확인되지 않은 항목
- Galaxy S25/Android 16 실기기 사진 검색 성공, 사진 미리보기, 갤러리 교체, GPS 장시간 주행, 푸시 알림 시간 지연.
- 현재 사용자가 설치한 APK의 서명과 고정 서명키가 같은지 여부.
- Actions debug APK는 **고정 서명 업데이트 APK가 아님**. 기존 설치본을 지우거나 덮어쓰지 말 것.
- 전체 세계 바이크/연식 사진은 공개 이미지 제공자와 실제 매칭 이미지 존재 여부에 좌우됨. 100% 보장하지 말 것.

## 다음 수정 순서
1. 실기기 갤러리 HTTP 오류·다운로드 상태를 개인정보 노출 없이 점검하고 결과에 따라 추가 수정.
2. 기존 설치본 signing fingerprint 비교 → 맞는 release keystore 확보 → `main`의 Fixed-signature MOTO HUB update APK 실행 → 해시/서명/versionCode 확인.
3. signed APK 설치 전 데이터 ZIP 백업·복원 테스트, 실제 설치/업데이트 검증.
4. 그 후 GPS 서비스, 배터리 제한, 모델 연식 사진, UI 스크롤·접근성 보완.
5. 기능 하나마다 GitHub 커밋과 CI로 검증; 기존 소스/기능 재작성 금지.

## 다른 대화에서 이어갈 때
`mercurial0416-lgtm/moto`의 `build/di518-ux4-prebuild-20261020` 브랜치에서 `CONTINUE_HERE_KO.md`와 `MOTO-HUB/docs/CHECKPOINT_ALPHA6_DI518_UX19_KO.md`를 읽어. UX19까지 통합돼 있고 APK debug CI는 성공했지만 실기기 사진 로딩과 고정 서명 업데이트는 미검증이야. 최신 기능을 보존한 채 이어서 수정하고, 각 단계마다 커밋·검증해.
