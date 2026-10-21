from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
h=Path('app/src/main/java/com/motohub/next/HubScreens.java').read_text()
checks={
 'compact new expense/service': 'boolean fastEntry=existing==null&&(key.equals("expenses")||key.equals("services"));' in s,
 'editable date/odo section': 'dateMileageFields.setVisibility(open?View.VISIBLE:View.GONE);' in s,
 'saved date/mileage': 'field(dateMileageFields,f,old,x[0],x[1],x[2]);' in s,
 'latest bike-only fuel price': 'bikeId.equals(entry.optString("vehicleId"))' in s,
 'explicit fuel calculation': 'QuickLogMath.litersFromAmount(' in s and 'QuickLogMath.amountFromLiters(' in s,
 'item-specific maintenance': 'QuickServiceHistory.latest(items("services"),bikeId,type.getText().toString().trim())' in s,
 'manual interval recalculation': 'RecentRecordReuse.nextServiceKm(' in s,
 'duplicate confirmation': 'QuickLogDuplicate.exists(items(key),item,key)' in s and '그래도 저장' in s,
 'fresh vehicle ID preserved': 'item.put("vehicleId",old.optString("vehicleId",b.optString("id")))' in s,
 'same saved backup mechanism': 'write(next);if(key.equals("vehicles")||key.equals("services")||key.equals("expenses"))DailyMaintenanceJob.checkSoon(this);clearDraft();close();render();toast("저장했어요.")' in s,
 'draft recovery unaffected': 'void resumeDraft(){' in s,
 'fuel full tank opt-in': 'full.setChecked(old.optBoolean("fullTank"))' in s,
 'top-level shortcuts': 'a.pair(actions,"간편 주유",()->new QuickEntryUi(a).fuel(),"정비주기 설정",()->new QuickEntryUi(a).maintenance())' in h and 'a::batchServiceEntry' in h,
 'full-tank quick button': '가득 주유로 표시' in s and 'fullTankBox.setChecked(true)' in s,
 'DIY maintenance zero-cost button': '직접 작업 · 비용 0원' in s,
 'nonfuel hides unit price label': 'if(fuelUnitLabel!=null)fuelUnitLabel.setVisibility(isFuel?View.VISIBLE:View.GONE)' in s,
 'saved odo only goes forward': 'if(v!=null&&item.optDouble("mileage")>v.optDouble("odo"))' in s,
 'no accidental write from helper': 'Store.upsert(' not in s[s.index('void quickRecordTools('):s.index('void field(')],
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL')+' quick-log UI '+k)
assert all(checks.values())
