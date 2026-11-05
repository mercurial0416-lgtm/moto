package com.motohub.next;

import android.app.*;
import android.os.Looper;
import android.widget.*;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MajorFeatureUiTest {
 MainActivity a;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(MainActivity.class).setup().get();new DashboardDesignTest().seed(a);a.render();}
 @After public void cleanup(){a.close();a.finish();}
 void await(BooleanSupplier done)throws Exception{for(int i=0;i<300;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(done.getAsBoolean())return;Thread.sleep(10);}fail("Background task did not finish");}
 @Test public void costComparisonRunsOnSnapshotAndDoesNotChangeRecords()throws Exception{
  String before=a.db.toString();new CostReportUi(a).open();await(()->EntryUiFixture.contains(a.formBody,"103,500원"));assertTrue(EntryUiFixture.contains(a.formBody,"KTM 390 DUKE"));assertEquals(before,a.db.toString());EntryUiFixture.capture(a,"major-period-costs",393);
 }
 @Test public void costComparisonPaginatesVehiclesAndFiltersWithoutWriting()throws Exception{
  for(int i=1;i<=24;i++)a.arr("vehicles").put(new JSONObject().put("id",Store.id()).put("name","Bike"+i).put("odo",1000));String before=a.db.toString();new CostReportUi(a).open();await(()->Ux106NativeUiTest.desc(a.formBody,"차량 이름 필터")!=null);assertNull(Ux106NativeUiTest.text(a.formBody,"Bike20"));EntryUiFixture.text(a.formBody,"다음 차량").performClick();assertNotNull(Ux106NativeUiTest.text(a.formBody,"Bike20"));EntryUiFixture.field(a,"차량 이름 필터").setText("Bike24");assertNotNull(Ux106NativeUiTest.text(a.formBody,"Bike24"));assertNull(Ux106NativeUiTest.text(a.formBody,"Bike23"));assertEquals(before,a.db.toString());
 }
 @Test public void componentDetailsSavePreservesOriginalTransactionAndUsesAtomicWriter()throws Exception{
  JSONObject old=a.arr("services").getJSONObject(0);String id=old.getString("id");new ComponentBookUi(a).editDetails(old,a.selected);EntryUiFixture.field(a,"제품명").setText("Oil product");EntryUiFixture.field(a,"규격").setText("10W-40");EntryUiFixture.text(a.formFooter,"저장").performClick();await(()->!RestoreRideGate.isFormWriting()&&a.db.optJSONArray("services").optJSONObject(0).has("componentDetails"));JSONObject saved=Store.find(a.db,"services",id);assertEquals(80000,saved.getDouble("cost"),0);assertEquals("2026-11-01",saved.getString("date"));assertEquals("10W-40",saved.getJSONObject("componentDetails").getString("spec"));
 }
 @Test public void componentBookSearchAndHistoryAreReadOnly()throws Exception{
  String before=a.db.toString();new ComponentBookUi(a).open();await(()->EntryUiFixture.contains(a.formBody,"엔진오일 교환 · 1회"));EntryUiFixture.field(a,"부품 이름 검색").setText("없는 항목");assertTrue(EntryUiFixture.contains(a.formBody,"해당 부품의 기록이 없습니다."));EntryUiFixture.field(a,"부품 이름 검색").setText("");assertTrue(EntryUiFixture.contains(a.formBody,"엔진오일 교환 · 1회"));assertEquals(before,a.db.toString());EntryUiFixture.capture(a,"major-component-book",393);
 }
 @Test public void componentDetailsRejectARecordChangedWhileEditing()throws Exception{
  JSONObject record=a.arr("services").getJSONObject(0);new ComponentBookUi(a).editDetails(record,a.selected);a.arr("services").getJSONObject(0).put("cost",90000);EntryUiFixture.field(a,"제품명").setText("Changed product");EntryUiFixture.text(a.formFooter,"저장").performClick();assertFalse(a.arr("services").getJSONObject(0).has("componentDetails"));assertEquals(90000,a.arr("services").getJSONObject(0).getDouble("cost"),0);assertFalse(RestoreRideGate.isFormWriting());
 }
}
