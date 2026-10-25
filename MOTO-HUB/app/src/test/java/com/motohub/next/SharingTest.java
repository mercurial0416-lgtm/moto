package com.motohub.next;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;import org.robolectric.*;import org.robolectric.annotation.*;import android.location.Location;import org.json.*;import java.time.Instant;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class SharingTest {
 @Before public void reset(){Sharing.enabled=false;Sharing.session="";Sharing.latest=null;RideService.running=false;}
 @After public void done(){Sharing.stop();}
 @Test public void defaultOffNeverBuffersPoints(){Sharing.offer(new Location("gps"));assertNull(Sharing.latest);assertFalse(Sharing.enabled);}
 @Test public void requiresExistingGpsService(){assertThrows(Exception.class,()->Sharing.start("00000000-0000-0000-0000-000000000001"));assertFalse(Sharing.enabled);}
 @Test public void stopDropsLatestAndCancelsTransport(){Sharing.enabled=true;Sharing.control=new SocialClient.Control();SocialClient.Control c=Sharing.control;Sharing.latest=new Location("gps");Sharing.stop();assertFalse(Sharing.enabled);assertNull(Sharing.latest);assertTrue(c.cancelled);}
 @Test public void invalidStateRejected(){assertThrows(IllegalArgumentException.class,()->Sharing.state("public"));}
 JSONObject point(long now)throws Exception{return new JSONObject().put("user_id","TEST").put("lat",37).put("lon",127).put("bearing",12).put("state","rest").put("received_at",Instant.ofEpochMilli(now).toString()).put("expires_at",Instant.ofEpochMilli(now+30000).toString());}
 @Test public void expiredMarkerRejected()throws Exception{long n=System.currentTimeMillis();assertThrows(Exception.class,()->new LivePoint(point(n-31000),n));}
 @Test public void invalidCoordinatesRejected()throws Exception{long n=System.currentTimeMillis();assertThrows(Exception.class,()->new LivePoint(point(n).put("lat",99),n));}
 @Test public void realPointAndUnknownBearing()throws Exception{long n=System.currentTimeMillis();LivePoint p=new LivePoint(point(n).put("bearing",JSONObject.NULL),n);assertEquals("휴식",p.label());assertTrue(Double.isNaN(p.bearing));}
}
