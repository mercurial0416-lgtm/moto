# MOTO HUB - Android 앱 내 업데이트 배포 절차

채널: Google Play 사용 안 함. 설치된 MOTO HUB > 더보기 > 앱 업데이트 > 배포 버전 확인.

자동 배포 트리거: `release/moto-hub-apk` 브랜치에 변경사항이 푸시될 때
`.github/workflows/moto-apk-inapp-publish.yml` 실행.

절차:
1. 전체 소스·Android 단위/린트 테스트 통과
2. 원래의 고정 서명 인증서로 새 버전 APK 빌드. 같은 앱 ID 필수
3. 인증서 SHA-256, APK 파일 검사 및 기존 공개 버전보다 증가한 버전코드 검증
4. GitHub 공개 Release의 `MOTO-HUB.apk` 업로드 완료
5. GitHub `main/updates/manifest.json`의 HTTPS URL과 SHA-256 갱신
6. 휴대폰에서는 앱 안에서 새 버전 확인·다운로드·서명 확인·Android 설치 승인

**반드시 필요한 외부 조건:** GitHub Actions 시크릿 4개
`MOTO_SIGNING_STORE_B64`, `MOTO_SIGNING_STORE_PASSWORD`,
`MOTO_SIGNING_KEY_ALIAS`, `MOTO_SIGNING_KEY_PASSWORD`.
기존 서명키는 공개 저장소/PR/배포 APK 이외의 압축파일에 업로드 금지.
시크릿 중 하나라도 없으면 배포를 실패 처리하고 공개 manifest는 보존한다.

업데이트 중 라이딩/OBD 기록 및 백업·복원 작업 차단.
서명이 다른 기존 설치본은 덮어쓰기 금지; 데이터 삭제 전에 백업.
명시적으로 확인되지 않은 2024 Duke 390 OBD 기능은 실험 기능으로 유지.
