# MOTO HUB — Android 4.0 Alpha6

최신 Android 앱 소스는 [MOTO-HUB/](MOTO-HUB/)에 있습니다. 과거 다운로드 ZIP으로 프로젝트 전체를 덮어쓰지 마세요.

- [기존 개발 체크포인트](CONTINUE_HERE_KO.md)
- [UX46 라이딩·주행거리·오프라인 성능 변경](MOTO-HUB/docs/UX46_RIDE_PERFORMANCE_KO.md)
- [UX47 전체 통합 / 고정 서명 배포 검증](MOTO-HUB/docs/UX47_INTEGRATION_RELEASE_READINESS_KO.md)
- [Android 디버그 코드 검증 워크플로](.github/workflows/moto-ux-validation.yml)
- [기존 설치본용 동일 서명 릴리스 워크플로](.github/workflows/moto-fixed-signed.yml)

**앱 설치본을 삭제하지 마세요.** 테스트용 디버그 APK는 기존 앱과 동일한 서명을 보장하지 않습니다. 데이터 보존은 동일 인증서 서명, 버전 상승, 실기기 덮어쓰기 설치 및 백업 복원 검증으로만 확인할 수 있습니다.

MOTO HUB는 GPS/주유/정비 내용을 휴대폰에 저장합니다. 선택적으로 연결하는 KTM 390 DUKE OBD2 기능은 실험적이며, 실차 표준 PID 호환성은 아직 검증되지 않았습니다. 고정 서명 릴리스는 원본 비공개 키와 별도 최종 검증이 필요합니다.
