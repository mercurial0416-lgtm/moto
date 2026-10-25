# Alpha6 DI518 UX19 — 2026-10-22 검증 체크포인트

- 소스 통합: `build/di518-ux4-prebuild-20261020`, `f7b05136fefd38c3fc0b1085a5a480f3f68dfcb4`
- PR: [UX19 #18](https://github.com/mercurial0416-lgtm/moto/pull/18) · [고정 서명 빌드 경로 #17](https://github.com/mercurial0416-lgtm/moto/pull/17).
- CI: [39755731137](https://github.com/mercurial0416-lgtm/moto/actions/runs/39755731137) — Java 회귀, Android assembleDebug, Lint, 단위 테스트, APK checksum/signature step success.
- 변경: 최근 주유 기록 선택, 단가 실시간 표시, 가득 주유 계기판 표시, 미리보기/선택 이미지 분리, 빠른 재검색/닫기 자원 정리, 오래된 선택 응답 차단, CDN User-Agent.
- 회귀 테스트: `tests/RecentFuelRecordTest.java`, `tests/run-ux19-fuel-quick-tests.sh`, 갤러리/BZ 재검증.
- 고정 서명: `main/.github/workflows/moto-fixed-signed.yml`은 별도 체크아웃을 사용하도록 변경됐지만 민감한 개인키·비밀번호는 제공되지 않았고 해당 workflow 성공은 검증되지 않았음.
- **실기기와 이전 설치본 업데이트는 아직 미검증.** CI debug APK는 고정 서명과 동일하지 않을 수 있으니 사용 중인 앱을 삭제하지 않도록 안내.
