# UX39 · MOTO HUB 가독성 / 그래픽 품질 패스

## 기반
- UX38 소스 브랜치 `design/ux38-workshop-ui-20261025`에서 파생.
- 수정 브랜치 `design/ux39-readable-native-20261025`.
- 이 변경은 UI 표현만 수정하고 로컬 DB, GPS 서비스, 권한, 서명, 복원/백업 로직을 변경하지 않음.

## 변경
1. 금액·주행거리·유지비 숫자: Android 네이티브 `tnum` (고정 폭 숫자) 적용, 목록 우측 정렬 및 크기·대비 개선.
2. 제목과 본문: 원래 Android SP 배율 유지, 다중행 1~2dp 줄 간격 추가. 기록 내용은 13sp 이상으로 조정.
3. 큰 글꼴: 홈의 계기판 거리/정비는 한 줄 고정 29dp 높이에서 두 줄 대응 가능한 wrap-content로 변경. 매물 이름 2줄·말줄임 지원.
4. 차고/매물: 실제 숫자를 더 돋보이게, 만료/상태는 보조 표시. 미입력 연식이 '0년식'으로 보이지 않게 처리.
5. 필터: 크게 주황색 채운 버튼 대신 연한 선택 배경·주황 텍스트의 44dp 높이 버튼.
6. 하단 네비게이션: 매물 아이콘을 가격표, 설정 아이콘을 톱니바퀴 형태의 Android Canvas 선 아이콘으로 교체.
7. 최근 기록: 날짜와 금액 계층화, 장식용 영수증 기호를 상세 문구로 대체.
8. 사진 출처/대표 이미지 설명: 작은 10sp 오버레이를 11sp 고대비 패널로 변경.

## 변경 범위
- `MotoDesign.java`, `MainActivity.java`, `HubScreens.java`, `GarageHomeUi.java`, `NavIcon.java`
- `tests/test_readable_ui.py` (UI 구조와 기존 기능 연결의 정적 회귀)

## 검증 및 주의
- GitHub 소스 실제 파일을 다시 읽어 **25/25** 코드 조건을 점검함.
- 이 점검은 Android APK 빌드, 애뮬레이터 렌더링 또는 갤럭시 실기기 테스트를 대체하지 않음.
- 병합 전에 `python3 MOTO-HUB/tests/test_workshop_ui.py` 및 `python3 MOTO-HUB/tests/test_readable_ui.py` 수행, Android assembleDebug / lintDebug / testDebugUnitTest 필요.
- 글꼴 100%/150%/200%, 폭 360dp/411dp, 화면 회전, 라이트/다크 모드, 바이크/매물 긴 이름, 8자리 금액, 빈 목록, 홈의 GPS 동의/종료, 주유·정비 진입을 실기기 확인할 것.
- 설치된 기존 앱을 **삭제하지 말 것**. Debug APK는 인증서 일치가 검증되지 않은 상태에서 덮어쓸 수 없음.
