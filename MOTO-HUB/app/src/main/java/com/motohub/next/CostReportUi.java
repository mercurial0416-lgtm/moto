package com.motohub.next;

import android.widget.*;
import android.app.Dialog;
import android.app.AlertDialog;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Period selection and side-by-side per-bike figures, separated from screen routing. */
final class CostReportUi {
 private final MainActivity a;CostReportUi(MainActivity a){this.a=a;}
 void open(){
  LinearLayout page=a.page("기간별 유지비 · 차량 비교");LocalDate today=LocalDate.parse(a.today());
  String[] range={today.withDayOfMonth(1).minusMonths(5).toString(),today.toString()};
  TextView period=a.text(range[0]+" – "+range[1],14,a.muted,false);page.addView(period);a.gap(page,16);
  LinearLayout results=a.col();page.addView(results);
  a.label(page,"예정·미래 비용 제외. km당 비용은 입력한 계기판 기록 구간의 참고값으로 전체 기간 실주행거리·GPS 거리와 다를 수 있습니다.",13,a.muted,false);
  a.action(a.formFooter,"기간 변경",false,()->choosePeriod(range,period,results));a.show();load(range[0],range[1],results);
 }
 private void choosePeriod(String[] range,TextView period,LinearLayout results){
  Dialog owner=a.dialog;LinearLayout fields=a.col();fields.setPadding(a.dp(20),a.dp(12),a.dp(20),0);
  EditText from=FeatureUi.day(a,fields,"시작일",range[0]),to=FeatureUi.day(a,fields,"종료일",range[1]);LocalDate today=LocalDate.parse(a.today());
  a.pair(fields,"이번 달",()->{from.setText(today.withDayOfMonth(1).toString());to.setText(today.toString());},"최근 12개월",()->{from.setText(today.withDayOfMonth(1).minusMonths(11).toString());to.setText(today.toString());});
  AlertDialog picker=new AlertDialog.Builder(a).setTitle("비교 기간").setView(fields).setNegativeButton("취소",null).setPositiveButton("적용",null).create();
  picker.setOnShowListener(d->picker.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   if(a.dialog!=owner||owner==null||!owner.isShowing()){picker.dismiss();return;}
   String start=from.getText().toString(),end=to.getText().toString();
   try{if(LocalDate.parse(end).isBefore(LocalDate.parse(start)))throw new IllegalArgumentException();}catch(Exception invalid){from.setError("시작일과 종료일을 확인하세요.");return;}
   range[0]=start;range[1]=end;period.setText(start+" – "+end);load(start,end,results);picker.dismiss();
  }));picker.show();
 }
 private void load(String start,String end,LinearLayout results){
  final LocalDate from,to;try{from=LocalDate.parse(start);to=LocalDate.parse(end);if(to.isBefore(from))throw new IllegalArgumentException();}catch(Exception invalid){a.error("시작일과 종료일을 확인하세요.");return;}
  final String snapshot=a.db.toString();final Dialog owner=a.dialog;Object request=new Object();results.setTag(request);results.removeAllViews();a.label(results,"지출을 계산하는 중…",14,a.muted,false);
  new Thread(()->{try{JSONObject stable=Store.parse(snapshot);List<CostPeriodReport.Bike> report=CostPeriodReport.build(rows(stable,"vehicles"),rows(stable,"services"),rows(stable,"expenses"),from,to,LocalDate.parse(a.today()));a.runOnUiThread(()->{if(!alive(owner,results,request))return;showReport(results,report);});}catch(Exception invalid){a.runOnUiThread(()->{if(alive(owner,results,request)){results.removeAllViews();a.label(results,"지출을 계산하지 못했습니다. 원본은 변경하지 않았습니다.",14,a.muted,false);}});}},"moto-period-cost").start();
 }
 private void showReport(LinearLayout results,List<CostPeriodReport.Bike> report){
  results.removeAllViews();if(report.isEmpty()){a.label(results,"등록된 바이크가 없습니다.",16,a.muted,false);return;}
  EditText search=report.size()>1?FeatureUi.input(a,results,"차량 이름 필터","",false):null;LinearLayout rows=a.col();results.addView(rows);final int[] offset={0};final Runnable[] draw={null};
  draw[0]=()->{rows.removeAllViews();String query=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);List<CostPeriodReport.Bike> matched=new ArrayList<>();for(CostPeriodReport.Bike bike:report)if(bike.name.toLowerCase(Locale.ROOT).contains(query))matched.add(bike);
   int end=Math.min(matched.size(),offset[0]+20);if(report.size()>1)a.label(rows,"차량 "+(matched.isEmpty()?0:offset[0]+1)+"–"+end+" / "+matched.size(),13,a.muted,false);
   for(int i=offset[0];i<end;i++){CostPeriodReport.Bike bike=matched.get(i);a.label(rows,bike.name,20,a.fg,true);a.label(rows,FeatureUi.money(bike.total)+" · "+bike.records+"건",22,a.fg,true);StringJoiner categories=new StringJoiner("  ·  ");for(Map.Entry<String,java.math.BigDecimal> item:bike.categories.entrySet())if(item.getValue().signum()!=0)categories.add(item.getKey()+" "+FeatureUi.money(item.getValue()));a.label(rows,bike.records==0?"이 기간에 등록된 지출이 없습니다.":categories.toString(),14,a.muted,false);double km=bike.observedKm();a.label(rows,Double.isFinite(km)?"입력된 계기판 구간 "+a.num(km)+" km · 구간 기준 약 "+a.won(bike.total.doubleValue()/km)+"/km":"거리 기록 부족 또는 역전 · km당 비용 표시 안 함",13,a.muted,false);if(bike.excluded>0)a.label(rows,"예정·미래·유효하지 않은 기록 "+bike.excluded+"건 제외",13,a.muted,false);a.divider(rows);}
   if(offset[0]>0)a.action(rows,"이전 차량",false,()->{offset[0]=Math.max(0,offset[0]-20);draw[0].run();});if(end<matched.size())a.action(rows,"다음 차량",false,()->{offset[0]+=20;draw[0].run();});
  };if(search!=null)search.addTextChangedListener(EntryAssist.watch(()->{offset[0]=0;draw[0].run();}));draw[0].run();
 }
 private boolean alive(Dialog owner,LinearLayout results,Object token){return !a.isFinishing()&&!a.isDestroyed()&&a.dialog==owner&&owner!=null&&owner.isShowing()&&results.getTag()==token;}
 private static List<JSONObject> rows(JSONObject data,String key)throws Exception{List<JSONObject> result=new ArrayList<>();JSONArray array=data.getJSONArray(key);for(int i=0;i<array.length();i++)result.add(array.getJSONObject(i));return result;}
}
