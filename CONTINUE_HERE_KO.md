# MOTO HUB 개발 재개 · 2026-10-22 · UX23

## 정확한 현재 소스
- 저장소: https://github.com/mercurial0416-lgtm/moto
- 브랜치: `build/di518-ux4-prebuild-20261020`
- Android 소스: `MOTO-HUB/`
- 최신 기능 통합 커밋: `39614f143395856bf5004fe231fa935496c10f76` (UX23)
- 패키지: `com.motohub.next`; 일반 debug `versionCode 518`
- `main`은 릴리스 manifest·워크플로·역사적 ZIP 중심. **루트 ZIP에서 앱을 다시 만들지 말 것.**

## 이번 검증 완료
1. UX22: 전체 ZIP 백업의 심볼릭 링크 경로 차단, 라이딩 부속 파일 보호, 이미지 네트워크/손상 실패 구분, 오래된 복원 staging 폴더의 영수증 사진 정리. PR #22, Android/Java 전체 CI 성공: https://github.com/mercurial0416-lgtm/moto/actions/runs/39770735252
2. UX23: 온라인 Commons 갤러리에서 사용자가 직접 고른 모델 JPEG와 출처 메타데이터를 전체 ZIP에 백업. 자동 다운로드 캐시는 제외. 복원에 출처/이미지 쌍·JPEG 검사·용량 검증 추가.
3. UX23: 복원 실패 시 이전 선택 사진과 출처 롤백. 구형 ZIP 복원은 현재 선택한 모델 사진을 삭제하지 않음. 심볼릭 링크된 모델 사진 폴더 복원 차단. 중단된 복원 폴더에 모델 사진이 있더라도 안전한 파일만 정리.
4. UX23 최종 소스의 순수 Java 회귀, Android assembleDebug, lintDebug, testDebugUnitTest, signature/checksum CI 성공: https://github.com/mercurial0416-lgtm/moto/actions/runs/39772847415

## 미완료, 별도 검증 필요
- 갤럭시/Android 16 실기기의 온라인 사진 호출/실제 이미지 선택, 장시간 GPS 백그라운드, 대량 기록 UI, 정비 알림 권한/시간.
- 사용자가 현재 설치한 앱과 고정 서명 인증서의 일치 여부. `main/.github/workflows/moto-fixed-signed.yml`에 비공개 서명 secrets가 구성됐는지는 확인하지 못했고 **실제 고정 서명 APK를 만들지 않았음**.
- 진행 중 프로세스가 강제 종료된 순간의 복원 트랜잭션에 대해 앱 재시작 후 자동 롤백하는 별도 영속 저널은 아직 구현/검증되지 않았음. 현재 예외에 따른 롤백과 오래된 stage 정리는 있음.
- 종단 간 UI 테스트 및 단말의 저사양/대량 데이터 성능 검증.

## 이어서 작업하는 절차
1. 이 브랜치 HEAD에서 코드를 직접 읽고 이전 기능을 보존한다.
2. 작은 기능 단위로 수정, GitHub 커밋, Java 회귀 및 Android CI를 확인하고 병합한다.
3. 실기기 이전에는 **현재 앱을 삭제하거나 debug APK로 업데이트할 것을 지시하지 않는다.**
4. 배포 시 고정 서명 키를 공개 저장소에 올리지 말고 인증서 SHA-256 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`와 현재 설치 앱 인증서를 먼저 확인한다.

다른 대화에선 이 문서와 `MOTO-HUB/docs/CHECKPOINT_ALPHA6_DI518_UX23_KO.md`를 읽고 UX24부터 이어서 수정해.
