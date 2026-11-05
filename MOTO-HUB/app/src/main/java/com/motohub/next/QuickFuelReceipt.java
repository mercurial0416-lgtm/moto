package com.motohub.next;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.*;

/** OCR proposal only: explicit review, active form/bike identity and input freshness are required. */
final class QuickFuelReceipt {
 private final MainActivity a;private final String bike;private final Dialog owner;
 private final EditText amount,liters,unitPrice,date;private final LinearLayout calculator;private final FuelAutoAmount math;
 QuickFuelReceipt(MainActivity a,String bike,Dialog owner,EditText amount,EditText liters,EditText unitPrice,EditText date,LinearLayout calculator,FuelAutoAmount math){this.a=a;this.bike=bike;this.owner=owner;this.amount=amount;this.liters=liters;this.unitPrice=unitPrice;this.date=date;this.calculator=calculator;this.math=math;}
 private boolean alive(){return !a.isFinishing()&&!a.isDestroyed()&&!a.readOnly&&a.dialog==owner&&owner.isShowing()&&a.bike()!=null&&bike.equals(a.bike().optString("id"));}
 void choose(){if(!alive())return;a.quickFuelReceipt=this;Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{a.startActivityForResult(pick,94);}catch(Exception unavailable){a.toast("사진 선택 앱을 열지 못했습니다. 직접 입력해 주세요.");}}
 void receive(Uri uri){if(!alive()||uri==null)return;a.toast("영수증을 기기에서 읽는 중… 저장 전 확인해 주세요.");ReceiptOcr.scan(a,uri,new ReceiptOcr.Callback(){public void failure(String message){if(alive())a.error(message);}public void success(String text){if(alive())propose(ReceiptTextParser.parse(text));}});}
 void propose(ReceiptTextParser.Result proposal){
  if(!alive())return;if(!proposal.hasAny()&&proposal.unitPrice.isEmpty()){a.error("인식할 수 있는 주유 값이 없습니다. 직접 입력해 주세요.");return;}
  if(proposal.amountCandidates.size()>1){String[] values=proposal.amountCandidates.toArray(new String[0]);new AlertDialog.Builder(a).setTitle("실제 결제금액 선택").setItems(values,(d,index)->confirm(proposal,values[index])).setNegativeButton("취소",null).show();}else confirm(proposal,proposal.amount);
 }
 private String inputs(){return amount.getText()+"\0"+liters.getText()+"\0"+unitPrice.getText()+"\0"+date.getText();}
 private void confirm(ReceiptTextParser.Result proposal,String paid){
  if(!alive())return;final String reviewed=inputs();String message=(paid.isEmpty()?"":"결제 금액 "+paid+"원\n")+(proposal.liters.isEmpty()?"":"주유량 "+proposal.liters+" L\n")+(proposal.unitPrice.isEmpty()?"":"단가 "+proposal.unitPrice+"원/L\n")+(proposal.date.isEmpty()?"":"날짜 "+proposal.date+"\n")+"\n선택한 값이 입력칸을 바꿉니다. 실제 영수증과 대조하세요. 계기판과 가득 주유 여부는 바꾸지 않으며 저장은 별도입니다.";
  new AlertDialog.Builder(a).setTitle("영수증 인식값 확인").setMessage(message).setNegativeButton("취소",null).setPositiveButton("입력칸에 적용",(d,w)->{
   if(!alive()||!reviewed.equals(inputs())){a.toast("입력 내용이 바뀌었습니다. 영수증 값을 다시 확인해 주세요.");return;}
   calculator.setVisibility(View.GONE);math.reset();if(!paid.isEmpty())amount.setText(paid);math.noteAmountEdited(amount.getText().toString());if(!proposal.liters.isEmpty())liters.setText(proposal.liters);if(!proposal.unitPrice.isEmpty())unitPrice.setText(proposal.unitPrice);if(!proposal.date.isEmpty())date.setText(proposal.date);a.toast("입력칸에 적용했습니다. 계기판과 날짜 확인 후 저장하세요.");
  }).show();
 }
}
