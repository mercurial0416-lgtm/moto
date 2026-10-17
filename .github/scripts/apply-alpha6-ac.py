#!/usr/bin/env python3
"""Apply the Alpha6-AC source checkpoint to the Alpha6-AB baseline."""
from pathlib import Path
root = Path("MOTO-HUB")
java = root / "app/src/main/java/com/motohub/next"

def replace(path, old, new):
    value = path.read_text(encoding="utf-8")
    if old not in value:
        if new in value:
            return
        raise RuntimeError(f"Source mismatch: {path} ({old[:60]})")
    path.write_text(value.replace(old, new), encoding="utf-8")

for name in ("HubScreens.java", "GarageHomeUi.java"):
    replace(java / name, "tile.setMinHeight(", "tile.setMinimumHeight(")
replace(java / "RideMapActivity.java", "base.requestApplyWindowInsets()", "base.requestApplyInsets()")
old = 'for(JSONObject s:latestServices(b)){double next=s.optDouble("nextKm");if(next>0)min=Math.min(min,next-odo);String d=s.optString("nextDate");if(!d.isEmpty()&&(date.isEmpty()||d.compareTo(date)<0))date=d;}'
new = 'for(JSONObject s:latestServices(b)){double next=s.optDouble("nextKm");String d=s.optString("nextDate");if((next>0||!d.isEmpty())&&MaintenancePlanner.status(d,next,a.today(),odo,30,500)==MaintenancePlanner.Status.OVERDUE)return "점검 필요";if(next>0)min=Math.min(min,next-odo);if(!d.isEmpty()&&(date.isEmpty()||d.compareTo(date)<0))date=d;}'
replace(java / "GarageHomeUi.java", old, new)
replace(root / "app/build.gradle", "versionCode 433; versionName '4.0.0-alpha6-ab'", "versionCode 434; versionName '4.0.0-alpha6-ac'")
anchor = '  check(MaintenancePlanner.status("",4500,"2026-10-16",4500,30,500)==Status.OVERDUE,"reached mileage");'
replace(root / "tests/OfflineFeatureTest.java", anchor, anchor + '\n  check(MaintenancePlanner.status("2026-10-15",8000,"2026-10-16",4500,30,500)==Status.OVERDUE,"expired date takes priority over distant mileage");')
(root / "CHANGELOG-alpha6-ac.md").write_text("""# MOTO HUB 4.0 Alpha6-AC (2026-10-17)

- 홈의 `다음 정비까지` 카드: 예정일이 이미 지났는데 주행거리까지 여유가 남은 경우에도 **점검 필요**를 우선 표시.
- Android View API 오타 3건을 원본 소스에서 수정 (`setMinimumHeight`, `requestApplyInsets`), 빌드용 임시 패치 제거 가능.
- 정비 예정일/주행거리 충돌 회귀 테스트 추가.
- 기존 로컬 정비·주유·매물·GPS·GPX·친구/그룹·백업/복원 로직 변경 없음.
- 실제 단말 동작 미검증.
""", encoding="utf-8")
print("Applied Alpha6-AC maintenance home and compilation fixes")
