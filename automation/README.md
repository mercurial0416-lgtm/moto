# 305분 개발 실행 대체 구성 — 아직 예약 미설정

개발 브랜치를 처음 만든 시각(검증 가능한 Git reflog 기준)을 최초 개발 기준으로 고정:
`2026-11-05 00:27:37 Asia/Seoul` (`2026-11-04T15:27:37Z`).
후속 기준 시각은 05:32:37, 10:37:37, 15:42:37이며 항상 최초 시각 + n×305분이다.
이 파일은 Codex 자동화 등록이나 실행 성공을 의미하지 않는다.

항상 켜진 Linux 호스트에서 인증된 Codex CLI, GitHub 접근, 같은 경로의
체크아웃과 도구를 준비한 뒤 외부 cron에 다음 한 줄을 등록할 수 있다:

```cron
* * * * * /usr/bin/python3 /workspace/moto/automation/dispatch.py /bin/bash /workspace/moto/automation/run-development.sh >> /workspace/onboarding/scheduler.log 2>&1
```

분 단위 호출이므로 실제 실행은 기준 시각 이후 최대 약 60초 늦을 수 있다.
초 단위 정확도가 필요하면 외부 타이머가 매초 dispatcher를 호출하도록 설정한다.
간격 계산은 UTC epoch, 표시 시간대는 Asia/Seoul이다. flock으로 중복을 막고,
시도 전에 슬롯을 영속 기록하여 중단·사용량 부족에도 같은 슬롯을 재시도하지 않는다.
오래 중단된 경우 누락 실행을 몰아 실행하지 않고 현재 슬롯만 시도한다.
실행이 305분을 넘으면 이전 실행 종료 후 첫 타이머 호출에서 현재 슬롯을 시도한다.
사용량 복구 시각은 현재 읽을 수 없으며, 이용 한도 우회나 복구 보장은 없다.
state-dir은 영속 디스크를 사용해야 한다. 원격의 다른 작업과 충돌하지 않도록
이 checkout을 해당 스케줄러 전용으로 사용한다. 자동 실행 활성화 시각에 작업 중인
다른 세션이 없는지도 호스트 운영자가 확인해야 한다.

현재 클라우드에는 crontab과 동작 중인 systemd가 없고 예약 API도 제공되지 않는다.
따라서 이 구성은 작성·로컬 검사만 했으며 예약 등록, 미래 실행, CLI 인증은 미검증이다.
스크립트 상태 확인: `python3 automation/dispatch.py --dry-run`
검사: `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s automation -p 'test_*.py'`
