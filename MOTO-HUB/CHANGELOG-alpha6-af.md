# MOTO HUB 4.0 Alpha6-AF

## 바이크 사진 표시 안정화
- Wikimedia Commons 검색에서 Android BitmapFactory가 읽을 수 없는 SVG/TIFF/PDF 등의 파일이 먼저 선택되면 사진 표시가 실패하던 문제 수정.
- Commons imageinfo에 MIME 유형을 요청하고 JPEG/PNG/WebP만 사진 후보로 채택.
- 연식이 비어 있을 때도 의미가 맞는 검색 상태 문구로 변경하고, 검색 실패 시 직접 사진 등록 방법을 안내.
- 기존 직접 등록한 사진 우선, 모델명/브랜드 검증, 라이선스/출처 표시, 위치 공유 동의 및 권한 로직 유지.
- 앱 버전: 4.0.0-alpha6-af (versionCode 437).

## 검증
- 순수 Java MIME 필터 단위 검사 9건 실행·통과.
- 코드 연결 및 ZIP 무결성 정적 검사 통과.
- Android Gradle APK 빌드와 실제 단말 실행은 수행하지 못함 (로컬 Android SDK/Gradle 의존성 부재).
