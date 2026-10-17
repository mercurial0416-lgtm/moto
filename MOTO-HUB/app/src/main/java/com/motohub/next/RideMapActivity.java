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
  caption=new TextView(this);caption.setText("저장된 경로를 불러오는 중…");caption.setPadding(18,12,14,14);caption.setTextColor(0xff9caebd);caption.setTextSize(14);base.addView(caption);
  map=new MapView(this);map.setTileSource(TileSourceFactory.MAPNIK);map.setMultiTouchControls(true);
  map.getOverlayManager().getTilesOverlay().setColorFilter(org.osmdroid.views.overlay.TilesOverlay.INVERT_COLORS);
  map.getController().setZoom(10.0);base.addView(map,new LinearLayout.LayoutParams(-1,0,1));
  TextView attribution=new TextView(this);attribution.setText("© OpenStreetMap contributors · GPX는 기기에만 저장됨");attribution.setTextColor(0xff9caebd);attribution.setPadding(12,8,12,14);base.addView(attribution);
  new Thread(()->{
   try{File track=new File(TrackFile.directory(getFilesDir()),filename);
    final int[] n={0};final ArrayList<GeoPoint> route=new ArrayList<>();
    final int total=TrackFile.read(track,(lat,lon,accuracy,time)->n[0]++);
    if(total<2)throw new IllegalArgumentException("좌표가 부족합니다");
    int step=Math.max(1,(total+3999)/4000);final int[] index={0};
    TrackFile.read(track,(lat,lon,accuracy,time)->{int i=index[0]++;if(i%step==0||i==total-1)route.add(new GeoPoint(lat,lon));});
    RideInsights.Result info=RideInsights.analyze(track);
    runOnUiThread(()->{if(!alive||map==null)return;
     Polyline poly=new Polyline(map);poly.setPoints(route);poly.getOutlinePaint().setColor(0xffff6b1b);poly.getOutlinePaint().setStrokeWidth(9f);map.getOverlays().add(poly);
     Marker start=new Marker(map);start.setPosition(route.get(0));start.setTitle("출발");map.getOverlays().add(start);
     Marker finish=new Marker(map);finish.setPosition(route.get(route.size()-1));finish.setTitle("도착");map.getOverlays().add(finish);
     double north=-90,south=90,east=-180,west=180;for(GeoPoint p:route){north=Math.max(north,p.getLatitude());south=Math.min(south,p.getLatitude());east=Math.max(east,p.getLongitude());west=Math.min(west,p.getLongitude());}
     if(north-south<.0005){north+=.002;south-=.002;}if(east-west<.0005){east+=.002;west-=.002;}
     map.zoomToBoundingBox(new BoundingBox(north,east,south,west),true,72);
     caption.setText(info.distanceLabel()+" · "+info.points+"개 좌표 · "+String.format(java.util.Locale.KOREA,"평균 %.0f km/h",info.averageKmh));
     map.invalidate();
    });
   }catch(Exception e){runOnUiThread(()->{if(alive&&caption!=null)caption.setText("경로를 열지 못했어요: "+e.getMessage());});}
  },"MOTO-route-load").start();
 }
 @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
 @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
 @Override protected void onDestroy(){alive=false;if(map!=null)map.onDetach();super.onDestroy();}
}
