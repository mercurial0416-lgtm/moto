# MOTO HUB · Android Alpha6 DI518 UX21

**실제 최신 Android 소스:** [MOTO-HUB/](MOTO-HUB/) · 통합 브랜치 `build/di518-ux4-prebuild-20261020`.
과거 루트 AB/AH/AQ ZIP으로 앱 소스를 덮어쓰지 마세요.

- [작업 재개 가이드](CONTINUE_HERE_KO.md)
- [UX20+UX21 변경 및 검증](MOTO-HUB/docs/CHECKPOINT_ALPHA6_DI518_UX21_KO.md)
- [검증된 UX20 Android APK 빌드](https://github.com/mercurial0416-lgtm/moto/actions/runs/39759995384)
- [검증된 UX21 Android APK 빌드](https://github.com/mercurial0416-lgtm/moto/actions/runs/39760731453)
- 주유·정비·사진·GPS·GPX·백업·로컬 저장 기능을 기존 프로젝트에서 이어서 관리함.

**릴리스:** `main`의 [고정 서명 워크플로](https://github.com/mercurial0416-lgtm/moto/actions/workflows/moto-fixed-signed.yml)는 최신 소스를 가져와 공개 manifest보다 높은 버전을 선택하고, 고정 인증서를 검증한 뒤 APK와 같은 커밋의 소스 ZIP을 산출하도록 수정됐습니다. 그러나 실제 고정 서명 빌드는 개인키 비밀값이 필요하며 이번 작업에서 실행하지 않았습니다.

**설치 주의:** CI의 debug APK를 기존 설치본에 덮어쓸 수 있다고 보장하지 않습니다. 데이터 백업 없이 앱 삭제나 재설치를 하지 마세요.
