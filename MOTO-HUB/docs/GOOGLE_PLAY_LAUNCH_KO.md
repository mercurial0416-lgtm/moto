# MOTO HUB — Google Play 배포 준비 (UX49, v521)

## 현재 상태
- 앱 패키지: `com.motohub.next`. 기존 기기에 저장된 GPS·정비·주유 데이터는 APK를 삭제하면 손실될 수 있으므로 **삭제하지 말 것**.
- 앱 실행 내비: 카카오내비, 네이버지도(내비게이션 포함), 티맵. 앱 실행만 수행하며 GPS/GPX/ECU 데이터는 자동으로 공유하지 않음.
- `playRelease` 전용 Android App Bundle(AAB)과 `STORE_DISTRIBUTION=true` 업데이트 화면 준비.
- Google Play 전용 빌드에는 `REQUEST_INSTALL_PACKAGES` 권한이 없어야 함. 스토어 업데이트는 Play에 위임.
- 이 소스의 Play Console 등록·개발자 본인확인·심사·배포 완료는 아직 수행되지 않음.

## Google Play Console 사전 준비 (개발자 계정 소유자 필요)
1. [Google Play Console](https://play.google.com/console/)에서 검증된 개발자 계정을 사용해 `com.motohub.next` 앱을 등록한다. 패키지 이름 등록·소유권이 다른 프로젝트에 선점되어 있지 않은지 확인한다.
2. Play 앱 서명을 설정한다. **중요:** 현재 설치한 MOTO HUB 앱에 적용된 고정 서명 인증서 SHA-256:
   `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`.
   Play App Signing에서 Google이 새 앱 서명키를 생성하면 이 인증서와 달라 기존 사이드로드 앱에 덮어쓰기 업데이트할 수 없다.
   기존 데이터를 그대로 지키려면 게시 **전** 앱 서명키로 기존 키를 사용할 수 있는지 Play Console 절차를 검토한다. 업로드 키와 앱 서명키는 별개이며, 이미 배포된 앱과 서명 일치 여부를 실제 기기에서 검증한다. 개인키를 Git 저장소나 대화에 올리지 않는다.
3. 앱 콘텐츠: 개인정보처리방침 공개 URL·개발자 연락처, 데이터 보안 양식, 정확한 위치 접근 목적, 백그라운드 GPS/알림/블루투스 권한, 그룹 위치 공유 선택 동의, 콘텐츠 등급, 스토어 아이콘, 스크린샷 등을 실제 동작에 맞게 작성한다.
4. **내부 테스트** 트랙을 먼저 만든다. Google Play Console은 앱을 생성·구성하고 서비스 계정에 해당 앱의 릴리스 관리 권한을 명시적으로 부여해야 API 자동 등록이 가능하다.
5. 신규 개인 개발자 계정(2023-11-13 이후)은 프로덕션 신청 전 12명 이상이 14일 연속 참여하는 비공개 테스트 및 제작자 신청·검토가 필요할 수 있다.
6. 내부 테스트가 성공한 뒤 콘텐츠 정책·서명 호환성·설치 및 로컬 기록 데이터 보존 실기기 테스트를 통과한 경우에만 공개 배포를 신청한다.

## AAB 생성
Android SDK 36·JDK 17 설치, 저장소 루트 `MOTO-HUB/`에서:
```bash
./gradlew :app:bundlePlayRelease :app:assemblePlayRelease \
  -PmotoReleaseCode=521 --no-daemon
```
- 산출물 `app/build/outputs/bundle/playRelease/app-play-release.aab`.
- `MOTO_SIGNING_*` 환경변수가 없으면 AAB는 **서명되지 않은 업로드용 빌드**다. 이 상태로 스토어에 업로드하지 말 것.
- 서명 환경 변수/Play 서비스 계정이 없는 상태로 앱 공개 배포가 완료됐다고 안내하지 않는다.
- 별도 일반 `release` 변형은 기존 서명된 APK 업데이트 호환 경로를 유지한다.

## 앱 내부 테스트 전용 배포 자동화
`.github/workflows/moto-play-internal.yml` 수동 워크플로:
- `BUILD_ONLY`: 코드·AAB 검증만 수행하며 실제 업로드하지 않음.
- `PUBLISH_INTERNAL_ONLY`: 명시적으로 선택, 모든 서명 시크릿·기대 인증서 지문·`MOTO_PLAY_SERVICE_ACCOUNT_JSON` 존재·내용 검증, 버전 521 AAB 생성, 인증서 확인 후 Play Developer Publishing API에 내부 테스트 트랙만 게시.
- 워크플로는 production/open/closed 게시를 **지원하지 않음**. `tools/publish_play_internal.py`를 통해 업로드 후 검증·편집사항 커밋하며, 오류시 편집을 취소한다.
- 필요한 GitHub Actions 시크릿: `MOTO_SIGNING_STORE_B64`, `MOTO_SIGNING_STORE_PASSWORD`, `MOTO_SIGNING_KEY_ALIAS`, `MOTO_SIGNING_KEY_PASSWORD`, `MOTO_EXPECTED_CERT_SHA256`, `MOTO_PLAY_SERVICE_ACCOUNT_JSON`.
- 서비스 계정의 권한은 이 앱의 필요한 릴리스 권한으로 최소화하고, 비밀키는 저장소나 아티팩트로 업로드하지 않는다.

## 스토어 등록정보 초안
앱 이름: MOTO HUB

짧은 설명: 바이크 정비·주유·GPS 주행기록을 한곳에 관리하는 라이딩 앱

상세 설명:
MOTO HUB는 바이크의 정비와 주유, 누적 주행거리, GPS 라이딩 기록과 즐겨찾기를 한곳에서 관리하는 Android 앱입니다.
원하는 바이크를 등록하고 정비주기를 설정해 다음 점검 시점을 확인할 수 있습니다.
GPS 주행기록은 사용자가 직접 시작하거나 실험적 OBD 자동 감지를 직접 켠 경우에 기록되며, 기록·복구·GPX 내보내기 기능을 제공합니다.
카카오내비·네이버지도·티맵 중 원하는 앱을 선택해 실행할 수 있습니다. MOTO HUB가 내비게이션 경로를 생성하거나 오토바이 전용 길찾기를 보장하는 것은 아닙니다.
친구 그룹에서의 실시간 위치 공유는 별도의 명시적 동의가 필요하며 언제든 중지할 수 있습니다. 기능에 따라 인터넷·위치·Bluetooth 권한이 필요합니다.

**출시 전 검증 필요:** 실제 Play Store 등록정보, 개인정보처리방침과 그룹 서버 데이터 보존·삭제 정책, 고객지원 연락처, 라이딩 중 화면 조작 경고 및 스크린샷.
