package org.json;
import java.util.HashMap;
import java.util.Map;
/** Tiny test-only stub, not an Android app class; validates pure Java record policies. */
public final class JSONObject {
 private final Map<String,Object> data=new HashMap<>();
 public JSONObject put(String key,Object value){data.put(key,value);return this;}
 public String optString(String key){return optString(key,"");}
 public String optString(String key,String other){Object value=data.get(key);return value==null?other:String.valueOf(value);}
 public boolean optBoolean(String key){Object value=data.get(key);return value instanceof Boolean?(Boolean)value:false;}
 public double optDouble(String key){return optDouble(key,0d);}
 public double optDouble(String key,double other){Object value=data.get(key);return value instanceof Number?((Number)value).doubleValue():other;}
}
