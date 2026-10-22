# Alpha6 DI518 UX21 — 통합 확인
- 소스 커밋: `fdfebbaee7cb0dd8793e413dc310044b99c6b9fe`
- 테스트: [UX20 success](https://github.com/mercurial0416-lgtm/moto/actions/runs/39759995384), [UX21 success](https://github.com/mercurial0416-lgtm/moto/actions/runs/39760731453) (Java regressions, Android assembleDebug, lintDebug, unit test, checksum/signature).
- **UX20:** `FuelAutoAmount`가 자동으로 산출한 금액의 유효성 추적; 입력 무효 시 과거 자동 계산값 비우기(수동 결제 금액 보호). `DailyMaintenanceJob` 중단 시 작업자 interrupt, 완료 콜백 중복 회피, 스냅샷 직렬화. 고정 서명 릴리스 versionCode 자동 상향 계산기와 회귀 검사.
- **UX21:** 지난 주유 불러오기 전에 단가 계산 화면을 닫고 자동액 상태를 재설정, 가득 주유의 계기판 입력 노출 유지.
- **main release:** 공개 manifest 최신 코드 비교, 고정 인증서 체크, APK 버전/패키지/해시 검사, 정확한 source ZIP 보관, 명시적 opt-in 배포 [#20](https://github.com/mercurial0416-lgtm/moto/pull/20).
- 기존 설치 APK 서명 호환성 미확인. 개인 keystore secret 없이는 실서명 릴리스 실행하지 말 것.
- 실제 Android 단말/위치 백그라운드/온라인 사진/설치 데이터 보존 검증은 미실행.
