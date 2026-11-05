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
 @Test public void receiptRequiresConfirmationAndAppliesOnlyFieldsWithoutSaving()throws Exception{
  new QuickEntryUi(a).fuel();String before=a.db.toString();a.quickFuelReceipt.propose(ReceiptTextParser.parse("총 결제 금액 23,500원 주유량 12.5 L 단가 1,880원 2026-05-01"));assertEquals("",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());assertEquals(before,a.db.toString());ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("23500",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());assertEquals("12.5",EntryUiFixture.field(a,"실제 주유량 (L)").getText().toString());assertEquals("1880",EntryUiFixture.field(a,"리터당 가격 (원)").getText().toString());assertEquals("12480",EntryUiFixture.field(a,"현재 계기판 (km)").getText().toString());assertEquals(before,a.db.toString());EntryUiFixture.capture(a,"major-receipt-review",393);
 }
 @Test public void receiptCancellationAndChangedInputCannotOverwriteUserEdits(){
  new QuickEntryUi(a).fuel();a.quickFuelReceipt.propose(ReceiptTextParser.parse("총 금액 20000원 주유량 10 L"));ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());a.quickFuelReceipt.propose(ReceiptTextParser.parse("총 금액 20000원 주유량 10 L"));EntryUiFixture.field(a,"주유 금액 (원)").setText("21100");ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("21100",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());assertEquals("",EntryUiFixture.field(a,"실제 주유량 (L)").getText().toString());
 }
 @Test public void oldReceiptCallbackCannotAffectANewOrDifferentBikeForm(){
  new QuickEntryUi(a).fuel();QuickFuelReceipt old=a.quickFuelReceipt;a.close();new QuickEntryUi(a).fuel();old.propose(ReceiptTextParser.parse("총 금액 20000원 주유량 10 L"));assertEquals("",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());
 }
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
 @Test public void tripStopEditorPersistsOnlyNotesAndKeepsGpsUntouched()throws Exception{
  java.io.File track=new java.io.File(TrackFile.directory(a.getFilesDir()),"1760000000000-11111111-2222-4333-8444-555555555555.track");byte[] original=(TrackFile.point(37.5,127,5,1760000000000L)+TrackFile.point(37.501,127.001,5,1760000060000L)).getBytes(java.nio.charset.StandardCharsets.UTF_8);java.nio.file.Files.write(track.toPath(),original);new java.io.File(track.getPath()+".done").createNewFile();RideNotes.update(a.db,track,"Weekend tour","Original memo");TripJournalUi journal=new TripJournalUi(a);journal.open(track,RideInsights.analyze(track));journal.editStop(37.5,127.0);EntryUiFixture.field(a,"지점 이름").setText("휴식 장소");EntryUiFixture.field(a,"지점 메모").setText("커피 한 잔");EntryUiFixture.text(a.formFooter,"저장").performClick();await(()->!RestoreRideGate.isFormWriting()&&TripNotes.stops(a.db,track).length()==1);assertEquals("Weekend tour",RideNotes.title(a.db,track));assertArrayEquals(original,java.nio.file.Files.readAllBytes(track.toPath()));assertTrue(EntryUiFixture.contains(a.formBody,"휴식 장소"));EntryUiFixture.capture(a,"major-trip-journal",393);
 }
 @Test public void recoveryComparisonIsReadOnlyUntilTheSecondConfirmation()throws Exception{
  JSONObject point=new JSONObject(a.db.toString());point.getJSONObject("settings").put("fromSnapshot",true);assertTrue(LocalSafetyBackup.checkpoint(a,point));java.io.File selected=LocalSafetyBackup.recoveryPoints(a)[0];String before=a.db.toString();RecoveryCenterUi center=new RecoveryCenterUi(a);center.open();center.preview(selected);
  await(()->{AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();return d!=null&&d.getButton(AlertDialog.BUTTON_POSITIVE)!=null&&"복원 확인으로".contentEquals(d.getButton(AlertDialog.BUTTON_POSITIVE).getText());});assertEquals(before,a.db.toString());ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("복원",ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(before,a.db.toString());assertFalse(RestoreRideGate.isRestoring());
 }
 @Test public void staleFeatureSaveAndReviewedRestoreCannotOverwriteNewRecords()throws Exception{
  String reviewed=a.db.toString();JSONObject next=new JSONObject(reviewed);next.getJSONObject("settings").put("test",1);a.settings().put("changed",true);a.commitFeatureRecordAsync(next,reviewed,"test",null);assertTrue(a.settings().getBoolean("changed"));assertFalse(RestoreRideGate.isFormWriting());JSONObject incoming=Store.empty();a.confirmImport(incoming,reviewed);ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,a.arr("vehicles").length());assertFalse(RestoreRideGate.isRestoring());
 }
}
