# 현재 코드에서 확인한 핵심 사실

- 원본 제공 패키지: MOTO-HUB-2.0-package.zip
- 기존 앱 HTML 소스 경로: `legacy/current_app/index.html`
- 기존 앱은 Android Gradle 프로젝트가 아닌 직접 만든 최소 WebView APK입니다.
- 패키지 ID: `com.motohub.app` (빌더 소스에 표시된 값)
- app versionCode: 2, versionName: 2.0 (빌더 소스에 표시된 값)
- localStorage key: `motohub_personal_v1`
- 초기 데이터는 사용자 데이터가 없을 때 데모 항목을 생성합니다. 그 값을 실제 바이크 정보로 오인하지 마세요.
- HTML에는 주유/정비/매물/GPX 등 브라우저 API 기반 기능이 있지만 Android WebView 실제 지원 여부는 별도 확인해야 합니다.
- 현재 APK에는 네이티브 foreground-service 기반 실시간 위치 공유 및 친구 로그인 시스템이 없습니다.
- 기존 ZIP의 `build_v2.py`에는 개발용 고정 비밀번호로 키를 만드는 예제가 있으므로, 이 패키지에는 키 생성/서명 단계를 **의도적으로 제거**했습니다. 앱 릴리스 서명 체계를 새로 안전하게 관리해야 합니다.
- 사용자 기록 보존을 위해 기존 자료의 JSON 백업/복원 및 마이그레이션을 테스트하세요.
- 사용자는 **갤럭시 모바일에서 쉽고 보기 편한 UI**를 최우선으로 요구하며, 이전 V3/V4/V5 레이아웃에 불만이 있었습니다. 단순 스킨 변경 금지.
