# 개발자 실험 — 듀크 390 자동 라이딩 감지

앱 > 더보기 > 앱 정보의 버전을 7번 탭하여 개발자 모드 활성화 > 개발자 모드 > 듀크 390 OBD2 읽기 전용(실험).

이미 휴대폰 Bluetooth 설정에 페어링된 ELM327 호환 Classic SPP 어댑터가 필요합니다. `OBD2 자동 라이딩 감지 켜기`를 눌러 기기를 선택하고 위치 기록에 별도 동의하세요. 기존의 수동 OBD2 센서 화면 연결과 동시에 사용하지 마세요.

자동 감지가 켜져 있으면 지속 알림이 보이고, 7km/h 이상 차량 속도(PID 010D)가 3번 연속 측정되면 휴대폰 GPS 기록을 시작합니다. 정차 3분 이후 완료 파일로 저장하고 같은 연결에서 다음 출발을 기다립니다. 속도 PID를 제공하지 않는 ECU라면 시작되지 않습니다. 분리/시동 종료로 연결이 끊기면 안전을 위해 자동 감지를 종료하며, 주행 중 연결이 끊긴 기록은 중단 기록으로 확인할 수 있습니다.

알림의 `자동 감지 중지`, 라이딩 화면의 종료 버튼, 개발자 모드의 끄기 버튼으로 즉시 해제할 수 있습니다. 자동 재연결, 부팅 후 감시, 그룹 공유, ECU 쓰기, DTC 삭제, 실제 계기판 누적 거리 자동 동기화는 제공하지 않습니다.

이 기능은 **실험 코드로만 소스에 반영**했으며 Android 컴파일, APK, 듀크 실차에서의 사용성/안정성은 아직 검증되지 않았습니다.

## Alpha6-CG follow-up

The OBD2 read-only experiment can now query **standard supported PID bitmaps 0100 / 0140**; these only report available Mode 01 sensors. They **do not** expose a verified Duke 390 odometer PID. Missing or malformed capability responses mean *unknown*, not supported.

Developer settings offer manual gauge ODO confirmation with explicit double approval. It does not write to the vehicle, query proprietary PIDs, or change historic ride/service records. **OBD detection is not automatic odometer synchronization**.

`AUTO_DISARM` disconnects the experimental OBD reader while preserving an active local GPS trip; `STOP` explicitly ends the GPS trip. KakaoNavi is an independent notification shortcut and receives no track payload. The battery setting shortcut merely opens Android Settings; it does not guarantee background execution.
