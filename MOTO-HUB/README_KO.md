> 최신 소스 체크포인트: **4.0.0-alpha6-ag (versionCode 438)**. 바이크 사진 첫 후보 다운로드 실패 시 최대 3개씩 재시도하고 동일 모델 참고 사진으로 대체 검색. `CHANGELOG-alpha6-ag.md` 참고. Android APK 빌드 및 실기기 검증은 미완료.

> 최신 소스 체크포인트: **4.0.0-alpha6-ab (versionCode 433)**. 라이딩 기록 날짜·시간·상태 검색 추가. `docs/CHECKPOINT_ALPHA6_AB_KO.md` 참고. APK 빌드 및 실기기 검증은 미완료.

> 최신 소스 체크포인트: **4.0.0-alpha6-aa (versionCode 432)**. 라이딩 기록 상태 필터도 앱 재실행 후 유지. `docs/CHECKPOINT_ALPHA6_AA_KO.md` 참고. APK는 아직 빌드되지 않았습니다.

# MOTO HUB 4.0 Alpha6-M

정식 네이티브 Android 앱 소스. **APK는 이 환경에서 빌드하지 못함**.

Alpha6-M: 연식/모델 사진을 빠르게 변경할 때 이전 비동기 조회 결과가 새 선택을 덮어쓰던 경합을 수정했습니다. 세부 검증은 `docs/CHECKPOINT_ALPHA6_M_KO.md`.

### 1. UI → 2. 편의성 → 3. 필요성
이번 체크포인트는 K의 MY GARAGE, GPS 요약, 슈퍼커브·모델별 이미지, 백업 UI를 유지하고, ZIP 일괄 복원을 설정에 추가했습니다.

설정 → 백업 · 복원 → **전체 ZIP 백업 복원**에서 백업 ZIP 하나로 JSON 데이터, 등록 사진, GPS 원본·상태파일을 복원할 수 있습니다. 복원 전에 화면에서 항목 수를 확인하고 명시적으로 승인합니다. ZIP과 JSON·GPS·JPG 형식을 검증하며, 파일이 손상되거나 제한을 초과한 경우 기존 파일을 유지합니다. 복원은 기존 데이터 교체입니다. 복원 전 기존 ZIP을 별도로 내보내세요.

**미검증:** Galaxy 설치, Android SDK 빌드, GPS 백그라운드/실시간 공유 두 기기 연동. 자동 매물 수집, 내비게이션, 푸시 알림, 앱 업데이트 서버도 구현 완료가 아닙니다.

### 빌드
GitHub Actions `.github/workflows/android-debug.yml` 또는 Android SDK 36 설치 환경에서 `./gradlew :app:assembleDebug`.

### 로컬 테스트
`./tests/run-restore-tests.sh` · `./tests/run-ride-tests.sh` · `./tests/run-backup-tests.sh` · `./tests/run-offline-tests.sh` · `./tests/run-server-settings-tests.sh` · `./tests/run-quick-entry-tests.sh` · `./tests/run-bike-visual-tests.sh` · `./tests/run-photo-request-tests.sh`

# 최신 개발 체크포인트: Alpha6-K

실제 수정 기능과 검증 범위는 `docs/CHECKPOINT_ALPHA6_K_KO.md`를 확인하세요. Android APK는 아직 빌드/실행 검증을 받지 않았습니다.

# MOTO HUB 4.0 Alpha6-I

> 제품 우선순위: **1. UI / 2. 편의성 / 3. 필요성**

## 포함된 결과물
- Android 네이티브 원본 프로젝트 (`app/`): 갤럭시용 정비, 주유, 매물, GPS/GPX, 친구·그룹, 위치 공유, JSON 백업/복원 코드를 포함합니다.
- `docs/previews/alpha6-i-browser.html`: **별도 브라우저판**, 정비·주유·매물·GPS(전경, 위치 권한/보안 컨텍스트 필요), 백업/복원 및 MY GARAGE UI.
- `.github/workflows/android-debug.yml`: Android SDK 36 설치 뒤 Debug APK 빌드/업로드용 워크플로.

## Alpha6-I 주요 변경
- 슈퍼커브 110 모델 자동 감지 → **슈퍼커브 참고 아트** 표시. Duke 이미지를 기본으로 쓰던 기존 이미지 파일 삭제.
- 슈퍼커브 외 차종은 중립적인 바이크 이미지 표시. 다른 차종을 슈퍼커브로 오표시하지 않음.
- 바이크 사진은 기기에서 직접 선택해 덮어쓸 수 있음. 실제 소유 차량 사진이 업로드되는 것은 아님.
- 신규 설치로 차고가 비어 있으면 슈퍼커브 110(2024) 이름·연식을 입력 폼에 제안. 주행거리는 **임의 생성하지 않음**.
- 기존 모든 네이티브 탭·동작 연결 유지. 브라우저판도 기존 제한된 개인 GPS 시작 버튼을 연결(브라우저/권한 지원 때만 동작).
- Android Gradle 버전명 `4.0.0-alpha6-i`, versionCode `414`.

## 빌드 (Android SDK 36 + JDK 17 + Gradle 8.11.1)
```bash
./gradlew :app:assembleDebug
```
성공 시 `app/build/outputs/apk/debug/app-debug.apk`를 생성합니다. GitHub Actions에서는 워크플로를 통해 artifact를 확인할 수 있습니다.

**중요: 이 작업 실행 환경에서는 Android SDK와 외부 다운로드를 이용할 수 없어서 Android APK를 실제 빌드/설치 테스트하지 못했습니다.** 브라우저판이 곧 Android APK는 아닙니다. 이전에 제공한 수작업 UI Preview APK는 실행 오류가 확인돼 사용을 권장하지 않습니다.

## 친구 서버
- MOTO HUB Supabase 프로젝트는 별도 구성되어 있으며 Gradle `supabaseUrl`/`supabaseKey`는 공개용 키입니다. 프로젝트 admin/service role 키를 앱에 넣지 마세요.
- 그룹 위치 공유는 사용자가 그룹 참여·공유 시작을 선택한 뒤에만 동작하도록 설계됨. 공개 브라우저판에서는 친구 실시간 GPS/백그라운드 GPS를 지원하지 않습니다.
- 백그라운드 위치 공유는 실제 기기 설치 후 권한·알림·배터리 설정 테스트가 필요합니다.

## 확인
- 오프라인/정비·유지비 19항목, 서버 입력 검증 16항목, 빠른 입력 12항목, 차종 자동 매핑 12항목 통과.
- Java 23파일 구문 파싱 통과.
- Chromium DOM 로딩, 슈퍼커브 홈, 사진 버튼, 주유/정비 폼, 매물 이동, GPS 시작 버튼 노출 점검 7항목 통과.
- APK 컴파일/실기기·서버 E2E는 **미실행**.
