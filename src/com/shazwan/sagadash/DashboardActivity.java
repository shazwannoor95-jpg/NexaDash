package com.shazwan.sagadash;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.media.*;
import android.media.session.*;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** English personal dashboard. All status comes from a real provider or an explicit unavailable state. */
public class DashboardActivity extends Activity implements LocationListener {
    static final int BG=0xff07131f, PANEL=0xff102236, LINE=0xff243c51, TEXT=0xffedf5ff, MUTED=0xff8ea8be, BLUE=0xff47b6ff;
    float scale;
    LinearLayout main, prayerBar;
    TextView clock,date,next,speed,gps,trip,title,artist,source,lyric,play,progress;
    ImageView artwork;
    SagaView car;
    DashMedia media;
    LocationManager locations;
    Location previous;
    volatile PrayerLocation prayerLocation;
    volatile boolean prayerMapFailed;
    private boolean clockEventsRegistered;
    private final BroadcastReceiver clockEvents=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction()))TrustedClock.wallChanged(DashboardActivity.this);
            Notice.dismissExpired();prayerRenderKey="";refreshClock();schedulePrayer();
        }
    };
    long prayerAttemptAt;
    String prayerAttemptKey="";
    long fixAt, fixTime;
    double tripMetres;
    boolean visible, prayerLoading;
    int lyricGeneration;
    String prayerRenderKey="";
    final Handler handler=new Handler(Looper.getMainLooper());
    final ExecutorService io=Executors.newSingleThreadExecutor();
    final ExecutorService prayerIo=Executors.newSingleThreadExecutor();
    String lyricKey="", prayerStatus="", requestedZone="";
    DashLyrics lyrics=new DashLyrics();
    final Runnable tick=new Runnable(){public void run(){if(!visible)return;refreshClock();if(SystemClock.elapsedRealtime()-fixAt>10000){speed.setText("— km/h");gps.setText("GPS · Waiting for signal");previous=null;}lyric.setText(lyrics.at(media.position()));long pos=media.position()/1000;progress.setText(String.format(Locale.US,"%d:%02d",pos/60,pos%60));handler.postDelayed(this,1000);}};
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Lang.wrap(base));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        TrustedClock.initialize(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN);
        float density=getResources().getDisplayMetrics().density;
        scale=Math.min(getResources().getDisplayMetrics().widthPixels/1280f,getResources().getDisplayMetrics().heightPixels/720f)/density;
        tripMetres=dashPrefs().getFloat("trip",0);
        build();
        media=new DashMedia(this,this::refreshMedia);
        locations=(LocationManager)getSystemService(LOCATION_SERVICE);
        IntentFilter timeFilter=new IntentFilter(Intent.ACTION_TIME_CHANGED);timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);timeFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(clockEvents,timeFilter);clockEventsRegistered=true;
        prayerIo.execute(()->{
            try{prayerLocation=new PrayerLocation(this);}catch(Exception e){prayerMapFailed=true;}
            for(String zone:PrayerZones.TRAVEL_ZONES)JakimSchedule.cache(this,zone);
        });
    }
    int px(float n){return Math.max(1,Math.round(n*scale*getResources().getDisplayMetrics().density));}
    TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));v.setGravity(Gravity.CENTER_VERTICAL);v.setFontFeatureSettings("tnum");return v;}
    GradientDrawable background(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(px(20));d.setStroke(px(1),LINE);return d;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void pad(View v,int n){v.setPadding(px(n),px(n),px(n),px(n));}
    TextView button(String s,Runnable action){TextView v=text(s,17,TEXT);v.setGravity(Gravity.CENTER);v.setBackground(background(PANEL));pad(v,12);v.setMinHeight(px(56));v.setOnClickListener(w->action.run());v.setContentDescription(s);return v;}
    void equal(LinearLayout parent,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(px(5),px(4),px(5),px(4));parent.addView(child,p);}
    LinearLayout iconButton(String label,String symbol,int color,Runnable action){
        LinearLayout box=column();box.setGravity(Gravity.CENTER);box.setOnClickListener(v->action.run());box.setContentDescription(label);
        TextView icon=text(symbol,25,TEXT);icon.setGravity(Gravity.CENTER);GradientDrawable bg=background(color);bg.setCornerRadius(px(13));icon.setBackground(bg);box.addView(icon,new LinearLayout.LayoutParams(px(46),px(46)));
        TextView caption=text(label,13,TEXT);caption.setGravity(Gravity.CENTER);box.addView(caption,new LinearLayout.LayoutParams(-1,px(24)));return box;
    }
    LinearLayout card(){LinearLayout l=column();l.setBackground(background(PANEL));pad(l,18);return l;}
    void build(){
        LinearLayout root=row();root.setBackgroundColor(BG);pad(root,14);setContentView(root);
        LinearLayout nav=column();nav.setGravity(Gravity.CENTER);root.addView(nav,new LinearLayout.LayoutParams(px(96),-1));
        TextView brand=text("S / D",23,BLUE);brand.setGravity(Gravity.CENTER);nav.addView(brand,new LinearLayout.LayoutParams(-1,px(84)));
        for(String item:new String[]{"⌂\nHome","▦\nApps","⚙\nSettings"}){TextView b=button(item,()->{if(item.contains("Apps"))apps();else if(item.contains("Settings"))settings();});nav.addView(b,new LinearLayout.LayoutParams(-1,px(90)));}
        main=column();root.addView(main,new LinearLayout.LayoutParams(0,-1,1));pad(main,10);
        LinearLayout header=row();clock=text("",38,TEXT);header.addView(clock,new LinearLayout.LayoutParams(0,px(55),1));TextView status=button("Media access  ·  ⚙",this::settings);header.addView(status);main.addView(header);clock.setOnClickListener(v->clockDialog());
        date=text("",16,MUTED);main.addView(date,new LinearLayout.LayoutParams(-1,px(27)));
        prayerBar=row();prayerBar.setBackground(background(0xff0b1c2b));prayerBar.setOnClickListener(v->prayerDialog());main.addView(prayerBar,new LinearLayout.LayoutParams(-1,px(66)));
        next=text("",13,BLUE);main.addView(next,new LinearLayout.LayoutParams(-1,px(23)));
        LinearLayout content=row();main.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout left=card();LinearLayout.LayoutParams leftP=new LinearLayout.LayoutParams(0,-1,.43f);leftP.setMargins(0,px(6),px(10),px(6));content.addView(left,leftP);
        LinearLayout vehicleHeader=row();TextView model=text("PROTON SAGA",17,MUTED);vehicleHeader.addView(model,new LinearLayout.LayoutParams(0,-2,1));speed=text("— km/h",24,TEXT);vehicleHeader.addView(speed);left.addView(vehicleHeader);
        car=new SagaView(this);left.addView(car,new LinearLayout.LayoutParams(-1,0,1));TextView hint=text("Drag to rotate · Pinch to zoom · Double-tap to reset",13,MUTED);hint.setGravity(Gravity.CENTER);left.addView(hint);
        gps=text("GPS · Waiting for signal",14,BLUE);left.addView(gps);trip=text("",14,MUTED);left.addView(trip);trip.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Reset GPS trip?").setPositiveButton("Reset",(d,w)->{tripMetres=0;saveTrip();}).setNegativeButton("Cancel",null).show();return true;});updateTrip();
        LinearLayout right=column();content.addView(right,new LinearLayout.LayoutParams(0,-1,.57f));
        LinearLayout shortcuts=row();right.addView(shortcuts,new LinearLayout.LayoutParams(-1,px(115)));
        equal(shortcuts,button("▶\nYouTube",()->launchVideo()));equal(shortcuts,button("↗\nNavigation",()->launch(new String[]{"com.waze","com.google.android.apps.maps"})));equal(shortcuts,button("♫\nMedia sources",this::sources));
        LinearLayout player=card();right.addView(player,new LinearLayout.LayoutParams(-1,0,1));
        source=text("NOW PLAYING",12,BLUE);player.addView(source);
        LinearLayout details=row();artwork=new ImageView(this);artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);details.addView(artwork,new LinearLayout.LayoutParams(px(68),px(68)));LinearLayout labels=column();pad(labels,8);details.addView(labels,new LinearLayout.LayoutParams(0,-2,1));title=text("No active media",24,TEXT);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);artist=text("Play YouTube, radio or Bluetooth audio",15,MUTED);artist.setMaxLines(2);labels.addView(artist);player.addView(details);
        lyric=text("Lyrics appear when a track is available",17,TEXT);lyric.setGravity(Gravity.CENTER);lyric.setMaxLines(3);player.addView(lyric,new LinearLayout.LayoutParams(-1,0,1));lyric.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Lyrics · LRCLIB").setMessage(lyrics.plain).setPositiveButton("Close",null).show());
        LinearLayout controls=row();progress=text("0:00",13,MUTED);controls.addView(progress,new LinearLayout.LayoutParams(px(50),-1));equal(controls,button("Previous",()->media.command(-1)));play=button("Play",()->media.command(0));equal(controls,play);equal(controls,button("Next",()->media.command(1)));player.addView(controls,new LinearLayout.LayoutParams(-1,px(54)));
        LinearLayout dock=row();main.addView(dock,new LinearLayout.LayoutParams(-1,px(87)));
        String[] names={"Music","Bluetooth","Radio","Video","Files","Equalizer","Car Link","DSP"};
        int[] colors={0xffec4d79,0xff288ff0,0xff9459ec,0xffec7148,0xffe7a52b,0xff6e8199,0xff239366,0xff336bf2};
        String[] symbols={"♫","ᛒ","FM","▶","▣","▥","▱","☷"};
        for(int i=0;i<names.length;i++){final String name=names[i];equal(dock,iconButton(name,symbols[i],colors[i],()->shortcut(name)));}
        TextView footer=text("Vehicle settings                                          SAGA DASH  /  PERSONAL EDITION",14,MUTED);pad(footer,10);footer.setBackground(background(0xff0c1b29));footer.setOnClickListener(v->launch(new String[]{"com.android.car.setting"}));main.addView(footer,new LinearLayout.LayoutParams(-1,px(44)));
    }
    void refreshClock(){
        TrustedClock.Reading time=TrustedClock.read(this);long now=time.utc;SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.US);f.setTimeZone(JakimSchedule.MY);clock.setText(f.format(new Date(now)));f.applyPattern("EEEE, dd MMM yyyy  ·  'Malaysia'");date.setText(f.format(new Date(now))+" · "+TrustedClock.label(this));
        String zone=JakimSchedule.zone(this);JakimSchedule.Day day=time.verified?JakimSchedule.readyAt(this,now):null;
        String renderKey=zone+time.verified+(day==null?"missing":day.date)+JakimSchedule.readyNext(this,now);
        boolean rebuild=!renderKey.equals(prayerRenderKey);
        if(rebuild){prayerBar.removeAllViews();prayerRenderKey=renderKey;}
        if(day==null){if(rebuild){TextView prompt=text(!time.verified?"JAKIM · Waiting for verified time — tap the clock":zone.isEmpty()?"JAKIM  ·  Waiting for GPS — tap for prayer settings":zone+(JakimSchedule.ready(this)?"  ·  Today’s offline timetable unavailable/expired — tap to download":"  ·  Loading offline timetable…"),17,BLUE);pad(prompt,12);prayerBar.addView(prompt);}next.setText((time.verified?prayerLocationLabel():"Countdown and prayer alerts wait for GPS time or your confirmation")+" · "+prayerStatus);}
        else{
            if(rebuild){TextView zoneView=text(zone+"\nJAKIM",12,MUTED);pad(zoneView,10);prayerBar.addView(zoneView);}
            long target=JakimSchedule.readyNext(this,now);
            if(rebuild)for(int i=0;i<6;i++){TextView v=text(JakimSchedule.NAMES[i]+"\n"+JakimSchedule.time(day.times[i]),16,day.times[i]==target?BLUE:TEXT);v.setGravity(Gravity.CENTER);if(rebuild)prayerBar.addView(v,new LinearLayout.LayoutParams(0,-1,1));}
            if(target>0){long sec=(target-now)/1000;next.setText(String.format(Locale.US,"Next prayer in %02d:%02d:%02d · Official JAKIM times · %s",sec/3600,sec/60%60,sec%60,zone)+" · "+prayerLocationLabel());}else next.setText("Today's timetable ended · Download tomorrow's schedule · "+prayerLocationLabel());
        }
        String requestKey=zone+(time.verified?JakimSchedule.dateKey(now):"unverified");
        if(!requestKey.equals(requestedZone)){requestedZone=requestKey;fetchPrayer(false);}
    }
    String prayerLocationLabel(){
        return prayerLocation==null?(prayerMapFailed?"Offline map unavailable · Choose a zone manually":"Loading offline GPS map…"):prayerLocation.label(SystemClock.elapsedRealtime());
    }
    void schedulePrayer(){if(!isDestroyed())PrayerAlarm.schedule(this);}
    void fetchPrayer(){fetchPrayer(false);}
    void fetchPrayer(boolean force){
        if(prayerLoading||isDestroyed())return;
        String selected=JakimSchedule.zone(this);
        String attemptKey=selected+JakimSchedule.dateKey(TrustedClock.now(this));
        long elapsed=SystemClock.elapsedRealtime();
        if(!force&&attemptKey.equals(prayerAttemptKey)&&elapsed-prayerAttemptAt<15*60_000)return;
        prayerAttemptAt=elapsed;prayerAttemptKey=attemptKey;
        LinkedHashSet<String> zones=new LinkedHashSet<>();
        if(!selected.isEmpty())zones.add(selected);
        Collections.addAll(zones,PrayerZones.TRAVEL_ZONES);
        prayerLoading=true;prayerStatus="Checking offline JAKIM timetables…";
        prayerIo.execute(()->{
            final List<String> required=new ArrayList<>();
            for(String zone:zones)if(force||JakimSchedule.refreshDue(this,zone,TrustedClock.now(this)))required.add(zone);
            List<String> failed=new ArrayList<>();
            for(String zone:required){if(Thread.currentThread().isInterrupted())break;try{JakimSchedule.refresh(this,zone);}catch(Exception e){failed.add(zone);}}
            String result=failed.isEmpty()?"Offline timetables updated":"Download unavailable for "+android.text.TextUtils.join(", ",failed)+" · Existing cache kept";
            handler.post(()->{prayerLoading=false;if(isDestroyed())return;prayerStatus=result;refreshClock();schedulePrayer();if(!selected.equals(JakimSchedule.zone(this)))fetchPrayer(false);});
        });
    }
    void clockDialog(){
        SimpleDateFormat f=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss",Locale.US);f.setTimeZone(JakimSchedule.MY);
        String message=TrustedClock.label(this)+"\nDevice time: "+f.format(new Date(System.currentTimeMillis()))
                +"\nSaga Dash time: "+f.format(new Date(TrustedClock.now(this)))
                +"\n\nGPS time works offline after a fresh satellite fix. It changes Saga Dash's clock only. The verified time is kept for this boot, up to 24 hours without another fix."
                +"\n\nIf GPS is unavailable, confirm device time only after checking it is correct. Prayer alerts remain paused while time is unverified.";
        new AlertDialog.Builder(this).setTitle("Clock & prayer time").setMessage(message)
            .setPositiveButton("Device time is correct",(d,w)->{
                if(TrustedClock.confirmDevice(this)){prayerRenderKey="";refreshClock();schedulePrayer();}
                else toast("Set the correct device date/time in Android Settings first");
            }).setNeutralButton("Wait for GPS",(d,w)->{TrustedClock.forget(this);prayerRenderKey="";refreshClock();schedulePrayer();})
            .setNegativeButton("Close",null).show();
    }
    void prayerDialog(){
        LinearLayout box=column();pad(box,20);
        CheckBox automatic=new CheckBox(this);automatic.setText("Auto zone from GPS (offline map)");automatic.setChecked(JakimSchedule.automatic(this));box.addView(automatic);
        TextView help=text("Offline GPS coverage: Selangor, Kuala Lumpur & Putrajaya. Near boundaries or without GPS, the last confirmed zone is kept. Choose a manual zone when travelling outside coverage.",16,MUTED);box.addView(help);
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(JakimSchedule.zone(this));input.setHint("Manual e-Solat code, e.g. SGR02");input.setEnabled(!automatic.isChecked());box.addView(input);
        automatic.setOnCheckedChangeListener((v,on)->input.setEnabled(!on));
        StringBuilder coverage=new StringBuilder("Official JAKIM data saved on device:\n");
        LinkedHashSet<String> zones=new LinkedHashSet<>(Arrays.asList(PrayerZones.TRAVEL_ZONES));if(!JakimSchedule.zone(this).isEmpty())zones.add(JakimSchedule.zone(this));
        for(String zone:zones)coverage.append(zone).append(": ").append(JakimSchedule.readyCoverage(zone)).append('\n');
        TextView saved=text(coverage+"\n"+prayerStatus,14,MUTED);box.addView(saved);
        ScrollView scroll=new ScrollView(this);scroll.addView(box);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Prayer location & offline data").setView(scroll).setPositiveButton("Save",null).setNeutralButton("Refresh offline data",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(v->{
            dialog.getButton(-1).setOnClickListener(w->{
                try{
                    if(automatic.isChecked()){JakimSchedule.useGps(this);if(prayerLocation!=null)prayerLocation.reset();if(visible)startGps();}
                    else JakimSchedule.select(this,input.getText().toString().trim().toUpperCase(Locale.US));
                    dialog.dismiss();prayerRenderKey="";refreshClock();schedulePrayer();fetchPrayer(false);
                }catch(Exception e){input.setError(e.getMessage());}
            });
            dialog.getButton(-3).setOnClickListener(w->{fetchPrayer(true);dialog.dismiss();});
        });dialog.show();
    }
    void refreshMedia(){
        if(media==null)return;MediaController c=media.current;MediaMetadata m=c==null?null:c.getMetadata();
        String t=m==null?"":m.getString(MediaMetadata.METADATA_KEY_TITLE),a=m==null?"":m.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if(t==null)t="";if(a==null)a="";
        title.setText(t.isEmpty()?"No active media":t);artist.setText(a.isEmpty()?media.problem:a);source.setText(c==null?"NOW PLAYING":appName(c.getPackageName()));play.setText(media.playing()?"Pause":"Play");
        Bitmap cover=m==null?null:m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(cover==null&&m!=null)cover=m.getBitmap(MediaMetadata.METADATA_KEY_ART);artwork.setImageBitmap(cover);artwork.setVisibility(cover==null?View.GONE:View.VISIBLE);
        String key=t+"\n"+a;if(!key.equals(lyricKey)){lyricGeneration++;lyricKey=key;lyrics=new DashLyrics();lyrics.plain=t.isEmpty()?"Play media to show track details":a.isEmpty()?"Artist metadata is needed to find lyrics":"Tap Lyrics in Settings to look up this track";if(dashPrefs().getBoolean("lyrics",false)&&!t.isEmpty()&&!a.isEmpty())fetchLyrics(t,a,key);}
        lyric.setText(lyrics.at(media.position()));
    }
    void fetchLyrics(String t,String a,String key){
        lyrics.plain="Finding lyrics…";final int generation=lyricGeneration;
        io.execute(()->{DashLyrics found=new DashLyrics();try{found=DashLyrics.parse(new JSONObject(DashNetwork.get("https://lrclib.net/api/get?track_name="+URLEncoder.encode(t,"UTF-8")+"&artist_name="+URLEncoder.encode(a,"UTF-8"))));}catch(Exception ignored){}final DashLyrics result=found;handler.post(()->{if(!isDestroyed()&&generation==lyricGeneration&&lyricKey.equals(key)&&dashPrefs().getBoolean("lyrics",false)){lyrics=result;lyric.setText(lyrics.at(media.position()));}});});
    }
    String appName(String pkg){try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){return pkg;}}
    void sources(){List<MediaController> sessions=media.sessions();List<String> names=new ArrayList<>();names.add("Auto · Follow playing media");for(MediaController c:sessions)names.add(appName(c.getPackageName()));names.add("Open Bluetooth");names.add("Open Radio");names.add("Open YouTube / TizenTube");new AlertDialog.Builder(this).setTitle("Media source").setItems(names.toArray(new String[0]),(d,i)->{if(i==0)media.choose("");else if(i<=sessions.size())media.choose(sessions.get(i-1).getPackageName());else if(i==sessions.size()+1)shortcut("Bluetooth");else if(i==sessions.size()+2)shortcut("Radio");else launchVideo();}).show();}
    void settings(){String[] items={"Prayer zone & timetable","Enable media / notification access","Lyrics: "+(dashPrefs().getBoolean("lyrics",false)?"On":"Off"),"Location permission","Android settings","AI assistant","Screen lock","Power saver","AI profiles","Set as default launcher","Clock / GPS time"};new AlertDialog.Builder(this).setTitle("Dashboard settings").setItems(items,(d,i)->{switch(i){case 0:prayerDialog();break;case 1:startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));break;case 2:new AlertDialog.Builder(this).setTitle("Online lyrics").setMessage("Send the current track title and artist to LRCLIB to find lyrics? Bluetooth itself does not supply lyrics. Synced lyrics depend on availability and playback position from the source.").setPositiveButton("Enable",(v,w)->{dashPrefs().edit().putBoolean("lyrics",true).apply();lyricKey="";refreshMedia();}).setNeutralButton("Disable",(v,w)->{dashPrefs().edit().putBoolean("lyrics",false).apply();lyricKey="";refreshMedia();}).setNegativeButton("Cancel",null).show();break;case 3:requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},7);break;case 4:startActivity(new Intent(Settings.ACTION_SETTINGS));break;case 5:startActivity(new Intent(this,AskActivity.class));break;case 6:configureLock();break;case 7:startActivity(new Intent(this,SettingsActivity.class).putExtra(SettingsActivity.EXTRA_TAB,SettingsActivity.TAB_POWER));break;case 8:startActivity(new Intent(this,SettingsActivity.class).putExtra(SettingsActivity.EXTRA_TAB,SettingsActivity.TAB_AI));break;case 9:startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));break;case 10:clockDialog();break;}}).show();}
    void apps(){io.execute(()->{Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo> list=getPackageManager().queryIntentActivities(query,0);Collections.sort(list,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));List<String> labels=new ArrayList<>();for(ResolveInfo r:list)labels.add(r.loadLabel(getPackageManager()).toString());handler.post(()->{if(isDestroyed())return;new AlertDialog.Builder(this).setTitle("Applications").setItems(labels.toArray(new String[0]),(d,i)->{ResolveInfo r=list.get(i);try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(r.activityInfo.packageName,r.activityInfo.name)));}catch(Exception e){toast("Unable to open app");}}).show();});});}
    void shortcut(String name){switch(name){case "Bluetooth":launch(new String[]{"com.nwd.android.bt","com.nwd.android.phone"});break;case "Radio":launch(new String[]{"com.nwd.android.radio"});break;case "Music":launch(new String[]{"com.spotify.music","com.nwd.android.music"});break;case "Video":launchVideo();break;case "Car Link":launch(new String[]{"com.zjinnova.zlink","com.zjinnova.zlink5"});break;case "Files":try{startActivity(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));}catch(Exception e){apps();}break;case "Equalizer":try{startActivity(new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL").putExtra("android.media.extra.PACKAGE_NAME",getPackageName()));}catch(Exception e){toast("Choose your head unit's equalizer in Apps");}break;default:toast("Choose your head unit's DSP app in Apps");}}
    void launchVideo(){launch(new String[]{"io.gh.reisxd.tizentube.cobalt","com.google.android.youtube","com.google.android.youtube.tv"});}
    void launch(String[] packages){for(String pkg:packages){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i!=null){try{startActivity(i);return;}catch(Exception ignored){}}}toast("App not installed or not compatible. Choose an installed app.");apps();}
    void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("No browser installed");}}
    android.content.SharedPreferences dashPrefs(){return createDeviceProtectedStorageContext().getSharedPreferences("dashboard",0);}
    android.content.SharedPreferences lockPrefs(){return createDeviceProtectedStorageContext().getSharedPreferences("klikdash",0);}
    void configureLock(){
        if(!ScreenLock.hasPin(lockPrefs()))ScreenLock.create(this,pin->{ScreenLock.setPin(lockPrefs(),pin);toast("PIN saved. Open Screen lock again to lock.");});
        else new AlertDialog.Builder(this).setTitle("Screen lock").setItems(new String[]{"Lock now","Remove PIN"},(d,i)->{if(i==0){ScreenLock.setLocked(lockPrefs(),true);toast("Locked · Tap the screen to unlock");}else ScreenLock.verify(this,lockPrefs(),()->ScreenLock.clear(lockPrefs()),()->{});}).show();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(ScreenLock.locked(lockPrefs())){if(e.getAction()==MotionEvent.ACTION_UP)ScreenLock.verify(this,lockPrefs(),()->ScreenLock.setLocked(lockPrefs(),false),()->{});return true;}return super.dispatchTouchEvent(e);}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(ScreenLock.locked(lockPrefs())&&e.getKeyCode()!=KeyEvent.KEYCODE_VOLUME_UP&&e.getKeyCode()!=KeyEvent.KEYCODE_VOLUME_DOWN&&e.getKeyCode()!=KeyEvent.KEYCODE_VOLUME_MUTE)return true;return super.dispatchKeyEvent(e);}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    void updateTrip(){trip.setText(String.format(Locale.US,"GPS TRIP  %.1f km · Hold to reset",tripMetres/1000));}
    void saveTrip(){dashPrefs().edit().putFloat("trip",(float)tripMetres).apply();updateTrip();}
    void startGps(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){gps.setText("GPS · Tap Settings to allow location");return;}try{locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);}catch(Exception e){gps.setText("GPS provider unavailable");}}
    @Override public void onLocationChanged(Location l){
        if(TrustedClock.gps(this,l)){prayerRenderKey="";refreshClock();schedulePrayer();}
        if(l.getTime()==fixTime||!l.hasAccuracy()||l.getAccuracy()>50||SystemClock.elapsedRealtimeNanos()-l.getElapsedRealtimeNanos()>10000000000L)return;
        if(prayerLocation!=null&&prayerLocation.update(l)){
            prayerRenderKey="";refreshClock();schedulePrayer();
        }
        fixTime=l.getTime();fixAt=SystemClock.elapsedRealtime();gps.setText("GPS · Connected");if(l.hasSpeed()){int kmh=Math.round(l.getSpeed()*3.6f);speed.setText(kmh+" km/h");HomeActivity.lastKmh=kmh;HomeActivity.lastKmhAt=fixAt;}else speed.setText("— km/h");
        if(previous!=null){float distance=l.distanceTo(previous);long elapsed=l.getElapsedRealtimeNanos()-previous.getElapsedRealtimeNanos();if(elapsed>0&&elapsed<30000000000L&&distance>Math.max(l.getAccuracy(),previous.getAccuracy())&&distance/(elapsed/1e9)<70){tripMetres+=distance;saveTrip();}else if(elapsed>0&&elapsed<30000000000L&&distance<=Math.max(l.getAccuracy(),previous.getAccuracy()))return;}
        previous=l;
    }
    @Override public void onProviderDisabled(String p){if(prayerLocation!=null)prayerLocation.signalLost();gps.setText("GPS · Disabled");previous=null;speed.setText("— km/h");}
    @Override public void onProviderEnabled(String p){}
    @Override public void onStatusChanged(String p,int s,Bundle b){}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);if(r==7&&visible)startGps();}
    @Override protected void onResume(){super.onResume();Notice.dismissExpired();visible=true;car.onResume();media.start();handler.removeCallbacks(tick);tick.run();startGps();PrayerAlarm.home=new java.lang.ref.WeakReference<>(this);schedulePrayer();fetchPrayer();}
    @Override protected void onPause(){visible=false;if(prayerLocation!=null)prayerLocation.signalLost();handler.removeCallbacks(tick);car.onPause();media.stop();locations.removeUpdates(this);previous=null;dashPrefs().edit().putFloat("trip",(float)tripMetres).apply();PrayerAlarm.home.clear();super.onPause();}
    @Override protected void onDestroy(){if(clockEventsRegistered)unregisterReceiver(clockEvents);io.shutdownNow();prayerIo.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){}
}
