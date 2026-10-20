package com.motohub.next;
import java.io.*;import java.nio.file.*;import java.util.*;
import org.json.JSONObject;
public final class ReceiptUx3Test {
 static int checks=0;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static final String ID="12345678-1234-1234-1234-123456789abc";
 static JSONObject fuel(String bike,double odo,double liters,double cost,boolean full){return new JSONObject().put("vehicleId",bike).put("type","주유").put("mileage",odo).put("liters",liters).put("amount",cost).put("fullTank",full).put("date","2026-10-20");}
 public static void main(String[] a)throws Exception {
  File dir=Files.createTempDirectory("moto-ux3-receipts").toFile();try{
   ok(ReceiptSlots.validId(ID),"valid id");ok(!ReceiptSlots.validId("../../hack"),"path traversal");
   ok(ReceiptSlots.name(ID,1).equals("record-receipt-"+ID+".jpg"),"legacy name");
   ok(ReceiptSlots.name(ID,4).equals("record-receipt-"+ID+"-4.jpg"),"fourth slot");
   for(int i=1;i<=4;i++){ok(ReceiptSlots.nextFree(dir,ID)==i,"next slot");Files.write(ReceiptSlots.file(dir,ID,i).toPath(),new byte[]{(byte)255,(byte)216,4,(byte)255,(byte)217});}
   ok(ReceiptSlots.count(dir,ID)==4,"all four receipts");ok(ReceiptSlots.nextFree(dir,ID)==0,"limit");ok(ReceiptSlots.existing(dir,ID).size()==4,"listing");
   for(int i=1;i<=4;i++)ok(BackupFormat.isReceipt("receipts/"+ReceiptSlots.name(ID,i)),"backup allow slot "+i);
   ok(!BackupFormat.isReceipt("receipts/record-receipt-"+ID+"-5.jpg"),"reject slot five");
   ok(!BackupFormat.isReceipt("receipts/../../"+ID+".jpg"),"reject traversal");
   ok(BackupFormat.MAX_RECEIPTS>=4000,"reasonable bounded limit");
  }finally{for(File f:Objects.requireNonNull(dir.listFiles()))Files.deleteIfExists(f.toPath());Files.deleteIfExists(dir.toPath());}
  Map<String,String> costs=new LinkedHashMap<>();costs.put("엔진오일","15000");costs.put("체인","0");
  String raw=BatchDraft.encode(ID,"2026-10-20","4523.5",costs,true);
  ok(!raw.isEmpty(),"encode");Properties p=BatchDraft.decode(raw,ID);
  ok(p.getProperty("cost.엔진오일").equals("15000"),"cost");ok(p.getProperty("cost.체인").equals("0"),"diy zero cost");
  ok(BatchDraft.decode(raw,"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa").isEmpty(),"bike isolation");
  ok(BatchDraft.decode("x".repeat(10_000),ID).isEmpty(),"bounded data");
  costs.put("private","token");ok(BatchDraft.encode(ID,"2026-10-20","2",costs,false).isEmpty(),"unknown item excluded");
  ReceiptTextHints h=ReceiptTextHints.parse("주유소\\n2026.10.20\\n주유량: 5.23L\\n결제금액 11,500원".replace("\\n","\n"));
  ok(h.recognized,"ocr hints");ok("2026-10-20".equals(h.date),"ocr date");ok("11500".equals(h.amount),"ocr amount");ok("5.23".equals(h.liters),"ocr liters");
  ok(!ReceiptTextHints.parse("광고번호: 12345678").recognized,"unstructured not guessed");
  ok(!"999".equals(ReceiptTextHints.parse("결제금액 999999999원").amount),"reject huge invoice");
  String bike=ID,other="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  List<JSONObject> list=Arrays.asList(fuel(bike,1000,8,16000,true),fuel(bike,1100,7,14000,false),fuel(bike,1200,8,16000,true),fuel(other,2000,50,100000,true),new JSONObject().put("vehicleId",bike).put("type","보험").put("amount",50000));
  FuelTrend.Summary f=FuelTrend.forBike(list,bike);
  ok(f.entries==3,"only bike fuel entries");ok(f.fullTankSegments==1,"complete segment");
  ok(Math.abs(f.fullTankKmPerLiter-(200d/15d))<0.01,"weighted economy");ok(Math.abs(f.unitPrice-2000)<0.01,"average price");
  ok(FuelTrend.forBike(list,other).fullTankSegments==0,"need two full tanks");
  System.out.println("PASS UX3 receipt, draft, OCR hint and fuel trend: "+checks+" assertions");
 }
}
