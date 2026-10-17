# MOTO HUB 4.0 Alpha6-L · 복원 안정성 체크포인트

## UI → 편의성 → 필요성

- Alpha6-K의 주행 요약/지도/유지비 인사이트/슈퍼커브 이미지/친구 그룹 기능 유지
- 설정 → 백업·복원에서 **전체 ZIP 백업 복원** 최상단 배치
- 복원 전 요약 화면: 바이크 수, GPS 수, 사진 수, 기존 기록 교체 경고
- ZIP 하나로 `MOTO-HUB-data.json`, 바이크 사진, GPS 원본 및 `.done`/`.imported` 표시 복원
- 기존 데이터의 `before-import.json` 사본 생성, 설치 실패 시 리소스 되돌리기 시도
- ZIP Slip 차단, 미허용 ZIP 파일 차단, 중복 파일 차단, JPEG 매직 바이트, GPS 시간·좌표 정합성, 압축 해제 최대 250MB/개별 25MB 제한, 6000개 파일 상한
- 로그인 세션/공유 상태를 ZIP에 넣거나 자동 위치 공유 시작하지 않음
- GPS 기록 중 ZIP 복원 거부

## 실행 확인

- `./tests/run-restore-tests.sh` → 17개 검사 통과 (실패 롤백, zip slip 등)
- `./tests/run-ride-tests.sh` → 16개 검사 통과
- `./tests/run-backup-tests.sh` → 통과
- 기존 JVM 테스트 → 19+16+12+12개 통과
- 브랜드 카탈로그 → 49개 브랜드 522개 모델 통과
- Java 구문 → 31개 파일 파싱 통과

## 남은 중요 작업

- **APK 빌드/실기기 검증 전**: Android SDK가 없어 네이티브 앱 빌드를 실행하지 못함
- GitHub Actions 워크플로에 ZIP 복원 테스트 추가; APK 빌드는 Actions에서 진행 필요
- 관리자 승인/키 없이 외부 서버에 매물 실시간 크롤링, 푸시, 내비 API 추가하는 건 미구현
- ZIP 복원은 기존 데이터 교체이며, 자산의 실패 롤백은 best effort; 복원 전 원본 백업을 별도로 보관해야 함
- 두 기기 친구 위치 공유 실기기 검증 전
