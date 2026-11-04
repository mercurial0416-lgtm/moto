package com.motohub.next;

import android.view.View;
import android.widget.*;
import java.time.Duration;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowSystemClock;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FuelConvenienceTest {
 MainActivity a;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(MainActivity.class).setup().get();new DashboardDesignTest().seed(a);a.render();new QuickEntryUi(a).fuel();}
 @After public void close(){a.close();a.finish();}
 void save(){ShadowSystemClock.advanceBy(Duration.ofSeconds(2));EntryUiFixture.text(a.formFooter,"저장").performClick();}
 CheckBox full(){return (CheckBox)EntryUiFixture.text(a.formBody,"가득 주유했어요 (연비 계산용)");}
 @Test public void commonAmountAndLiveReceiptSummaryMatchTheSavedActualValues()throws Exception{
  String before=a.db.toString();Ux106NativeUiTest.desc(a.formBody,"주유 금액 2만원 선택").performClick();EntryUiFixture.field(a,"실제 주유량 (L)").setText("10.5");
  String summary=((TextView)EntryUiFixture.tag(a.formFooter,"fuel-save-summary")).getText().toString();assertTrue(summary.contains("20,000원"));assertTrue(summary.contains("10.5 L"));assertEquals(before,a.db.toString());
  EntryUiFixture.capture(a,"fuel-entry-light",393);save();JSONObject last=a.arr("expenses").getJSONObject(a.arr("expenses").length()-1);assertEquals(20000,last.getDouble("amount"),0);assertEquals(10.5,last.getDouble("liters"),0);assertEquals(a.selected,last.getString("vehicleId"));assertEquals(a.today(),last.getString("date"));
 }
 @Test public void visibleLastRecordReusePreservesPrecisionAndCurrentDateOdometerAndTankChoice()throws Exception{
  a.close();a.arr("expenses").getJSONObject(0).put("amount",23500.5).put("liters",12.55).put("mileage",10000).put("fullTank",true);new QuickEntryUi(a).fuel();
  View reuse=EntryUiFixture.text(a.formBody,"지난 주유 금액·리터 불러오기");assertTrue(EntryUiFixture.shown(reuse));reuse.performClick();
  assertEquals("23500.5",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());assertEquals("12.55",EntryUiFixture.field(a,"실제 주유량 (L)").getText().toString());assertEquals("12480",EntryUiFixture.field(a,"현재 계기판 (km)").getText().toString());assertFalse(full().isChecked());assertTrue(((TextView)EntryUiFixture.tag(a.formBody,"fuel-date-odometer")).getText().toString().contains(a.today()));
 }
 @Test public void manualPaymentIsPreservedWhenOpeningTheCalculatorBeforeAnyAutomaticTotal(){
  EntryUiFixture.field(a,"주유 금액 (원)").setText("20100");EntryUiFixture.field(a,"실제 주유량 (L)").setText("10");EntryUiFixture.text(a.formBody,"리터당 가격으로 자동 계산").performClick();EntryUiFixture.field(a,"리터당 가격 (원)").setText("2000");assertEquals("20100",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());
  EntryUiFixture.field(a,"주유 금액 (원)").setText("");EntryUiFixture.field(a,"리터당 가격 (원)").setText("2100");assertEquals("21000",EntryUiFixture.field(a,"주유 금액 (원)").getText().toString());
 }
 @Test public void missingValueFocusesItsFieldAndDoesNotWrite(){
  EntryUiFixture.field(a,"주유 금액 (원)").setText("20000");String before=a.db.toString();save();assertEquals(before,a.db.toString());assertNotNull(EntryUiFixture.field(a,"실제 주유량 (L)").getError());assertTrue(a.dialog.isShowing());
 }
 @Test public void fullTankStillRequiresExplicitRealOdometerConfirmation(){
  EntryUiFixture.field(a,"주유 금액 (원)").setText("20000");EntryUiFixture.field(a,"실제 주유량 (L)").setText("10");full().setChecked(true);String before=a.db.toString();save();assertEquals(before,a.db.toString());
  CheckBox confirmation=(CheckBox)EntryUiFixture.text(a.formBody,"실제 계기판 12480 km를 확인했어요");confirmation.setChecked(true);EntryUiFixture.field(a,"현재 계기판 (km)").setText("12500");assertFalse(confirmation.isChecked());save();assertEquals(before,a.db.toString());
 }
 @Test public void fuelSummaryKeepsFractionalLitersAcrossLargeDarkTextAndLedger()throws Exception{
  a.close();a.arr("expenses").getJSONObject(0).put("liters",12.55);AccessibleNavigationTest.configure(a,320,1.5f);a.settings().put("theme","dark");a.render();new QuickEntryUi(a).fuel();EntryUiFixture.field(a,"주유 금액 (원)").setText("23500");EntryUiFixture.field(a,"실제 주유량 (L)").setText("12.55");EntryUiFixture.capture(a,"fuel-entry-dark-large",320);
  TextView summary=(TextView)EntryUiFixture.tag(a.formFooter,"fuel-save-summary");assertTrue(summary.getText().toString().contains("12.55 L"));assertTrue(CommercialUiTest.contrast(summary.getCurrentTextColor(),a.bg)>=4.5);
  a.close();a.navigate("차고");View timeline=EntryUiFixture.tag(a.root,"ledger-timeline-entry");assertNotNull(timeline);assertTrue(EntryUiFixture.contains(timeline,"12.55 L"));
 }
}
