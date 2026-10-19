package com.motohub.next;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;import org.robolectric.*;import org.robolectric.annotation.*;import android.content.*;import android.location.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class SharingPermissionTest {
 RideService service;
 @Before public void start(){Sharing.enabled=false;Sharing.session="";RideService.running=false;Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.POST_NOTIFICATIONS);service=Robolectric.buildService(RideService.class).create().get();LocationManager lm=service.getSystemService(LocationManager.class);Shadows.shadowOf(lm).setProviderEnabled(LocationManager.GPS_PROVIDER,true);service.onStartCommand(new Intent(),0,1);assertTrue(RideService.running);Sharing.enabled=true;}
 @After public void finish(){service.onDestroy();}
 @Test public void locationRevokedStopsGpsAndSharing(){Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION);service.permissionCheck.run();assertFalse(RideService.running);assertFalse(Sharing.enabled);}
 @Test public void notificationPermissionRevokedStopsSharing(){Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS);service.permissionCheck.run();assertFalse(Sharing.enabled);}
 @Test public void disabledGpsStopsSharing(){service.onProviderDisabled(LocationManager.GPS_PROVIDER);assertTrue(RideService.running);assertFalse(Sharing.enabled);}
 @Test public void serviceDestroyDropsPendingPoint(){Sharing.offer(new Location("gps"));service.onDestroy();assertFalse(Sharing.enabled);assertNull(Sharing.latest);}
 @Test public void cancelledRequestCannotReachTransport(){SocialClient.Control c=new SocialClient.Control();c.cancel();assertThrows(Exception.class,()->SocialClient.INSTANCE.call("POST","/rest/v1/rpc/mh_share_publish",new org.json.JSONObject(),true,c));}
}
