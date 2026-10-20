package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.json.JSONObject;

public final class Ux3ConvenienceTest {
 static int assertions;
 static void ok(boolean v,String why){assertions++;if(!v)throw new AssertionError(why);}
 static final String BIKE="bff56dd0-0b03-4710-bcca-4b7784bd2fe5";
 static final String RECORD="acd936a1-f7de-4001-9402-aabbccddeeff";
 static JSONObject fill(String date,int odo,int amount,double liters,boolean full,String bike){
  return new JSONObject().put("date",date).put("mileage",odo).put("amount",amount).put("liters",liters).put("fullTank",full).put("vehicleId",bike).put("type","주유");
 }
 public static void main(String[] args)throws Exception {
  File root=Files.createTempDirectory("moto-ux3-test").toFile();
  try {
   ok(ReceiptFiles.validId(RECORD),"uuid");ok(!ReceiptFiles.validId("../bad"),"traversal rejected");
   ok(ReceiptFiles.name(RECORD,1).equals("record-receipt-"+RECORD+".jpg"),"legacy 1st name");
   ok(ReceiptFiles.name(RECORD,2).endsWith("-2.jpg"),"new slot");
   ok(ReceiptFiles.recognized(ReceiptFiles.name(RECORD,5)),"fifth recognized");
   ok(!ReceiptFiles.recognized("record-receipt-../../wrong.jpg"),"unsafe name");
   ok(!ReceiptFiles.recognized("record-receipt-"+RECORD+"-6.jpg"),"sixth forbidden");
   ok(ReceiptFiles.nextSlot(root,RECORD)==1,"slot one available");
   for(int i=1;i<=5;i++){
    Files.write(ReceiptFiles.file(root,RECORD,i).toPath(),new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,(byte)0xd9});
    ok(ReceiptFiles.nextSlot(root,RECORD)==(i<5?i+1:-1),"sequential limit "+i);
   }
   ok(ReceiptFiles.existing(root,RECORD).size()==5,"5 photos listed");
   ok(BackupFormat.allowed("receipts/"+ReceiptFiles.name(RECORD,5),false),"backup allow slot5");
   ok(!BackupFormat.allowed("receipts/"+ReceiptFiles.name(RECORD,1)+"/bad",false),"backup traverse blocked");
   ok(BackupFormat.isReceipt("receipts/"+ReceiptFiles.name(RECORD,3)),"receipt routing");
   ReceiptTextParser.Guess parsed=ReceiptTextParser.parse("서울주유소\n2026-10-19 19:20\n주유량: 7.25L\n단가: 1,650\n합계: 11,963원");
   ok(parsed.useful(),"ocr meaningful");ok("2026-10-19".equals(parsed.date),"date");
   ok("11963".equals(parsed.amount.replace(",","")),"total");
   ok("7.25".equals(parsed.liters),"liters");ok("1650".equals(parsed.unitPrice.replace(",","")),"unit price");
   ok(ReceiptTextParser.parse("2026-02-31\n카드 1234567890123456").date.isEmpty(),"bad date ignored");
   ok(ReceiptTextParser.parse(null).amount.isEmpty(),"blank OCR safe");
   boolean[] selected={true,false,true,false,false,false,false,false};
   String[] costs={"10000","0","2500","0","0","0","0","0"};
   String draft=BatchDraft.encode(BIKE,"2026-10-20","5061",true,selected,costs);
   BatchDraft restored=BatchDraft.decode(draft,BIKE);
   ok(restored!=null,"draft restores");ok(restored.selected[2],"choice kept");
   ok("2500".equals(restored.costs[2]),"price kept");ok(restored.intervals,"interval kept");
   ok(BatchDraft.decode(draft,RECORD)==null,"bike isolated");
   ok(BatchDraft.decode(draft.substring(0,18),BIKE)==null,"truncation rejected");
   List<JSONObject> records=new ArrayList<>();
   records.add(fill("2026-10-01",5000,10000,5.0,true,BIKE));
   records.add(fill("2026-10-05",5100,5500,3.0,false,BIKE));
   records.add(fill("2026-10-10",5300,13000,7.0,true,BIKE));
   records.add(fill("2026-10-11",5400,20000,10.0,true,RECORD));
   FuelEfficiency.Result measured=FuelEfficiency.forBike(records,BIKE);
   ok(measured!=null,"full-to-full exists");ok(measured.trips==1,"no contamination");
   ok(Math.abs(measured.kmPerLiter-30.0)<0.001,"300km / 10L");
   ok(Math.abs(measured.costPerKm-61.6666666667)<.01,"18500/300");
   ok(FuelEfficiency.forBike(Collections.singletonList(records.get(0)),BIKE)==null,"one fill insufficient");
   ok(ServiceIntervals.validated("엔진오일",3000)==3000,"starter interval validated");
   ok(ServiceIntervals.validated("엔진오일",0)==0,"invalid interval ignored");
   System.out.println("PASS UX3 policies: "+assertions+" assertions");
  }finally{for(File file:Objects.requireNonNull(root.listFiles()))file.delete();root.delete();}
 }
}
