# MOTO HUB GitHub 오류 수정 이어가기 — 2026-10-18

## 작업 기준
- 저장소: https://github.com/mercurial0416-lgtm/moto
- 수정 브랜치: `fix/aq-validation-handoff-20261018`
- 원본 main: `dc960820ecce949c69ddc33565e86129045bab5b`, Alpha6 AQ (448).
- 현재 소스: `MOTO-HUB/` 디렉터리. 루트의 AB/AH/AQ ZIP은 과거 체크포인트다. ZIP을 다시 풀어 수정본을 덮어쓰지 말 것.
- 다른 대화에 Alpha6 AZ(457) 체크포인트가 있다는 이전 대화 기록이 있다. 이번 작업은 사용자가 지정한 GitHub AQ를 기준으로 했다. AZ를 읽거나 병합하지 않았다. AQ 수정본을 AZ보다 최신 전체 개발본이라고 주장하지 말 것. 다음에는 AZ를 별도 디렉터리에 풀어 변경사항을 비교/병합한다.
- 버전명: `4.0.0-alpha6-aq-fix1`, versionCode 448. 패키지 `com.motohub.next`.

## 이번 수정
1. ZIP 안의 소스를 Git에서 직접 추적하도록 추가. CI의 불필요한 sed 패치 제거.
2. 바이크 사진 선택 변경 시 이전 이미지/출처 링크 초기화. 캐시 출처 JSON 검증 후에만 이미지 적용.
3. ZIP 복원 전 안전 사본 생성이 실패하면 원본에 롤백을 시도하지 않음.
4. 복원 후 롤백까지 실패한 경우 `moto-rollback-*` 안전 사본을 삭제하지 않고 복구 위치를 오류에 표시.
5. 전체 백업에서 25MB 초과 파일을 조용히 누락하며 성공 처리하던 문제 수정. 파일 크기/읽기 오류를 사용자에게 전달하고 읽는 중 총량도 검사.
6. GitHub Actions에서 순수 Java 테스트, assembleDebug, lintDebug, Robolectric 단위 테스트와 APK 서명/SHA 검사를 실행. CI에서는 Supabase URL/key를 비워 실제 서버 요청 방지.

## 검증 및 제한
- 로컬 Java 테스트 스크립트 24개 실행. model 스크립트는 JSON_JAR 지정 후 재실행 통과. 복원 테스트는 롤백 실패 재현을 포함해 22개 검사 통과.
- 로컬 Gradle: 의존성 다운로드 제한, offline 캐시 부족으로 빌드 불가. GitHub Actions 검증 결과는 최종 결과 항목에서 확인.
- Galaxy S25 / Android 16 실기기, 설치, 키보드, 화면 꺼짐 GPS, 두 계정 위치 공유는 미실행.
- 이번 작업에서 Supabase 스키마나 실제 데이터를 수정하지 않았다.
- debug 서명은 CI 실행마다 달라질 수 있다. 기존 설치본과의 서명 일치/업데이트 데이터 보존은 보장하지 않는다. 기존 설치본을 삭제하기 전에 전체 ZIP·GPX를 백업하고 사용자 소유 release 서명키를 준비할 것. 키는 Git/소스 ZIP에 포함하지 않는다.

## 빌드와 테스트
Java 17, Android SDK 36, Build Tools 36.0.0.
```sh
cd MOTO-HUB
chmod +x gradlew
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest -PsupabaseUrl= -PsupabaseKey= --no-daemon
# 순수 Java 검사: org.json:json:20240303 JAR을 JSON_JAR로 지정
export JSON_JAR=/absolute/path/json-20240303.jar
for test in tests/run-*.sh; do bash "$test"; done
```
Actions는 APK와 lint/test 보고서를 같은 artifact로 업로드한다. 과거 `evidence`나 기존 문서의 통과 횟수를 이번 실행 결과로 합산하지 말 것.

## 다음 작업
1. 이번 수정 브랜치의 최종 Actions 결과 확인 후 PR 검토/병합.
2. AZ 체크포인트와 비교해 이번 데이터 보호/사진 상태/CI 수정을 병합. 기존 기능을 AQ로 되돌리지 말 것.
3. 전체 ZIP 복원 중 UI 편집·GPS 시작과의 동시 실행 차단을 추가 검증.
4. 전체 ZIP의 매우 많은 항목/손상 경로/GPX 확장 크기 처리와 복원 호환성 추가 검증.
5. 사용자 기기에서 설치/실행/기록 저장/복원 및 GPS 테스트. 검증한 결과만 기록.

## 다른 채팅에 붙여넣기
`mercurial0416-lgtm/moto` 저장소의 `fix/aq-validation-handoff-20261018` 브랜치를 열고 `CONTINUE_HERE_KO.md`부터 읽어. GitHub AQ 기반 오류 수정본이며 다른 대화의 AZ와 아직 병합하지 않았어. Actions 결과와 테스트 로그를 확인한 뒤 AZ와 비교 병합하고 실기기 미검증 항목을 이어서 처리해. 기존 데이터와 서명키 호환성을 지켜줘.
