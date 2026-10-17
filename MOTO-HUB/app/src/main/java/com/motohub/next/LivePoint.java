package com.motohub.next;
import org.json.*;import java.time.Instant;
final class LivePoint {
 final String user,state,time;final double lat,lon,bearing;final long expires;
 LivePoint(JSONObject o,long now)throws Exception{user=o.getString("user_id");lat=o.getDouble("lat");lon=o.getDouble("lon");bearing=o.isNull("bearing")?Double.NaN:o.getDouble("bearing");state=o.getString("state");time=o.getString("received_at");expires=Instant.parse(o.getString("expires_at")).toEpochMilli();long received=Instant.parse(time).toEpochMilli();if(!TrackFile.valid(lat,lon,0,received)||(!o.isNull("bearing")&&(!Double.isFinite(bearing)||bearing<0||bearing>=360))||expires<=now||expires>received+30000||received>now+5000)throw new IllegalArgumentException("만료되었거나 유효하지 않은 위치");}
 String label(){return state.equals("rest")?"휴식":state.equals("fuel")?"주유":state.equals("stopped")?"정차":"주행";}
}
