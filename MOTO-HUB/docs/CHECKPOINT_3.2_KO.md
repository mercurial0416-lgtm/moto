# 3.2 빌드 체크포인트 — 2026-10-15 UTC
기존 첨부 3.2.0-dev 소스 재사용. package com.motohub.next, versionCode 320, compile/target SDK 36, min SDK 26.
이번 세션에서 JVM 데이터/주유 35개 + 기변/정비 19개 테스트 통과. evidence/*this-session.txt 참고.
Android 설치·실행·화면·SAF 백업·복원·Android16 런타임은 아직 미검증. SmokeRunner는 테스트 코드이며 실행 통과 기록이 있어야 결과로 인정할 수 있음.
서명키는 소스 ZIP에서 제외. 새 환경 debug 키로 빌드되므로 이전 3.1과 인증서를 반드시 비교해야 함. 패키지가 같아도 서명이 다르면 덮어쓰기 불가. 기존 앱을 지우기 전에 JSON 백업 필수. 기존 com.motohub.app과는 별도 패키지.
원본 JSON motohub-v3.json 및 스키마 version3 유지. 안드로이드 자동 백업 비활성, 앱의 수동 JSON 백업 사용. 출발 체크/미저장 초안은 JSON 백업 대상 아님.
친구/서버/GPS 미구현. 이 체크포인트는 4.0 완성판이 아님.
다음: Android 실행 검증, 구조적 UI 개선, 위치 기능 및 권한 테스트.
