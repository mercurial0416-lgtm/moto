# MOTO HUB 고정 서명 운영 규칙

**목표:** 한 번 설치한 MOTO HUB가 이후 업데이트를 앱 삭제 없이 받을 수 있도록 동일한 서명 개인키를 계속 사용합니다.

- 새 고정 인증서 SHA-256: `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`
- 패키지 이름: `com.motohub.next`; 신규 APK는 `versionCode`를 증가시켜야 합니다.
- 기존 Alpha6-DH(517) **디버그 APK**는 임시 서명키로 만들어져 이 인증서와 다릅니다. 기존 설치 데이터의 백업 없이 삭제하지 마세요. 새 고정 키로 이전 설치본을 덮어쓸 수는 없습니다.
- 개인키와 비밀번호는 **공개 GitHub 저장소에 절대 추가하지 않습니다.** 별도 비공개 `ChatGPT Library /MOTO-HUB/private-signing`에 저장한 파일을 보존해야 합니다.

## GitHub Actions 릴리스 자동서명 설정 (1회)

GitHub 저장소 Settings → Secrets and variables → Actions → New repository secret에서 아래 네 항목을 설정해야 합니다. 저장소에 등록되는 값은 로그·소스에 나타나지 않아야 합니다.

- `MOTO_SIGNING_STORE_B64` : `MOTO-HUB-FIXED-v1.p12` 파일을 base64로 인코딩한 내용
- `MOTO_SIGNING_STORE_PASSWORD` : 같은 비밀 폴더의 `key-password.txt` 내용
- `MOTO_SIGNING_KEY_ALIAS` : `moto-hub`
- `MOTO_SIGNING_KEY_PASSWORD` : 동일한 `key-password.txt` 내용

인증된 `gh` CLI가 있는 환경에서는 개인 폴더에 제공한 `register-github-secrets.sh`를 **로컬에서 한 번** 실행할 수 있습니다. 공개 저장소에는 키를 업로드하면 안 됩니다.

등록 후 GitHub Actions의 `Fixed-signature MOTO HUB update APK` 워크플로를 직접 실행합니다. 이 워크플로는 비밀키가 없거나 서명 인증서가 위 해시와 다르면 APK 배포를 **실패 처리합니다**. 다른 키로 임의 서명한 디버그 APK는 업데이트용으로 사용하지 않습니다.

## 키 보관

`MOTO-HUB-FIXED-v1.p12` + 비밀번호를 서로 다른 안전한 장소에 보관하십시오. 둘 중 하나를 잃으면 이후 동일 서명 APK 생성이 불가능합니다. 사용자가 GitHub Secrets 등록 전이라도, 모델의 실행 환경에서 **비공개 Library 키를 명시적으로 복구해** 고정키로 서명할 수 있습니다.
