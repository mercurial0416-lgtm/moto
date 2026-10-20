package com.motohub.next;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;import org.robolectric.*;import org.robolectric.annotation.*;import android.content.*;import android.location.*;import android.os.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class RideServiceTest {
 @Before public void reset(){RideService.running=false;RideService.active="";RideService.count=0;}
 @Test public void missingPermissionDoesNotStart()throws Exception{RideService s=Robolectric.buildService(RideService.class).create().get();Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION);assertEquals(android.app.Service.START_NOT_STICKY,s.onStartCommand(new Intent(),0,1));assertFalse(RideService.running);assertEquals(0,TrackFile.tracks(s.getFilesDir()).size());s.onDestroy();}
 @Test public void stopCommandNeverStartsTracking(){RideService s=Robolectric.buildService(RideService.class).create().get();s.onStartCommand(new Intent().setAction(RideService.STOP),0,1);assertFalse(RideService.running);assertEquals("",RideService.active);s.onDestroy();}
 @Test public void stoppedServiceIgnoresLocation(){RideService s=Robolectric.buildService(RideService.class).create().get();Location p=new Location("gps");p.setLatitude(37);p.setLongitude(127);p.setAccuracy(1);p.setTime(System.currentTimeMillis());p.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());s.onLocationChanged(p);assertEquals(0,RideService.count);s.onDestroy();}
 @Test public void explicitStartRecordsAndStopRemovesListener()throws Exception{
  Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.POST_NOTIFICATIONS);
  RideService s=Robolectric.buildService(RideService.class).create().get();LocationManager lm=s.getSystemService(LocationManager.class);Shadows.shadowOf(lm).setProviderEnabled(LocationManager.GPS_PROVIDER,true);
  s.onStartCommand(new Intent(),0,1);assertTrue(RideService.status,RideService.running);assertFalse(Shadows.shadowOf(lm).getLocationUpdateListeners().isEmpty());
  Location p=new Location("gps");p.setLatitude(37);p.setLongitude(127);p.setAccuracy(5);p.setTime(System.currentTimeMillis());p.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());s.onLocationChanged(p);assertEquals(1,RideService.count);
  java.io.File f=s.track;assertEquals(1,TrackFile.read(f,(a,b,c,d)->{}));
  s.onStartCommand(new Intent().setAction(RideService.STOP),0,2);
  // Location listeners must stop immediately, but final .done verification is asynchronous.
  assertTrue(Shadows.shadowOf(lm).getLocationUpdateListeners().isEmpty());
  long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(6);
  while(RideService.running&&System.nanoTime()<deadline){
   Shadows.shadowOf(Looper.getMainLooper()).idle();
   Thread.sleep(10);
  }
  Shadows.shadowOf(Looper.getMainLooper()).idle();
  assertFalse("Completed track should finish after background validation",RideService.running);
  assertTrue("A verified user STOP creates the completion marker",new java.io.File(f.getPath()+".done").isFile());
  s.onLocationChanged(p);assertEquals(1,TrackFile.read(f,(a,b,c,d)->{}));s.onDestroy();
 }
}
