# MOTO HUB 개발 재개 · 2026-10-23 · UX36

## 정확한 현재 소스
- 저장소: https://github.com/mercurial0416-lgtm/moto
- 브랜치: `build/di518-ux4-prebuild-20261020`
- Android 소스: `MOTO-HUB/`
- 최신 기능 통합 커밋: `e0df7daf88bc1fa3a718bb7a41b3c1770c82e352` (UX36)
- 패키지: `com.motohub.next`; 일반 debug `versionCode 518`
- `main`은 릴리스 manifest·워크플로·역사적 ZIP 중심. **루트 ZIP에서 앱을 다시 만들지 말 것.**

## 이번 검증 완료
1. UX22: 전체 ZIP 백업의 심볼릭 링크 경로 차단, 라이딩 부속 파일 보호, 이미지 네트워크/손상 실패 구분, 오래된 복원 staging 폴더의 영수증 사진 정리. PR #22, Android/Java 전체 CI 성공: https://github.com/mercurial0416-lgtm/moto/actions/runs/39770735252
2. UX23: 온라인 Commons 갤러리에서 사용자가 직접 고른 모델 JPEG와 출처 메타데이터를 전체 ZIP에 백업. 자동 다운로드 캐시는 제외. 복원에 출처/이미지 쌍·JPEG 검사·용량 검증 추가.
3. UX23: 복원 실패 시 이전 선택 사진과 출처 롤백. 구형 ZIP 복원은 현재 선택한 모델 사진을 삭제하지 않음. 심볼릭 링크된 모델 사진 폴더 복원 차단. 중단된 복원 폴더에 모델 사진이 있더라도 안전한 파일만 정리.
4. UX23 최종 소스의 순수 Java 회귀, Android assembleDebug, lintDebug, testDebugUnitTest, signature/checksum CI 성공: https://github.com/mercurial0416-lgtm/moto/actions/runs/39772847415

## UX24: 이번 작업에서 추가 완료
- 표준 ZIP 도구의 명시적 `model-photos/` 디렉터리 항목을 복원에서 정상 수락하도록 공통 `BackupFormat` 허용 목록을 사용.
- 바이크/영수증/직접 고른 온라인 모델 사진이 손상된 JPEG 헤더·종료표시를 가지면 ZIP 내보내기 과정에서 즉시 실패 처리. 잘못된 파일을 성공한 백업으로 오인하지 않도록 회귀 테스트 추가.
- [PR #24 병합](https://github.com/mercurial0416-lgtm/moto/pull/24) · [최종 Java/Android/Lint/unit 빌드 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39774524003)
- **빠른 개발 원칙**: [한 묶음-한 빌드·우선순위 목록](MOTO-HUB/docs/DEVELOPMENT_RULES_AND_QUEUE_UX24_KO.md). 사용자가 3시간씩 기다리지 않도록 새로운 이슈를 끝없이 작업에 추가하지 않고 완료 시 종료.

## UX25·UX26 추가 완료
- UX25: 온라인 갤러리 수동 선택 메타데이터를 엄격한 JSON 최상위 boolean으로 판별. 잘못된 JSON과 중복 키 거부. PR #25, CI 성공: https://github.com/mercurial0416-lgtm/moto/actions/runs/39779789959
- UX26: **주유 간편 입력**에서 계기판 사진 선택 → 기존 기기 내 OCR 모델로 주행거리 후보 인식 → 사용자가 선택했을 때만 입력칸 변경(자동 저장 없음).
- UX26: 완료된 라이딩 기록에 별 즐겨찾기. `.favorite` 마커를 전체 ZIP 백업·검증·복원·임시 폴더 정리까지 일관되게 처리.
- PR #26 병합: https://github.com/mercurial0416-lgtm/moto/pull/26
- UX26 순수 Java 전체 회귀, assembleDebug, lintDebug, testDebugUnitTest, 서명·SHA256 검증 성공: https://github.com/mercurial0416-lgtm/moto/actions/runs/39782312508
- 추출된 테스트 APK: `MOTO-HUB-UX26-DEBUG-TEST.apk` (48,355,294 bytes), SHA256 `0c460124e45cf3792baad3b5cc80dc6e7b08d66855d4e614d87d1c762182c249`.
- **서명 주의:** 이번 APK는 Android Debug 인증서로 서명된 테스트본. 기존 고정 서명 앱에 덮어쓰기 불가. 현재 설치 앱 삭제 금지. 진짜 업데이트 APK는 비공개 고정 서명 인증서 검증이 선행되어야 함.
- 유지비 6개월 바이크별 대시보드·계정 없는 로컬 저장은 **기존 소스에 이미 구현**, 이번에는 재작성하지 않음.

## UX27 ~ UX29 최신 완료 (2026-10-23)
- UX27: 신규 바이크/매물 등록의 '슈퍼커브 110', '2024' 고정 입력 예시를 제거; 실제로 저장된 값과 카탈로그 연식 선택은 유지. [PR #27](https://github.com/mercurial0416-lgtm/moto/pull/27), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39790863477).
- UX28: 라이딩 즐겨찾기를 별도 큰 버튼 대신 기존 기록 행 내부 작은 별 버튼으로 변경; 주유 기록에서 계기판 카메라 즉시 촬영 → 온디바이스 OCR → 수동 확인 지원; 전체 ZIP 복원 후 부가 체크포인트 실패와 실제 복원 커밋 분리. [PR #28](https://github.com/mercurial0416-lgtm/moto/pull/28), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39799730195).
- UX29: 전체 ZIP 복원 중 프로세스 종료 시 안전 저널(`moto-rollback-UUID`)에서 기존 GPS/사진 및 JSON 복구. 사전 스냅샷+pending/committed 표식, 시작 시 DB 읽기 이전에 원복, 중간 오류·악성 심볼릭 링크 차단 및 저장 보호. [PR #29](https://github.com/mercurial0416-lgtm/moto/pull/29), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39809333668).
- 최신 테스트 APK: `MOTO-HUB-UX29-DEBUG-TEST.apk`, SHA-256 `954affcbb8f236ef7f13dbe6adfe6b9956f9f93d26d9ef72a49f22d56cc61cef`. **Android Debug 서명 인증서**. 기존 고정 서명 앱 위에 업데이트 설치 불가; 앱 삭제 금지. 고정 서명 인증서 SHA-256은 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`.
- 이번 UX29에서 구현한 건 **프로세스 강제 종료를 가정한 순수 Java 회귀**이며 전원 차단/파일시스템 손상/Android 16 실기기 복구는 검증되지 않았음.

## UX30 ~ UX33 추가 완료 (2026-10-23)
- UX30: 정비 반복 알림(사용자 동의 기반·주 단위 억제) 및 모델 사진 캐시 정리 보호. 통합 커밋 `ddbb65a63017521ab5b10fb4fc474fa3f265484c`.
- UX31: 재시작 복원 저널에서 삭제 전에 모든 새로운 파일 경로의 안전을 확인. [PR #31](https://github.com/mercurial0416-lgtm/moto/pull/31), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39890795302).
- UX32: JSON 단독 백업 복원은 JSON의 원자적 저장을 먼저 완료하고 체크포인트/위젯 새로고침을 후처리로 분리. 저널의 늦은 심볼릭 링크 탐지 테스트 추가. [PR #32](https://github.com/mercurial0416-lgtm/moto/pull/32), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39899213448).
- UX33: 주유 자동 계산값을 사용자가 수동 수정하면 이후 리터·단가 입력이 직접 수정 금액을 덮어쓰지 않음(금액 지우기 시 자동 계산 재개). ZIP 복원 임시 폴더 삭제 시 심볼릭 링크를 재귀로 따라가지 않음. 위젯의 데이터베이스 읽기는 `AtomicFile.readFully()` 이용. [PR #34](https://github.com/mercurial0416-lgtm/moto/pull/34), [전체 CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39901211779).
- UX33 실제 테스트용 APK: `MOTO-HUB-UX33-DEBUG-TEST.apk`, 48,371,678 bytes, SHA-256 `60e9f7d6914927bb3b7e64b7fbe22b1969beaeca74c5af0e1e6e6e36eaf17cb0`. 단위 테스트 69개, 실패 0개, 건너뜀 2개. debug v2 서명 인증서 `5b01da503ada11c205408644453db6c9a0fecd67ee0ff92c8c80b781d49b6216`, 기존 고정 서명 인증서 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`와 다름. **기존 설치 앱 삭제 금지.**
- 정정: 이 시점의 `main/.github/workflows/moto-fixed-signed.yml`은 별도 개발 브랜치를 안전하게 체크아웃하도록 이미 수정됨. 소스 부재가 직접 빌드 장애라는 판단은 더 이상 해당하지 않음. signing secrets는 확인 불가, 정식 Release 검증 별도.

## UX34~UX35 추가 완료 (2026-10-23)
- UX33 사진 보완도 이전 체크포인트 직후 통합됨: 유효한 자동 사진 로딩 대기 시간 제한, 사용자 직접 선택 사진 우선 적용. 통합 커밋 `06a55c05`.
- UX34: 정비 알림을 바이크마다 모든 서비스 레코드를 재검색하지 않고 한 번 바이크별로 분류하여 대량 기록 성능 개선. 정비 예정일·예정주행거리·알림 기준 변경은 알림 식별값에 반영하고, 실제 주행거리 소폭 변경은 반복 알림으로 취급하지 않음. 7,500건 규모 회귀 + 전체 Android CI 통과. [PR #35](https://github.com/mercurial0416-lgtm/moto/pull/35), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39952626466).
- UX35: OBD 자동 감지 중 실제 GPS 트랙을 기록할 때 GPS가 꺼지는 경우 자동 감지만 중지하고 기존 트랙 및 서비스를 보존. 기록이 시작되지 않은 OBD 대기 상태는 종료. 네 가지 정책 조합 회귀 + Android CI 성공. [PR #36](https://github.com/mercurial0416-lgtm/moto/pull/36), [CI 성공](https://github.com/mercurial0416-lgtm/moto/actions/runs/39953215930).
- UX35 테스트 APK: `MOTO-HUB-UX35-DEBUG-TEST.apk` (48,371,678 bytes), SHA-256 `9e6f04491123a63c2cea0ac03e7d6eb82a8e7e2e292156f38f50eb150c01f95d`, Android 단위 69개 중 실패 0·건너뜀 2, debug signer SHA-256 `e8d688d776e7e3133c44026307521f48553e4b62162a0b2bd5547f355cfe3f9f`. **기존 고정 서명 설치 앱은 삭제하지 말 것.**
- 정정: `main`의 실제 최신 `.github/workflows/moto-fixed-signed.yml`은 이미 `source/`에 최신 개발 브랜치 Android 소스를 따로 checkout 하며 버전 자동 증가, 고정 인증서 검증, opt-in 공개 manifest 갱신을 구현했다. 초기 체크포인트의 “main에 소스가 없어 정식 릴리스 불가” 설명은 현재 워크플로 기준으로는 잘못됨. 다만 비공개 signing secrets 존재 여부를 읽을 권한이 없어 검증하지 못했고, 정식 고정 서명 APK 발급·기존 설치본의 인증서 검증도 미완료.

## UX36 GPS 부가 기능 오류 분리 (2026-10-23)
- GPS 파일 쓰기·flush·fsync 성공 뒤 실행하는 그룹 위치 공유·알림 전송을 `RidePostCommit.afterWrite`로 격리. 공유 서비스 오류 시 공유를 중지하더라도 로컬 GPS 트랙은 종료하지 않음. 알림 전송 실패도 로컬 기록을 중단하지 않음. 실제 GPS 기록 파일 쓰기 실패만 중단.
- 순수 Java 예외 조합 회귀 테스트, 기존 전체 Java 회귀, Android assembleDebug/lintDebug/testDebugUnitTest 성공. [PR #37](https://github.com/mercurial0416-lgtm/moto/pull/37), [최종 CI](https://github.com/mercurial0416-lgtm/moto/actions/runs/39954458590).
- UX36 최신 테스트 APK `MOTO-HUB-UX36-DEBUG-TEST.apk`, 48,371,678 bytes, SHA-256 `0b6d39cae97e5715cc52f585fd1c96cd1696edafa228c05accb882a72fd4eb6d`, 단위 테스트 69건 중 실패 0·건너뜀 2. Android DEBUG 인증서 SHA-256 `05925dcc25e3e03ccae89c4630928b66cc10f1277fac92dadeac12eec1958500`. **고정 서명과 불일치; 기존 앱 삭제·업데이트 덮어쓰기 권고 금지.**
- 사용자가 요청한 다른 기능들은 재분석 시 이미 일부 구현됨(사진 조회 대기 제한 UX33, 대량 라이딩 비동기/페이징, 기존 로그인 없는 로컬 데이터, OBD 재연결, 정비·GPX·위젯). 실기기 검증 없이 “전체 완성”으로 오인시키지 말 것.

## 미완료, 별도 검증 필요
- 갤럭시/Android 16 실기기의 온라인 사진 호출/실제 이미지 선택, 장시간 GPS 백그라운드, 대량 기록 UI, 정비 알림 권한/시간.
- 사용자가 현재 설치한 앱과 고정 서명 인증서의 일치 여부. `main/.github/workflows/moto-fixed-signed.yml`에 비공개 서명 secrets가 구성됐는지는 확인하지 못했고 **실제 고정 서명 APK를 만들지 않았음**.
- 복원 영속 저널은 UX29에서 Java 회귀까지 구현·검증 완료. 갤럭시 Android 16 실기기 강제 종료·배터리 제거 수준 전원 차단 및 스토리지 부족 상황의 종단 간 검증은 여전히 필요.
- 종단 간 UI 테스트 및 단말의 저사양/대량 데이터 성능 검증.

## 이어서 작업하는 절차
1. 이 브랜치 HEAD에서 코드를 직접 읽고 이전 기능을 보존한다.
2. 증거 있는 수정사항을 한 묶음으로 모아 코드와 회귀 테스트를 작성하고, 최종 GitHub Android CI **한 번**으로 검증 후 병합한다. 실패했을 때만 추가 빌드한다.
3. 실기기 이전에는 **현재 앱을 삭제하거나 debug APK로 업데이트할 것을 지시하지 않는다.**
4. 배포 시 고정 서명 키를 공개 저장소에 올리지 말고 인증서 SHA-256 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`와 현재 설치 앱 인증서를 먼저 확인한다.

다른 대화에선 이 문서와 `MOTO-HUB/docs/DEVELOPMENT_RULES_AND_QUEUE_UX24_KO.md`를 읽고 UX37부터 이어서 수정해. UX35·36의 GPS 전환·부가 오류를 갤럭시 실기기에서 확인하고, 진짜 고정 서명 APK/사진/대량 UI 성능 문제를 우선한다.

## UX37 작업 진행 (2026-10-25 · 이번 대화)
- 브랜치: `fix/ux37-notification-failure-isolation-20261025`
- PR: https://github.com/mercurial0416-lgtm/moto/pull/38 (Android CI 검증 전 · **아직 병합하지 않음**)
- `RideService`의 위치 샘플/워치독/OBD/공유/프로바이더 상태 알림 갱신은 `RideNotificationSafety`로 격리. 알림 서비스 오류가 동기화된 로컬 GPS 기록을 종료하지 않도록 구현.
- 위치 공유 가능 여부 확인 시 권한·알림 서비스 예외는 `false`로 처리하여 공유 중단(개인정보 보호 방향).
- 추가 테스트: `bash MOTO-HUB/tests/run-ux37-notification-safe-tests.sh` / `RideNotificationSafetyTest`.
- 로컬 순수 Java 회귀 **7/7 통과**, GitHub의 `RideService` 호출 경로 정적 점검 **6/6 통과**. Android SDK·실기기가 없어 `assembleDebug`, `lintDebug`, `testDebugUnitTest`, 서명 검증은 **아직 미수행/미확인**.
- 차기 작업자는 PR #38 검토 및 Android CI 실행·성공 확인 후 원본 통합 브랜치에 병합하고, 고정 서명 인증서가 일치하는 릴리스 APK만 기존 앱에 덮어쓰기 허용할 것.
- 기존 설치 앱 삭제 금지. 일반 debug APK는 기존 설치본과 서명이 달라 덮어쓰기 불가.
