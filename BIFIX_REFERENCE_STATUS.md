# BIFIX 기준 개편: 레퍼런스 확보 및 화면 조사

## 확인 상태

- 작업 기준: 사용자 지정 BIFIX 한 곳. MOTO HUB 브랜드·사진·아이콘 자산은 독립적으로 유지.
- `https://bifix.app/`, Google Play `com.bifix.app`: 2026-11-05 UTC 재확인: 실행 환경 프록시에서 HTTP 403. 실제 앱 화면·웹 페이지를 확인하지 못했다.
- 현재 대화/checkout에 BIFIX 스크린샷 없음. 존재하지 않는 첨부를 분석했다고 주장하지 않는다.
- 공식 도메인 4개를 네트워크 설정 초안에 추가했으며 저장 확인. 설정 검토·저장 후 환경 게시가 필요하며 현재 실행 인스턴스 접속 허용을 뜻하지 않는다.
- 따라서 색상, 폰트, 카드 수치, 정보 순서, 내비게이션, 지도 UI, 바텀시트, 전환 동작은 모두 **미확인**이다. 추측으로 BIFIX 디자인 시스템을 만들지 않는다.
- 아래 기능 확장은 기존 MOTO HUB UI 구성 요소를 임시로 재사용한다. BIFIX UI 개편 완료로 계산하지 않는다.

## 기존 화면·기능 대응 조사

| MOTO HUB 화면/기능 | 현재 구현 | BIFIX 자료 확인 |
| --- | --- | --- |
| 홈·차고 대시보드 | GarageHomeUi, ExperienceUi | 필요 |
| 헤더·하단 내비게이션 | MotoDesign, MainActivity.render | 필요 |
| 내 바이크·판매/보관 | HubScreens.manageBike, chooseBike | 필요 |
| GPS 시작·종료·집중 모드 | RideUi | 필요 |
| 지도·재생·속도별 경로 | RideMapActivity | 필요 |
| 기록 목록·검색·필터 | RideUi, HubScreens.records/searchAllRecords | 필요 |
| 주행 상세·분석 | MainActivity.openRideInsights | 필요 |
| 정비·소모품 | ServiceStatusUi, QuickEntryUi.maintenance, ComponentBookUi | 필요 |
| 주유·영수증 | QuickEntryUi.fuel, QuickFuelReceipt, ReceiptOcr, ReceiptTextParser | 필요 |
| 주행 통계·유지비 | RideStatsUi, CostReportUi, HubScreens.legacySixMonthCosts | 필요 |
| OBD2·장치 연결 | DeveloperObdScreen, ObdAutoSpeedMonitor | 필요 |
| 친구·그룹·공유 | SocialUi, GroupUi, GroupMapActivity, Sharing | 필요 |
| 사진 일지·여행기·수동 지점 | RidePhotoUi, RidePhotoJournal, TripJournalUi, TripNotes | 필요 |
| 백업·복구·이전 | RecoveryCenterUi, HubScreens.basicBackup/advancedBackup, BackupBundle | 필요 |
| 설정·업데이트·진단 | HubScreens.settings, AppUpdater | 필요 |
| 전체 입력 폼·팝업 | MainActivity.page/edit, AlertDialog | 필요 |

공통 외형 변경은 검증 가능한 자료 확보 후 이 표를 화면별 대응·검토 결과로 갱신한다. 패키지명·서명 설정·GPS/OBD·로컬 v3 데이터 형식과 기존 업데이트 경로는 유지한다.

## 기능 확장 검증 결과 (BIFIX 외형 검증 아님)

부품 장부·기간별 비용 비교·영수증 확인 입력·수동 지점 여행기·복구센터를 실제 코드에 추가했다. 기존 화면 API를 사용했으며 BIFIX 전용 토큰/레이아웃/에셋은 만들지 않았다. 데이터 스키마와 앱 패키지·서명 설정은 유지했다.

Android 95 suites / 325개 중 통과 322·건너뜀 3·실패/오류 0. Lint 오류 0·경고 90. shell 회귀 176개 최신 결과 통과. 신규 기능 네이티브 렌더 4개 중 비용 비교와 여행기 이미지를 직접 열어 검토했다. 실기기 사진 OCR 인식·지도 선택 제스처·TalkBack·설치 업데이트는 미검증이다.

실제 BIFIX 화면을 확보하면 이 표의 '필요'를 해당 출처·확인 가능한 레이아웃·적용 화면·렌더 비교 결과로 대체해야 한다. 정지 화면으로 전환/터치 동작을 확정하지 말고 실제 앱 관찰이나 영상이 있어야 구분해서 기록한다.
