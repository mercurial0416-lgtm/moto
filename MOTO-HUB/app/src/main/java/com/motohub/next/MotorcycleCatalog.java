package com.motohub.next;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline curated catalog plus OPTIONAL NHTSA model-year lookup. No claims of global completeness. */
final class MotorcycleCatalog {
 private static JSONArray cache;
 private MotorcycleCatalog(){}
 static synchronized JSONArray all(Context c) {
  if(cache!=null)return cache;
  try(InputStream in=c.getAssets().open("motorcycle_catalog.json")){
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
   while((n=in.read(buf))!=-1){if(out.size()+n>400000)throw new IOException("catalog too large");out.write(buf,0,n);}
   cache=new JSONObject(out.toString("UTF-8")).getJSONArray("models");
  }catch(Exception ignored){cache=new JSONArray();}
  return cache;
 }
 static String norm(String value){return CatalogSearch.norm(value);}
 static List<String> brands(Context c){TreeSet<String> result=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null)result.add(x.optString("brand"));}return new ArrayList<>(result);}
 static List<String> models(Context c,String brand){TreeSet<String> result=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&brand.equalsIgnoreCase(x.optString("brand")))result.add(x.optString("model"));}return new ArrayList<>(result);}
 /** Offline model -> search aliases for one brand; no network needed. */
 static Map<String,List<String>> modelAliases(Context c,String brand){
  Map<String,List<String>> result=new HashMap<>();JSONArray a=all(c);
  for(int i=0;i<a.length();i++){
   JSONObject x=a.optJSONObject(i);if(x==null||!brand.equalsIgnoreCase(x.optString("brand")))continue;
   JSONArray aliases=x.optJSONArray("aliases");List<String> terms=new ArrayList<>();
   if(aliases!=null)for(int j=0;j<aliases.length();j++)terms.add(aliases.optString(j));
   result.put(x.optString("model"),terms);
  }
  return result;
 }
 static JSONObject lookup(Context c,String brand,String model){String b=norm(brand),m=norm(model);JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null||!norm(x.optString("brand")).equals(b))continue;
   if(norm(x.optString("model")).equals(m))return x;
   JSONArray other=x.optJSONArray("aliases");if(other!=null)for(int j=0;j<other.length();j++)if(norm(other.optString(j)).equals(m))return x;
  }return null;}
 static JSONObject search(Context c,String model){String term=norm(model);if(term.isEmpty())return null;JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;if(norm(x.optString("model")).equals(term)||norm(x.optString("brand")+x.optString("model")).equals(term))return x;
   JSONArray aliases=x.optJSONArray("aliases");if(aliases!=null)for(int j=0;j<aliases.length();j++)if(norm(aliases.optString(j)).equals(term))return x;
  }return null;}
 static String displayName(String brand,String model){return ("Honda".equalsIgnoreCase(brand)&&"Super Cub 110".equalsIgnoreCase(model))?"슈퍼커브 110":model;}
 static int validYear(int y){return y>=1900&&y<=Calendar.getInstance().get(Calendar.YEAR)+2?y:0;}
 static JSONArray readResults(URL url)throws Exception {
  if(!"https".equals(url.getProtocol())||!"vpic.nhtsa.dot.gov".equals(url.getHost()))throw new IOException("Unsupported provider");
  HttpURLConnection con=(HttpURLConnection)url.openConnection();try{con.setConnectTimeout(5000);con.setReadTimeout(7000);con.setRequestProperty("User-Agent","MOTO-HUB/4 MotorcycleCatalogue");
   if(con.getResponseCode()!=200)throw new IOException("Motorcycle catalog unavailable");
   try(InputStream in=con.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>350000)throw new IOException("Response too large");out.write(buf,0,n);}return new JSONObject(out.toString("UTF-8")).optJSONArray("Results");}
  }finally{con.disconnect();}
 }
 /** Expand beyond the bundled manufacturers, without claiming global coverage. */
 static List<String> brandsOnline()throws Exception{
  JSONArray rows=readResults(new URL("https://vpic.nhtsa.dot.gov/api/vehicles/GetMakesForVehicleType/motorcycle?format=json"));
  TreeSet<String> brands=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
  if(rows!=null)for(int i=0;i<rows.length();i++){
   JSONObject item=rows.optJSONObject(i);if(item==null)continue;
   String brand=item.optString("MakeName").trim();if(brand.length()>0&&brand.length()<90)brands.add(brand);
  }
  return new ArrayList<>(brands);
 }
 /** NHTSA is US manufacturer data, may be incomplete for Korean-market bikes. Call off UI thread. */
 static List<String> lookupYearOnline(String brand,int year)throws Exception{
  if(validYear(year)==0||year<1996)throw new IOException("온라인 연식 검색은 1996년 이후만 지원해요.");
  URL url=new URL("https://vpic.nhtsa.dot.gov/api/vehicles/GetModelsForMakeYear/make/"+URLEncoder.encode(brand,"UTF-8").replace("+","%20")+"/modelyear/"+year+"/vehicletype/motorcycle?format=json");
  JSONArray rows=readResults(url);TreeSet<String> names=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
  if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null){String model=row.optString("Model").trim();if(model.length()>=1&&model.length()<=90)names.add(model);}}
  return new ArrayList<>(names);
 }
}
