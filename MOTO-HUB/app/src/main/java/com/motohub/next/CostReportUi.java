package com.motohub.next;

import android.widget.*;
import android.app.Dialog;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Period selection and side-by-side per-bike figures, separated from screen routing. */
final class CostReportUi {
 private final MainActivity a;CostReportUi(MainActivity a){this.a=a;}
 void open(){
  LinearLayout page=a.page("기간별 유지비 · 차량 비교");LocalDate today=LocalDate.parse(a.today());
  EditText from=FeatureUi.day(a,page,"시작일",today.withDayOfMonth(1).minusMonths(5).toString()),to=FeatureUi.day(a,page,"종료일",today.toString());
  a.pair(page,"이번 달",()->{from.setText(today.withDayOfMonth(1).toString());to.setText(today.toString());},"최근 12개월",()->{from.setText(today.withDayOfMonth(1).minusMonths(11).toString());to.setText(today.toString());});
  a.action(page,"최근 6개월 월별 상세",false,()->new HubScreens(a).legacySixMonthCosts());
  LinearLayout results=a.col();a.action(page,"이 기간으로 비교",true,()->load(from.getText().toString(),to.getText().toString(),results));page.addView(results);a.label(page,"예정·미래 비용은 제외합니다. km당 비용은 입력된 계기판 기록 구간의 참고값이며 전체 기간 실주행거리나 GPS 주행거리와 다를 수 있습니다.",13,a.muted,false);a.show();load(from.getText().toString(),to.getText().toString(),results);
 }
 private void load(String start,String end,LinearLayout results){
  final LocalDate from,to;try{from=LocalDate.parse(start);to=LocalDate.parse(end);if(to.isBefore(from))throw new IllegalArgumentException();}catch(Exception invalid){a.error("시작일과 종료일을 확인하세요.");return;}
  final String snapshot=a.db.toString();final Dialog owner=a.dialog;Object request=new Object();results.setTag(request);results.removeAllViews();a.label(results,"지출을 계산하는 중…",14,a.muted,false);
  new Thread(()->{try{JSONObject stable=Store.parse(snapshot);List<CostPeriodReport.Bike> report=CostPeriodReport.build(rows(stable,"vehicles"),rows(stable,"services"),rows(stable,"expenses"),from,to,LocalDate.parse(a.today()));a.runOnUiThread(()->{if(!alive(owner,results,request))return;showReport(results,report);});}catch(Exception invalid){a.runOnUiThread(()->{if(alive(owner,results,request)){results.removeAllViews();a.label(results,"지출을 계산하지 못했습니다. 원본은 변경하지 않았습니다.",14,a.muted,false);}});}},"moto-period-cost").start();
 }
 private void showReport(LinearLayout results,List<CostPeriodReport.Bike> report){
  results.removeAllViews();if(report.isEmpty()){a.label(results,"등록된 바이크가 없습니다.",16,a.muted,false);return;}
  EditText search=FeatureUi.input(a,results,"차량 이름 필터","",false);LinearLayout rows=a.col();results.addView(rows);final int[] offset={0};final Runnable[] draw={null};
  draw[0]=()->{rows.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);List<CostPeriodReport.Bike> matched=new ArrayList<>();for(CostPeriodReport.Bike bike:report)if(bike.name.toLowerCase(Locale.ROOT).contains(query))matched.add(bike);
   int end=Math.min(matched.size(),offset[0]+20);a.label(rows,"차량 "+(matched.isEmpty()?0:offset[0]+1)+"–"+end+" / "+matched.size(),13,a.muted,false);
   for(int i=offset[0];i<end;i++){CostPeriodReport.Bike bike=matched.get(i);a.label(rows,bike.name,20,a.fg,true);a.label(rows,FeatureUi.money(bike.total)+" · "+bike.records+"건",22,a.fg,true);for(Map.Entry<String,java.math.BigDecimal> item:bike.categories.entrySet())a.label(rows,item.getKey()+"  "+FeatureUi.money(item.getValue()),14,a.muted,false);double km=bike.observedKm();a.label(rows,Double.isFinite(km)?"입력된 계기판 구간 "+a.num(km)+" km · 구간 기준 약 "+a.won(bike.total.doubleValue()/km)+"/km":"거리 기록 부족 또는 역전 · km당 비용 표시 안 함",13,a.muted,false);if(bike.excluded>0)a.label(rows,"예정·미래·유효하지 않은 기록 "+bike.excluded+"건 제외",13,a.muted,false);a.divider(rows);}
   if(offset[0]>0)a.action(rows,"이전 차량",false,()->{offset[0]=Math.max(0,offset[0]-20);draw[0].run();});if(end<matched.size())a.action(rows,"다음 차량",false,()->{offset[0]+=20;draw[0].run();});
  };search.addTextChangedListener(EntryAssist.watch(()->{offset[0]=0;draw[0].run();}));draw[0].run();
 }
 private boolean alive(Dialog owner,LinearLayout results,Object token){return !a.isFinishing()&&!a.isDestroyed()&&a.dialog==owner&&owner!=null&&owner.isShowing()&&results.getTag()==token;}
 private static List<JSONObject> rows(JSONObject data,String key)throws Exception{List<JSONObject> result=new ArrayList<>();JSONArray array=data.getJSONArray(key);for(int i=0;i<array.length();i++)result.add(array.getJSONObject(i));return result;}
}
