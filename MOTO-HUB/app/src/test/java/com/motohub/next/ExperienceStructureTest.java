package com.motohub.next;

import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Navigation and information architecture, beyond palette snapshots. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class ExperienceStructureTest {
 MainActivity a;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(MainActivity.class).setup().get();new DashboardDesignTest().seed(a);a.render();}
 @After public void cleanup(){a.close();a.finish();}
 static View tagged(View root,String tag){if(tag.equals(root.getTag()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=tagged(group.getChildAt(i),tag);if(found!=null)return found;}}return null;}
 @Test public void vehicleStageAndPersistentPrimaryActionAreDifferentRegions()throws Exception{
  CommercialUiTest.capture(a,"experience-home",393,852);
  View stage=tagged(a.root,"vehicle-stage"),dock=tagged(a.root,"primary-action-dock");assertNotNull(stage);assertNotNull(dock);
  View photo=Ux106NativeUiTest.desc(a.root,"바이크 사진 변경");assertEquals(stage.getWidth(),photo.getWidth());
  assertNotSame(a.content,dock.getParent());assertTrue(dock.getTop()>0);assertTrue(dock.getBottom()<=a.root.getHeight());
  assertNotNull(Ux106NativeUiTest.text(dock,"라이딩 시작"));assertNotNull(tagged(a.root,"monthly-spending"));
 }
 @Test public void vehicleMileageAndUnitStayReadableAtDoubleTextSize()throws Exception{
  AccessibleNavigationTest.configure(a,320,2f);a.render();CommercialUiTest.capture(a,"experience-home-double-text",320,852);
  LinearLayout footer=(LinearLayout)Ux106NativeUiTest.desc(a.root,"계기판 누적거리 12,480 km · 직접 갱신");assertNotNull(footer);assertEquals(LinearLayout.VERTICAL,footer.getOrientation());
  TextView mileage=(TextView)footer.getChildAt(0);assertEquals(1,mileage.getLineCount());assertTrue(mileage.getLayout().getLineWidth(0)<=mileage.getWidth()-mileage.getCompoundPaddingLeft()-mileage.getCompoundPaddingRight()+1);
  assertTrue(mileage.getLayout().getHeight()<=mileage.getHeight());
 }
 @Test public void rideCockpitAndHistoryAreSeparateWorkspacesWithoutChangingTheDatabase(){
  a.navigate("라이딩");String original=a.db.toString();assertNotNull(tagged(a.root,"ride-instrument"));
  Ux106NativeUiTest.text(a.root,"라이딩 기록").performClick();assertEquals("ride-history-workspace",a.content.getTag());assertNull(tagged(a.root,"ride-instrument"));
  Ux106NativeUiTest.text(a.root,"주행하기").performClick();assertNotNull(tagged(a.root,"ride-instrument"));assertEquals(original,a.db.toString());
 }
 @Test public void activeRideKeepsConfirmedStopInTheFixedDockAndFocusHidesTheInstrument(){
  boolean running=RideService.running,armed=RideService.autoArmed;
  try{
   RideService.running=true;RideService.autoArmed=true;a.navigate("라이딩");
   View dock=tagged(a.root,"primary-action-dock");assertNotNull(Ux106NativeUiTest.desc(dock,"현재 GPS 기록을 저장 후 종료"));
   assertNotNull(Ux106NativeUiTest.text(a.root,"OBD만 중지 · GPS 기록은 계속"));
   Ux106NativeUiTest.text(a.root,"주행 집중 모드").performClick();assertEquals(View.GONE,tagged(a.root,"ride-instrument").getVisibility());
   Ux106NativeUiTest.text(tagged(a.root,"primary-action-dock"),"라이딩 저장 후 종료").performClick();
   android.app.Dialog confirmation=org.robolectric.shadows.ShadowDialog.getLatestDialog();
   assertNotNull(confirmation);
   View modal=confirmation.getWindow().getDecorView();
   assertNotNull(Ux106NativeUiTest.text(modal,"저장하고 종료"));assertTrue(RideService.running);
   Ux106NativeUiTest.text(modal,"취소").performClick();assertTrue(RideService.running);
  }finally{RideService.running=running;RideService.autoArmed=armed;}
 }
 @Test public void ledgerSeparatesTimelineSchedulingAndEntryTools(){
  a.navigate("차고");String original=a.db.toString();assertNotNull(tagged(a.root,"ledger-timeline-entry"));assertNull(Ux106NativeUiTest.text(a.root,"간편 주유"));
  Ux106NativeUiTest.text(a.root,"기록 추가  +").performClick();assertNotNull(Ux106NativeUiTest.text(a.dialog.getWindow().getDecorView(),"간편 주유"));a.close();
  Ux106NativeUiTest.text(a.root,"정비 일정").performClick();assertTrue(a.ledgerSchedulePage);assertNull(tagged(a.root,"ledger-timeline-entry"));
  Ux106NativeUiTest.text(a.root,"타임라인").performClick();assertNotNull(tagged(a.root,"ledger-timeline-entry"));assertEquals(original,a.db.toString());
 }
 @Test public void managementHubHasDirectDestinationsAndBackReturnsFromDetailedSettings(){
  a.navigate("더보기");String original=a.db.toString();
  for(String label:new String[]{"바이크 관리","홈 화면 위젯","백업 · 복원","유지비 통계"})assertTrue(tagged(a.root,"destination-tile:"+label).isClickable());
  Ux106NativeUiTest.text(a.root,"전체 설정  ›").performClick();assertTrue(a.settingsDetailPage);assertNotNull(Ux106NativeUiTest.text(a.root,"정비 알림"));
  a.back();assertFalse(a.settingsDetailPage);assertEquals("더보기",a.tab);assertNotNull(tagged(a.root,"destination-tile:홈 화면 위젯"));assertEquals(original,a.db.toString());
 }
}
