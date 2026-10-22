# MOTO HUB · Android Alpha6 DI518 UX19

현재 **실제 Android 소스**는 이 브랜치의 [MOTO-HUB/](MOTO-HUB/)에 있습니다.
루트의 AB/AH/AQ ZIP은 과거 보관본입니다. 최신 소스를 이 ZIP으로 덮어쓰지 마세요.

- 가장 최신 통합 브랜치: `build/di518-ux4-prebuild-20261020`
- 이어서 작업할 때: [CONTINUE_HERE_KO.md](CONTINUE_HERE_KO.md)
- 최신 주요 변경: [UX19 체크포인트](MOTO-HUB/docs/CHECKPOINT_ALPHA6_DI518_UX19_KO.md)
- 빠른 주유, 정비 알림, 갤러리, 주행기록, GPX, 사진, 백업, 로컬 앱 기능은 같은 프로젝트에 유지됩니다.
- [성공한 UX19 APK·Lint·Unit Test CI](https://github.com/mercurial0416-lgtm/moto/actions/runs/39755731137)

**중요:** CI의 `assembleDebug` APK는 기존 앱의 서명과 호환된다는 보장이 없습니다.
설치본을 지우지 마세요. 데이터 백업 없이 재설치하거나 임의 서명 APK로 교체하지 마세요.
고정 서명 APK는 비공개 키가 있는 [수동 Release Workflow](https://github.com/mercurial0416-lgtm/moto/actions/workflows/moto-fixed-signed.yml)에서 별도 인증서 검증 후 생성해야 합니다.

현재 코드와 APK 빌드 통과 사실은 실제 휴대폰의 갤러리 통신, 권한, 사진 로드, GPS 및 업데이트 설치 성공을 의미하지 않습니다.
