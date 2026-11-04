package com.motohub.next;

import android.widget.*;
import java.time.LocalDate;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MaintenanceConvenienceTest {
 MainActivity a;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(MainActivity.class).setup().get();new DashboardDesignTest().seed(a);a.render();}
 @After public void close(){a.close();a.finish();}
 @Test public void quickIntervalAndLivePreviewMatchThePersistedSchedule()throws Exception{
  new QuickEntryUi(a).maintenance();String before=a.db.toString();
  Ux106NativeUiTest.desc(a.formBody,"거리 주기 +3,000km 선택").performClick();EntryUiFixture.select(a,"또는 몇 개월 뒤에 정비할까요?",2);
  TextView preview=(TextView)EntryUiFixture.tag(a.formBody,"maintenance-due-preview");assertTrue(preview.getText().toString().contains("15,480 km"));assertTrue(preview.getText().toString().contains(LocalDate.parse(a.today()).plusMonths(3).toString()));assertEquals(before,a.db.toString());
  EntryUiFixture.capture(a,"maintenance-preview-light",393);EntryUiFixture.text(a.formFooter,"저장").performClick();
  JSONObject saved=a.arr("services").getJSONObject(a.arr("services").length()-1);assertEquals(15480,saved.getDouble("nextKm"),0);assertEquals(3000,saved.getInt("intervalKm"));assertTrue(saved.getBoolean("scheduleOnly"));assertEquals(0,saved.getDouble("cost"),0);
 }
 @Test public void changingOdometerRecalculatesThePreviewAndInvalidTargetsDoNotSave()throws Exception{
  new QuickEntryUi(a).maintenance();EntryUiFixture.select(a,"몇 km 뒤에 정비할까요?",3);
  EntryUiFixture.field(a,"현재 계기판 (km)").setText("13000");assertTrue(((TextView)EntryUiFixture.tag(a.formBody,"maintenance-due-preview")).getText().toString().contains("16,000 km"));
  EntryUiFixture.select(a,"몇 km 뒤에 정비할까요?",6);EntryUiFixture.field(a,"직접 지정하는 다음 정비 거리 (km)").setText("12000");String before=a.db.toString();EntryUiFixture.text(a.formFooter,"저장").performClick();assertEquals(before,a.db.toString());assertTrue(a.dialog.isShowing());
 }
 @Test public void cycleAdjustmentRestoresOnlyTheChosenSameBikeType()throws Exception{
  String id=a.selected;a.arr("services").put(new JSONObject().put("id","44444444-2222-4333-8444-555555555555").put("vehicleId",id).put("type","체인").put("date",a.today()).put("mileage",12480).put("nextKm",15480).put("intervalKm",3000).put("intervalMonths",3).put("scheduleOnly",true));
  String before=a.db.toString();new QuickEntryUi(a).maintenance("체인");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("체인",EntryUiFixture.spinner(a,"정비 항목").getSelectedItem());assertEquals(3,EntryUiFixture.spinner(a,"몇 km 뒤에 정비할까요?").getSelectedItemPosition());assertEquals(2,EntryUiFixture.spinner(a,"또는 몇 개월 뒤에 정비할까요?").getSelectedItemPosition());assertEquals(before,a.db.toString());
 }
 @Test public void recentServiceShortcutUsesTheNewestSameBikeType()throws Exception{
  a.arr("services").put(new JSONObject().put("id","old-oil").put("vehicleId",a.selected).put("type","엔진오일").put("date","2026-01-01").put("mileage",100));
  a.arr("services").put(new JSONObject().put("id","new-chain").put("vehicleId",a.selected).put("type","체인").put("date",a.today()).put("mileage",12480));
  String before=a.db.toString();new QuickEntryUi(a).maintenance();EntryUiFixture.text(a.formBody,"이 바이크 최근 정비 항목 · 체인 선택").performClick();assertEquals("체인",EntryUiFixture.spinner(a,"정비 항목").getSelectedItem());assertEquals(before,a.db.toString());
 }
 @Test public void moreThanEightCyclesAreReachableAndUrgencyIsReadable()throws Exception{
  for(int i=0;i<10;i++)a.arr("services").put(new JSONObject().put("id","cycle-"+i).put("vehicleId",a.selected).put("type","검사 "+i).put("date",a.today()).put("nextKm",i==0?12000:16000+i).put("mileage",11000));
  a.ledgerSchedulePage=true;a.navigate("차고");TextView summary=(TextView)EntryUiFixture.tag(a.root,"maintenance-status-summary");assertTrue(summary.getText().toString().contains("전체 11개"));
  Button more=(Button)EntryUiFixture.tag(a.root,"maintenance-more");assertTrue(EntryUiFixture.shown(more));assertEquals(8,((LinearLayout)EntryUiFixture.tag(a.root,"maintenance-cycle:cycle-0").getParent()).getChildCount());more.performClick();assertFalse(EntryUiFixture.shown(more));assertNotNull(EntryUiFixture.tag(a.root,"maintenance-cycle:cycle-9"));
  TextView overdue=EntryUiFixture.text(EntryUiFixture.tag(a.root,"maintenance-cycle:cycle-0"),"점검 필요");assertTrue(CommercialUiTest.contrast(overdue.getCurrentTextColor(),a.panel)>=4.5);CommercialUiTest.capture(a,"maintenance-schedule-light",393,852);
 }
 @Test public void maintenancePreviewSupportsDarkModeAndDoubleText()throws Exception{
  AccessibleNavigationTest.configure(a,320,2f);a.settings().put("theme","dark");a.render();new QuickEntryUi(a).maintenance();EntryUiFixture.select(a,"몇 km 뒤에 정비할까요?",4);EntryUiFixture.capture(a,"maintenance-preview-dark-large",320);
  TextView preview=(TextView)EntryUiFixture.tag(a.formBody,"maintenance-due-preview");assertTrue(preview.getText().toString().contains("17,480 km"));assertTrue(CommercialUiTest.contrast(preview.getCurrentTextColor(),a.highlight)>=4.5);
 }
}
