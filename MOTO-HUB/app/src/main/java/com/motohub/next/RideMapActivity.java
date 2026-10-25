package com.motohub.next;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;
import org.osmdroid.views.*;
import org.osmdroid.views.overlay.*;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.BoundingBox;

/** View a saved route without sharing its coordinates with other MOTO HUB users. */
public final class RideMapActivity extends Activity {
 private MapView map;private TextView caption;private volatile boolean alive=true;
 private final ArrayList<Polyline> speedLines=new ArrayList<>();
 private boolean speedVisible=false;private Polyline baseLine;
 private Button speedButton;
 private Button tileButton;private boolean onlineTiles=false;
 @Override public void onCreate(Bundle b){super.onCreate(b);
  String filename=getIntent().getStringExtra("track");
  if(filename==null||!filename.matches("[0-9]{10,15}-[0-9a-f-]{36}\\.track")){finish();return;}
  org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName()+"/"+BuildConfig.VERSION_NAME);
  org.osmdroid.config.Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(),"map"));
  org.osmdroid.config.Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(),"map/tiles"));
  LinearLayout base=new LinearLayout(this);base.setOrientation(LinearLayout.VERTICAL);base.setBackgroundColor(0xff090f14);setContentView(base);
  base.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(x.left,x.top,x.right,x.bottom);}return i;});base.requestApplyInsets();
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(12,8,12,8);
  Button back=new Button(this);back.setText("←");back.setOnClickListener(v->finish());top.addView(back);
  TextView title=new TextView(this);title.setText("MY RIDE  ·  주행 경로");title.setTextColor(Color.WHITE);title.setTextSize(19);title.setPadding(16,0,0,0);top.addView(title);base.addView(top);
  speedButton=new Button(this);speedButton.setText("속도별 경로 보기");speedButton.setEnabled(false);
  speedButton.setOnClickListener(v->{
   speedVisible=!speedVisible;
   for(Polyline line:speedLines)line.setVisible(speedVisible);
   if(baseLine!=null)baseLine.getOutlinePaint().setColor(speedVisible?0xff596572:0xffff6b1b);
   speedButton.setText(speedVisible?"단색 경로 보기":"속도별 경로 보기");map.invalidate();
  });base.addView(speedButton);
  tileButton=new Button(this);tileButton.setText("온라인 배경지도 켜기 (선택)");
  tileButton.setOnClickListener(v->{
   if(onlineTiles){setOnlineTiles(false);return;}
   new AlertDialog.Builder(this).setTitle("온라인 배경지도")
    .setMessage("OpenStreetMap 타일 서버에 지도 위치 주변의 영역 요청이 전달됩니다. 저장된 GPS 원본은 전송하지 않습니다. Wi-Fi/모바일 데이터가 사용될 수 있습니다. 배경지도 없이도 경로를 볼 수 있습니다.")
    .setNegativeButton("오프라인 유지",null)
    .setPositiveButton("배경지도 켜기",(d,w)->setOnlineTiles(true)).show();
  });base.addView(tileButton);
  caption=new TextView(this);caption.setText("저장된 경로를 불러오는 중…");caption.setPadding(18,12,14,14);caption.setTextColor(0xff9caebd);caption.setTextSize(14);base.addView(caption);
  map=new MapView(this);map.setTileSource(TileSourceFactory.MAPNIK);map.setMultiTouchControls(true);
  // Saved routes stay available without automatically contacting any map tile server.
  map.setUseDataConnection(false);
  map.getOverlayManager().getTilesOverlay().setColorFilter(org.osmdroid.views.overlay.TilesOverlay.INVERT_COLORS);
  map.getController().setZoom(10.0);base.addView(map,new LinearLayout.LayoutParams(-1,0,1));
  TextView attribution=new TextView(this);attribution.setText("오프라인 경로 표시 · 배경지도는 캐시에 있을 때만 표시 · © OpenStreetMap contributors");attribution.setTextColor(0xff9caebd);attribution.setPadding(12,8,12,14);base.addView(attribution);
  new Thread(()->{
   try{File track=new File(TrackFile.directory(getFilesDir()),filename);
    final int[] n={0};final ArrayList<GeoPoint> route=new ArrayList<>();
    final int total=TrackFile.read(track,(lat,lon,accuracy,time)->n[0]++);
    if(total<2)throw new IllegalArgumentException("좌표가 부족합니다");
    int step=Math.max(1,(total+1999)/2000);final int[] index={0};
    final ArrayList<Long> times=new ArrayList<>();final ArrayList<Double> accuracies=new ArrayList<>();
    TrackFile.read(track,(lat,lon,accuracy,time)->{int i=index[0]++;if(i%step==0||i==total-1){route.add(new GeoPoint(lat,lon));times.add(time);accuracies.add(accuracy);}});
    final ArrayList<ArrayList<GeoPoint>> colored=new ArrayList<>();final ArrayList<Integer> grades=new ArrayList<>();
    ArrayList<GeoPoint> chunk=null;int lastGrade=RideSpeedSegments.UNKNOWN;
    for(int i=1;i<route.size();i++){
     GeoPoint previous=route.get(i-1),current=route.get(i);
     int grade=RideSpeedSegments.category(times.get(i)-times.get(i-1),
       RideInsights.meters(previous.getLatitude(),previous.getLongitude(),current.getLatitude(),current.getLongitude()),
       accuracies.get(i-1),accuracies.get(i));
     if(grade==RideSpeedSegments.UNKNOWN){chunk=null;lastGrade=grade;continue;}
     if(chunk==null||grade!=lastGrade){
      if(colored.size()>350){colored.clear();grades.clear();break;} // Avoid thousands of overlays.
      chunk=new ArrayList<>();chunk.add(previous);colored.add(chunk);grades.add(grade);
     }
     chunk.add(current);lastGrade=grade;
    }
    RideInsights.Result info=RideInsights.analyze(track);
    runOnUiThread(()->{if(!alive||map==null)return;
     Polyline poly=new Polyline(map);poly.setPoints(route);poly.getOutlinePaint().setColor(0xffff6b1b);poly.getOutlinePaint().setStrokeWidth(9f);map.getOverlays().add(poly);baseLine=poly;
     for(int i=0;i<colored.size();i++){
      Polyline line=new Polyline(map);line.setPoints(colored.get(i));
      int grade=grades.get(i);
      line.getOutlinePaint().setColor(grade==RideSpeedSegments.SLOW?0xff48d99a:grade==RideSpeedSegments.CRUISE?0xffffcf65:0xffef6868);
      line.getOutlinePaint().setStrokeWidth(11f);line.setVisible(false);map.getOverlays().add(line);speedLines.add(line);
     }
     speedButton.setEnabled(!speedLines.isEmpty());
     Marker start=new Marker(map);start.setPosition(route.get(0));start.setTitle("출발");map.getOverlays().add(start);
     Marker finish=new Marker(map);finish.setPosition(route.get(route.size()-1));finish.setTitle("도착");map.getOverlays().add(finish);
     double north=-90,south=90,east=-180,west=180;for(GeoPoint p:route){north=Math.max(north,p.getLatitude());south=Math.min(south,p.getLatitude());east=Math.max(east,p.getLongitude());west=Math.min(west,p.getLongitude());}
     if(north-south<.0005){north+=.002;south-=.002;}if(east-west<.0005){east+=.002;west-=.002;}
     map.zoomToBoundingBox(new BoundingBox(north,east,south,west),true,72);
     caption.setText(info.distanceLabel()+" · "+info.points+"개 좌표 · "+String.format(java.util.Locale.KOREA,"평균 %.0f km/h",info.averageKmh)+"\n속도 색상: 초록 <30 · 노랑 30~69 · 빨강 ≥70km/h (GPS 추정, 끊긴 구간 제외)");
     map.invalidate();
    });
   }catch(Exception e){runOnUiThread(()->{if(alive&&caption!=null)caption.setText("경로를 열지 못했어요: "+e.getMessage());});}
  },"MOTO-route-load").start();
 }
 private void setOnlineTiles(boolean allowed){
  onlineTiles=allowed;
  if(map!=null){map.setUseDataConnection(allowed);map.invalidate();}
  if(tileButton!=null)tileButton.setText(allowed?"온라인 배경지도 끄기":"온라인 배경지도 켜기 (선택)");
 }
 @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
 @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
 @Override protected void onDestroy(){alive=false;if(map!=null)map.onDetach();super.onDestroy();}
}
