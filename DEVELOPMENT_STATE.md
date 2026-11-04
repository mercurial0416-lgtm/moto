# MOTO HUB 개발 상태

- 최초 개발 기준: 2026-11-05 00:27:37 Asia/Seoul (2026-11-04T15:27:37Z, 개발 브랜치 생성 reflog).
- 작업 브랜치: `dev/autonomous-readiness-20261105`.
- 기준 소스: `design/commercial-ui-20261104` @ `cea29fe7757741946975cdccf3880135b3514011`.
- 원격 확인: `release/moto-hub-apk` @ `e2b6a256690788294794db5b301f87b72b809b5d` (v581); 위 디자인 소스는 해당 커밋을 포함하며 v583 UI 단순화 작업까지 포함.
- `main` @ `f3cbe70b48575369aea467779f9706130868976f`는 과거 ZIP·배포 메타데이터 중심. README의 UX23 안내는 현재 최신 상태가 아님. ZIP 덮어쓰기 금지.

## 이번 완료

1. JDK 17.0.17, Gradle 8.11.1, Android 36 / build-tools 36.0.0, org.json 20240303 및 TLS 프록시 신뢰 설정. 도구는 `/workspace/toolchains`에 보존. 설치 스크립트 재실행 통과.
2. `RideBikeBinding`의 읽기 함수가 `.bike` 심볼릭 링크를 따라 외부 UUID를 신뢰하는 오류 수정. strict 읽기는 정상 대상 링크와 끊어진 링크 모두 거부. 표시용 읽기도 링크를 따라가지 않음. 기존 정상 파일 및 실제 누락 파일의 하위 호환 유지.
3. 링크·외부 원본 보존·기존 누락 파일 회귀 추가. 수정 전 코드에서는 새 회귀가 실패함을 확인. 원본 GPS·스키마·계기판·APK 서명 변경 없음.
4. 최초 기준 + n×305분 dispatcher 작성. 실패한 슬롯 중복 재시도 방지, flock 동시 실행 차단, 누락 슬롯 몰아 실행 방지. `automation/README.md` 참조.

## 실제 검증 결과

- `source /workspace/toolchains/activate.sh` 후 `MOTO-HUB/`에서 실행.
- `gradle :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --no-daemon --max-workers=2`: BUILD SUCCESSFUL.
- Android 단위 테스트 80 suites, 총 224: 통과 221, 건너뜀 3, 실패/오류 0. 건너뜀은 통과로 계산하지 않음.
- Android Lint 오류 0, 경고 93. 경고 해결 여부는 별도 분류 필요.
- 모든 `tests/run-*.sh` 176개 PASS. 실제 Java 실행과 문자열 기반 구조 검사 혼합이며 실기기 검증을 대신하지 않음.
- `test_workshop_ui.py` 26개, `test_readable_ui.py` 22개 정적 조건 통과.
- dispatcher 단위 테스트 3개 통과(앵커 경계 100구간·실패 슬롯/누락 구간·동시 실행).
- 디버그 APK 생성: `MOTO-HUB/app/build/outputs/apk/debug/app-debug.apk`; SHA256 `c241d826732f9f9325cd06f5f9eb15df792a4a015536a7c12c0817dc47ad9c9d`. 배포본 아님.
- 생성된 UI 프리뷰 16개는 `/workspace/setup-results/native-previews`에 보존. 320dp light 홈 프리뷰에서 핵심 라이딩 시작·거리·정비·바로가기와 4탭 구조 확인. 실기기 사용성 검사 아님.

## 현재 공개 APK 확인

GitHub Releases 웹 페이지에는 `moto-hub-v575-offline`, `moto-hub-v575` 등이 표시됨.
`main/updates/manifest.json`이 가리키는 v575 `MOTO-HUB.apk`를 실제 다운로드해 확인:

- package `com.motohub.next`, versionCode 575, versionName `4.0.0-alpha6-di.575`.
- SHA256 `3331ba897e2843b1b5dfe2dd324a9d060e1d876b2f29fdef20afad45e8054dae`가 manifest와 일치.
- 공식 Android 36 `apksigner verify` 성공, v2 서명 확인.
- 인증서 SHA256 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b` 일치.
- 사용자의 실제 설치본 인증서·덮어쓰기·데이터 유지·최신 CI 결과는 확인하지 못함.

## 미완료 및 외부 조건

- **305분 예약 실행 미설정.** 예약 API·crontab·동작 중인 systemd가 없음. 외부 호스트용 실행 가능한 dispatcher/cron 예제만 작성·검사. Codex CLI 미래 실행 인증과 계정 사용량 복구 시각 조회는 미검증. 한도 우회·무한 재시도 없음.
- GitHub Git 읽기 가능. GitHub API는 프록시 CONNECT 403. 환경 초안에 기존 패키지 프리셋 유지 + `api.github.com`을 추가했으나 현재 인스턴스 반영/CI API/PR 생성 미검증.
- `install_script`, `start_skill` 저장 완료. 설정 검토·저장 후 환경 게시가 필요하며 새 작업 복원은 아직 검증하지 않음.
- 실기기 GPS/백그라운드/재부팅/OBD/Bluetooth/전원 중단 복원, 지도·Supabase 온라인 동작, 고정 개인키 빌드 미검증. 정식 배포 미수행.
- 전체 기능·보안·성능 종합 감사는 미완료. 초기 코드 구조/manifest/백업/바이크 연결 검토 후 검증 가능한 안전성 수정 1건을 완료한 상태.
- 승인 필요: 사용자 데이터 삭제, 비호환 형식, 서명/업데이트 경로 변경, 유료 서비스, 위험한 운영 변경, 정식 APK 공개 배포.

## 다음 실행

`DEVELOPMENT_BACKLOG.md` 순서로 실제 증거를 재확인한다. 기존 checkout을 사용하고 worktree는 명시 요청이 있을 때만 만든다. 커밋 SHA는 Git의 실제 HEAD를 읽고 이 문서의 기준 SHA와 구분한다.

## 재개 실행: 2026-11-05 00:50~ Asia/Seoul

- 원격 main/release/design/작업 브랜치 SHA 재조회: 앞서 기록한 커밋과 일치. 사용자 변경 없는 checkout에서 계속 작업.
- 이번 수정 기준: `6d0d0a2511eb2c04fbf7fc6edf2127512e35baa8`.
- 320dp 큰 글꼴 프리뷰에서 하단 `정비·주유`가 세 줄로 나뉘는 실제 UI 문제를 기준으로 수정.
- 하단 표시를 `기록`으로 단순화. 실제 목적지 ID `차고`, 화면 제목 `정비 · 주유`, 음성 설명 `정비·주유`는 유지. 모든 탭의 실제 높이 일치, 텍스트 중복 접근성 노출 방지.
- 공통 2개 버튼 배치를 360dp 미만 또는 글꼴 130% 이상이면 세로로 전환. 버튼 클릭 기능/색/최소 터치 영역 유지.
- 기록 화면 상세 도구의 펼침·접힘에 맞게 버튼 문구와 음성 설명 변경. 기존에 펼친 후에도 `보기/펼치기`가 남던 문제 수정.
- `AccessibleNavigationTest` 추가: 320/393dp 및 100/130/150/200% 글꼴에서 실제 Android 뷰 측정·높이·라벨 잘림·음성 설명·탭 이동·두 버튼 클릭·펼침/접힘·DB 무변경 확인. 새 테스트 3개 통과.
- Android `assembleDebug`, `lintDebug`, `testDebugUnitTest`: BUILD SUCCESSFUL. 81 suites 총 227개: 통과 224, 건너뜀 3, 실패/오류 0. Lint 경고 93, 오류 0.
- UI 정적 검사 48개 및 관련 기존 UI 구조 검사 3개 스크립트 통과.
- 디버그 APK 생성 및 apksigner 검증 성공. SHA256 `fe6add3bbfbdb3f688ad70bc15e7748a32a485d65461b4f14c954a154c318271`. 정식 고정 서명 배포본이 아니며 사용자 설치본 업데이트용으로 안내하지 않음.
- 새 하단 메뉴 PNG 8개: `MOTO-HUB/app/build/reports/ui-review/navigation-{320,393}-{1.0,1.3,1.5,2.0}.png`. 320dp/150%, 200% 이미지를 직접 확인.
- 실기기 상호작용·TalkBack 종단 간 검사는 미실행. 자동 예약 및 GitHub API 차단 상태는 앞선 미완료 항목과 동일.
- 기존 전체 `tests/run-*.sh` 176개 재실행 모두 PASS. 이번 실행에서 실패한 최종 검사 없음.

## 추가 실행: GPS 추정 통계 일관성 수정

- 기준 커밋 `bc06d80e1be4ffa390110a99790b69da62f5ff97`에서 재현 검사 6개 중 5개 실패 확인: 바이크 변경 후 이전 합계 잔류, 통합 원본 중복 합산, 복구함 이동 미반영, 동일 크기/mtime 원본 교체 후 오래된 거리 재사용, 백그라운드 재계산 모니터에 홈 조회 대기.
- 추정 통계는 현재 저장된 완료·바이크 지정 GPS 컬렉션에서 원자적 스냅샷으로 재구성. 통합 원본/보관본/미완료/미지정 기록 제외. 기존 SharedPreferences 형식을 유지하고 계기판이나 원본 GPS를 수정하지 않음.
- 거리 캐시의 SHA256 재검증을 재사용해 같은 크기/mtime 교체도 감지. 캐시 요약에 좌표 수를 포함해 기존의 좌표 0개 제외 조건 유지.
- 바이크 변경, 통합/취소, 복구함 이동/복원 후 비동기 갱신 연결. 삭제뿐 아니라 재배정/거리 변경도 화면 갱신 대상으로 처리.
- 홈의 합계 조회는 긴 쓰기 모니터를 획득하지 않고 이전에 확정된 SharedPreferences 스냅샷을 읽음. 원본 확인/저장 실패 및 기존 250,000자 제한 초과 시 부분 합계를 게시하지 않고 이전 합계 유지.
- 홈 설명을 `저장된 GPS 추정`으로 명확화. 재검증을 위해 백그라운드에서 현재 GPS 파일을 읽는 비용은 있으며 실제 대규모 기기 성능은 별도 검증 필요.
- `GpsEstimateConsistencyTest` 9개: 재배정, 통합/취소, 복구함/복원, 동일 크기/mtime 교체, 실제 재계산 지연 중 홈 조회, 일부 원본 손상 시 이전 스냅샷 보존, 미완료/미지정 제외, 좌표 0개 제외, 안전하지 않은 컬렉션 실패 검사. 모두 통과.
- 전체 Android build/lint/unit 통과. 총 236개 중 통과 233, 건너뜀 3, 실패/오류 0. 전체 회귀 스크립트 176개 PASS.
