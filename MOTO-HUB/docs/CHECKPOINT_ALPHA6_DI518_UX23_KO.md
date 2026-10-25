# Alpha6 DI518 UX23 — 검증 체크포인트

- 최신 통합 소스: `39614f143395856bf5004fe231fa935496c10f76`
- UX22: [병합 PR #22](https://github.com/mercurial0416-lgtm/moto/pull/22) / [성공한 Actions](https://github.com/mercurial0416-lgtm/moto/actions/runs/39770735252)
- UX23: [병합 PR #23](https://github.com/mercurial0416-lgtm/moto/pull/23) / [성공한 Actions](https://github.com/mercurial0416-lgtm/moto/actions/runs/39772847415)
- UX22는 ZIP 백업 심볼릭 링크 읽기 차단, 이미지 실패 원인 분류, 중단된 복원 staging 내 유효 영수증 JPG 정리를 구현함.
- UX23은 `moto-year-photos/`에서 사용자 직접 선택(`userSelected:true`)한 JPG/JSON만 `model-photos/`에 묶어 내보내고, 검증·롤백·기존 ZIP 호환성·링크 폴더 차단을 추가함.
- 원본 바이크 사진 직접 업로드, 정비 영수증, GPS 트랙/GPX/라이딩 상태, 로컬 데이터는 기존 백업 로직을 유지함.
- CI 확인: Java 회귀, Android assembleDebug, lintDebug, 단위 테스트, APK 서명/해시 확인 성공. **실기기 설치 테스트 결과 아님.**
- 기존 설치본과 고정 인증서가 일치하는지 알 수 없음. Debug APK는 업데이트용 아님. 고정 서명 secrets에 접근하지 않았으며 실제 fixed-signed APK 생성은 수행하지 않음.
