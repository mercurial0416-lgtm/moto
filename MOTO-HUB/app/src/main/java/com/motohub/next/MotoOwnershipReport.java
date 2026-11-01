package com.motohub.next;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.util.*;
import org.json.*;

/** User-initiated SAF PDF: maintenance/sale handover, never an official inspection. */
final class MotoOwnershipReport {
 static final long MAX_PDF_BYTES=12_000_000L;
 private static final int WIDTH=595,HEIGHT=842,MARGIN=42,LINE=20;
 static final class Report {
  final String bikeName;
  final List<String> heading,records;
  final int completedServiceCount,fuelCount,otherCount,totalCount;
  final double serviceCost,fuelCost,otherCost;
  Report(String bikeName,List<String> heading,List<String> records,
         int completedServiceCount,int fuelCount,int otherCount,
         double serviceCost,double fuelCost,double otherCost,int totalCount){
   this.bikeName=bikeName;this.heading=heading;this.records=records;
   this.completedServiceCount=completedServiceCount;this.fuelCount=fuelCount;this.otherCount=otherCount;
   this.serviceCost=serviceCost;this.fuelCost=fuelCost;this.otherCost=otherCost;this.totalCount=totalCount;
  }
 }
 private static final class Item {
  final String date,type,title,description;
  Item(String date,String type,String title,String description){
   this.date=date;this.type=type;this.title=title;this.description=description;
  }
 }
 private MotoOwnershipReport(){}
 static String clean(String s,int max){
  if(s==null)return "";
  String filtered=s.replaceAll("[\\p{Cntrl}]+"," ").trim();
  return filtered.length()>max?filtered.substring(0,max)+"…":filtered;
 }
 static String money(double amount){
  return String.format(Locale.KOREA,"%,.0f원",amount);
 }
 static Report prepare(JSONObject snapshot,String bikeId)throws IOException{
  if(snapshot==null||!RideBikeBinding.valid(bikeId))throw new IOException("차량 정보가 없습니다.");
  JSONObject bike;
  try{bike=Store.find(snapshot,"vehicles",bikeId);}
  catch(Exception e){throw new IOException("차고 데이터를 확인할 수 없습니다.",e);}
  if(bike==null)throw new IOException("선택한 바이크를 찾지 못했습니다.");
  String name=clean(bike.optString("name"),54);
  if(name.isEmpty())throw new IOException("바이크 이름이 없습니다.");
  List<String> head=new ArrayList<>();
  head.add(name);
  head.add(bike.optInt("year")+"년식  ·  계기판 기록 "+String.format(Locale.KOREA,"%,.0f km",bike.optDouble("odo")));
  if(!bike.optString("purchaseDate").isEmpty())head.add("등록된 구매일 "+clean(bike.optString("purchaseDate"),20));
  if(!bike.optString("soldDate").isEmpty())head.add("등록된 판매일 "+clean(bike.optString("soldDate"),20));
  List<Item> items=new ArrayList<>();
  int service=0,fuel=0,other=0;double sc=0,fc=0,oc=0;
  for(String collection:new String[]{"services","expenses"}){
   JSONArray rows=snapshot.optJSONArray(collection);
   if(rows==null)continue;
   for(int i=0;i<rows.length();i++){
    JSONObject record=rows.optJSONObject(i);
    if(record==null||!bikeId.equals(record.optString("vehicleId")))continue;
    boolean schedule="services".equals(collection)&&record.optBoolean("scheduleOnly",false);
    if(schedule)continue; // A scheduled task is NOT proof of completed maintenance.
    String date=record.optString("date");
    if(MotoTimeline.startOfDay(date)<0)continue;
    String type="services".equals(collection)?"정비":"주유".equals(record.optString("type"))?"주유":"기타";
    double amount=record.optDouble("services".equals(collection)?"cost":"amount",Double.NaN);
    if(!Double.isFinite(amount)||amount<0||amount>1_000_000_000)continue;
    if("정비".equals(type)){service++;sc+=amount;}
    else if("주유".equals(type)){fuel++;fc+=amount;}
    else{other++;oc+=amount;}
    String title=clean(record.optString("type"),42);
    if(title.isEmpty())title=type;
    String mileage=record.has("mileage")?String.format(Locale.KOREA," · %,.0f km",record.optDouble("mileage")):"";
    String note=clean(record.optString("note"),120);
    String description=money(amount)+mileage+(note.isEmpty()?"":" · "+note);
    items.add(new Item(date,type,title,description));
   }
  }
  items.sort((x,y)->{int c=y.date.compareTo(x.date);return c!=0?c:x.type.compareTo(y.type);});
  List<String> lines=new ArrayList<>();
  int max=Math.min(160,items.size());
  for(int i=0;i<max;i++){
   Item item=items.get(i);
   lines.add(item.date+"   ["+item.type+"] "+item.title);
   lines.add("   "+item.description);
  }
  return new Report(name,head,lines,service,fuel,other,sc,fc,oc,items.size());
 }
 private static void draw(Canvas canvas,Paint paint,String text,float x,float y,int size,int color,boolean bold){
  paint.setColor(color);paint.setTextSize(size);
  paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
  canvas.drawText(text,x,y,paint);
 }
 private static List<String> wrap(String raw,Paint paint,int size,int maxWidth){
  String clean=clean(raw,240);
  List<String> result=new ArrayList<>();
  if(clean.isEmpty())return result;
  paint.setTextSize(size);
  StringBuilder line=new StringBuilder();
  for(int i=0;i<clean.length();i++){
   char c=clean.charAt(i);
   if(paint.measureText(line.toString()+c)>maxWidth&&line.length()>0){
    result.add(line.toString());line.setLength(0);
   }
   line.append(c);
  }
  if(line.length()>0)result.add(line.toString());
  return result;
 }
 static int write(Report report,OutputStream output)throws IOException{
  if(report==null||output==null)throw new IOException("PDF 보고서 대상 오류");
  PdfDocument pdf=new PdfDocument();
  Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
  PdfDocument.Page page=null;Canvas canvas=null;int y=0,pageNumber=0;
  try{
   List<String> lines=new ArrayList<>();
   lines.add("MOTO HUB  ·  차량 관리 내역");
   lines.addAll(report.heading);
   lines.add("정비 "+report.completedServiceCount+"회  "+money(report.serviceCost)+
     "  |  주유 "+report.fuelCount+"회  "+money(report.fuelCost));
   lines.add("기타 "+report.otherCount+"회  "+money(report.otherCost));
   lines.add("사용자 입력 기록이며 공식 점검·무사고·주행거리 인증 문서가 아닙니다.");
   lines.add("GPS 좌표, 라이더 연락처, 계정 정보는 포함하지 않습니다.");
   lines.add(" ");
   lines.add("주유·정비·비용 이력  ·  최신 "+Math.min(report.totalCount,160)+"/"+report.totalCount+"건");
   lines.addAll(report.records);
   lines.add(" ");
   lines.add("본 보고서는 MOTO HUB 기기에 보관된 사용자 입력 자료로 작성됨");
   for(String source:lines){
    boolean title=source.startsWith("MOTO HUB");
    boolean warning=source.startsWith("사용자 입력");
    int size=title?19:warning?10:source.startsWith("정비 ")?12:11;
    int color=warning?0xffA24B20:0xff1B242C;
    List<String> wrapped=wrap(source,pen,size,WIDTH-2*MARGIN);
    if(wrapped.isEmpty())wrapped=Collections.singletonList(" ");
    for(String display:wrapped){
     if(page==null||y+LINE>HEIGHT-52){
      if(page!=null){draw(canvas,pen,"MOTO HUB  ·  "+pageNumber,WIDTH-MARGIN-116,HEIGHT-27,9,0xff65717D,false);pdf.finishPage(page);}
      page=pdf.startPage(new PdfDocument.PageInfo.Builder(WIDTH,HEIGHT,++pageNumber).create());
      canvas=page.getCanvas();canvas.drawColor(Color.WHITE);y=MARGIN+23;
     }
     draw(canvas,pen,display,MARGIN,y,size,color,title);y+=title?32:LINE;
    }
   }
   if(page!=null){draw(canvas,pen,"MOTO HUB  ·  "+pageNumber,WIDTH-MARGIN-116,HEIGHT-27,9,0xff65717D,false);pdf.finishPage(page);}
   pdf.writeTo(output);
   return pageNumber;
  }catch(Exception e){throw new IOException("차량 이력 PDF 생성 실패: "+e.getMessage(),e);}
  finally{pdf.close();}
 }
}
