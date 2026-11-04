# 1시간 개발 실행 대체 구성 — 아직 예약 미설정

개발 브랜치를 처음 만든 시각(검증 가능한 Git reflog 기준)을 최초 개발 기준으로 고정:
`2026-11-05 00:27:37 Asia/Seoul` (`2026-11-04T15:27:37Z`).
2026-11-05 사용자 요청으로 305분에서 1시간 간격으로 변경.
후속 기준 시각은 매시 27분 37초이며 항상 최초 시각 + n×60분이다.
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
실행이 1시간을 넘으면 이전 실행 종료 후 첫 타이머 호출에서 현재 슬롯을 시도한다.
사용량 복구 시각은 현재 읽을 수 없으며, 이용 한도 우회나 복구 보장은 없다.
state-dir은 영속 디스크를 사용해야 한다. 원격의 다른 작업과 충돌하지 않도록
이 checkout을 해당 스케줄러 전용으로 사용한다. 자동 실행 활성화 시각에 작업 중인
다른 세션이 없는지도 호스트 운영자가 확인해야 한다.

현재 클라우드에는 crontab과 동작 중인 systemd가 없고 예약 API도 제공되지 않는다.
따라서 이 구성은 작성·로컬 검사만 했으며 예약 등록, 미래 실행, CLI 인증은 미검증이다.
스크립트 상태 확인: `python3 automation/dispatch.py --dry-run`
검사: `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s automation -p 'test_*.py'`

한 실행은 근거가 있는 개선 두 묶음 이상을 목표로 한다. 각 묶음은 독립 검증과
검토 가능한 커밋으로 남긴다. 같은 수정의 문서/테스트를 별도 작업으로 세거나,
작업 수를 채우려고 불필요한 변경을 만들지 않는다. 권한·실기기·사용량 제한 등
구체적 외부 조건이 막으면 미완료 항목을 정확히 기록한다.

이전 305분 구성의 state 파일이 있어도 attempted_at을 현재 1시간 슬롯으로
다시 계산하여 같은 시간대에 중복 실행하지 않는다. 실행 시간이 한 시간을 넘으면
동시 실행은 건너뛰고 이후 호출에서 현재 슬롯만 시도한다. 정확한 매시 실행 및
성공은 호스트 가동·예약 등록·인증·사용량에 달려 있다.

Linux systemd 사용자 타이머 예제도 함께 제공한다:
`moto-hourly.service.example`, `moto-hourly.timer.example`.
호스트 경로·Codex CLI PATH/인증·도구를 준비한 뒤 사용자 unit 디렉터리에
`.example`을 뺀 이름으로 설치하고 `systemctl --user enable --now moto-hourly.timer`로
등록할 수 있다. 예제는 매시 27분 37초 Asia/Seoul로 실행하며 실제 calendar 표현식은
`systemd-analyze calendar`로 검증했다. 현재 클라우드에 unit을 설치/활성화한 것은 아니다.
호스트 로그아웃 후에도 유지하려면 호스트의 사용자 서비스 가동 정책도 설정해야 한다.
