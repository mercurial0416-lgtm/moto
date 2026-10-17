import com.motohub.next.Store;
import org.json.*;
public class FuelTest {
 static int passed=0;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println("PASS "+name);passed++;}
 static JSONObject d()throws Exception{JSONObject d=Store.empty();d.getJSONArray("vehicles").put(new JSONObject("{\"id\":\"b\",\"name\":\"test\"}"));return d;}
 static void fill(JSONObject d,String id,String date,double km,double liters,boolean full)throws Exception{d.getJSONArray("expenses").put(new JSONObject().put("id",id).put("vehicleId","b").put("type","주유").put("date",date).put("mileage",km).put("liters",liters).put("amount",10000).put("fullTank",full));}
 public static void main(String[] args)throws Exception{
  JSONObject d=d();check(Store.fuelStats(d,"b").getInt("segments")==0,"no data no estimate");
  fill(d,"1","2026-10-01",1000,8,true);check(Store.fuelStats(d,"b").getInt("segments")==0,"one full is not enough");
  fill(d,"2","2026-10-02",1120,4,false);fill(d,"3","2026-10-03",1300,6,true);JSONObject s=Store.fuelStats(d,"b");check(s.getDouble("kmPerLiter")==30,"partial fills included and baseline fuel excluded");
  fill(d,"4","2026-10-04",1500,10,true);s=Store.fuelStats(d,"b");check(s.getDouble("kmPerLiter")==25&&s.getInt("segments")==2,"weighted fuel efficiency");check(Store.fuelStats(d,"other").getInt("segments")==0,"vehicle isolation");
  JSONObject missing=d();fill(missing,"a","2026-10-01",1000,8,true);fill(missing,"b","2026-10-02",1100,0,false);fill(missing,"c","2026-10-03",1300,10,true);check(Store.fuelStats(missing,"b").getInt("segments")==0,"unknown fuel invalidates segment");
  JSONObject bad=d();fill(bad,"a","2026-10-01",1000,8,true);fill(bad,"b","2026-10-02",900,8,true);check(Store.fuelStats(bad,"b").getInt("segments")==0,"decreasing odometer rejected");
  JSONObject old=d();fill(old,"a","2026-10-01",1000,8,false);fill(old,"b","2026-10-02",1300,8,false);check(Store.fuelStats(old,"b").getInt("segments")==0,"legacy fills not assumed full");
  JSONObject same=d();fill(same,"a","2026-10-01",1000,8,true);fill(same,"b","2026-10-02",1000,8,true);check(Store.fuelStats(same,"b").getInt("segments")==0,"zero distance ignored");
  check(Store.parse(d.toString()).getJSONArray("expenses").getJSONObject(0).getBoolean("fullTank"),"fullTank survives backup");
  System.out.println("TOTAL "+passed+" passed");
 }
}
