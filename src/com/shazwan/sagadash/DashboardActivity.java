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
    SagaAngleView angles;
    SagaShowroom showroom;
    TextView vehicleHint,vehicleMode;
    boolean vehicleVideo;
    LinearLayout root,dock,vehicleContent;
    FrameLayout vehicleStage;
    DashLayoutSurface layoutSurface;
    Pane leftPane,rightPane;
    DashMediaPanel primaryMedia;
    static final int PICK_PANEL_VIDEO=4816;
    String pendingVideoPane="";
    TextView editButton,footerText;
    int editMode,artToken;
    boolean blockEditTouch,appsLoading;
    int appsGeneration;
    AlertDialog appsDialog;
    DashAppGrid appsGrid;
    AlertDialog settingsDialog,aboutDialog;
    DashSettingsGrid settingsGrid;
    private String installedVersion;
    final Rect editSurfaceBounds=new Rect(),editDockBounds=new Rect();
    String dockBeforeEdit="";
    final float[] dockSizesBefore=new float[8];
    ArrayList<String> dockOrder;
    Bitmap smallArtwork;
    Location latestFix;
    final SimpleDateFormat clockFormat=new SimpleDateFormat("HH:mm",Locale.US);
    final SimpleDateFormat dayFormat=new SimpleDateFormat("EEEE, dd MMM yyyy  ·  'Malaysia'",Locale.US);
    static final String[] DOCK_NAMES={"Music","Bluetooth","Radio","Video","Files","Equalizer","DiAuto","YouTube"};
    static final String[] DOCK_SYMBOLS={"♫","ᛒ","FM","▶","▣","▥","▱","▶"};
    static final int[] DOCK_COLORS={0xffec4d79,0xff288ff0,0xff9459ec,0xffec7148,0xffe7a52b,0xff6e8199,0xff239366,0xff336bf2};
    DashAppearance appearance;
    DashPanelCompatibility panelCompatibility;
    DashPerformance performance;
    DashRadioRuntime radioRuntime;
    DashRadioExport radioExport;
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
    volatile int lyricGeneration;
    String prayerRenderKey="";
    final Handler handler=new Handler(Looper.getMainLooper());
    final ExecutorService io=Executors.newSingleThreadExecutor();
    final ExecutorService lyricsIo=Executors.newSingleThreadExecutor();
    final ExecutorService prayerIo=Executors.newSingleThreadExecutor();
    String lyricKey="", prayerStatus="", requestedZone="";
    DashLyrics lyrics=new DashLyrics();
    final Runnable tick=new Runnable(){public void run(){
        if(!visible)return;refreshClock();
        if(SystemClock.elapsedRealtime()-fixAt>10000){setText(speed,"— km/h");setText(gps,"GPS · Waiting for signal");previous=null;}
        updateMediaPosition();handler.postDelayed(this,1000);
    }};
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Lang.wrap(base));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        TrustedClock.initialize(this);clockFormat.setTimeZone(JakimSchedule.MY);dayFormat.setTimeZone(JakimSchedule.MY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN);
        float density=getResources().getDisplayMetrics().density;
        scale=Math.min(getResources().getDisplayMetrics().widthPixels/1280f,getResources().getDisplayMetrics().heightPixels/720f)/density;
        tripMetres=dashPrefs().getFloat("trip",0);
        appearance=new DashAppearance(this,dashPrefs(),io,unit(),this::applyStyle);
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
    android.graphics.drawable.Drawable background(int color){return appearance.control(color,px(20));}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void pad(View v,int n){v.setPadding(px(n),px(n),px(n),px(n));}
    TextView button(String s,Runnable action){TextView v=text(s,17,TEXT);v.setGravity(Gravity.CENTER);v.setBackground(background(PANEL));pad(v,12);v.setMinHeight(px(56));v.setOnClickListener(w->action.run());v.setContentDescription(s);return v;}
    void equal(LinearLayout parent,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(px(5),px(4),px(5),px(4));parent.addView(child,p);}
    LinearLayout iconButton(String label,String symbol,int color,Runnable action){
        float iconScale=Math.max(.7f,Math.min(1.3f,dashPrefs().getFloat("dock_icon_scale_"+label,1)));
        return new DashDockItem(this,unit(),iconScale,label,symbol,color,action,neon());
    }
    LinearLayout card(){LinearLayout l=column();l.setBackground(appearance.panel(PANEL,px(20)));pad(l,14);return l;}
    void build(){
        root=row();root.setBackground(appearance.backdrop());pad(root,14);root.setPadding(root.getPaddingLeft(),root.getPaddingTop(),root.getPaddingRight(),px(4));setContentView(root);
        LinearLayout nav=column();nav.setGravity(Gravity.CENTER);root.addView(nav,new LinearLayout.LayoutParams(px(96),-1));
        LinearLayout identity=column();identity.setGravity(Gravity.CENTER);identity.setContentDescription("NexaDash");ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_launcher);identity.addView(logo,new LinearLayout.LayoutParams(px(36),px(36)));TextView brand=text("NexaDash",14,BLUE);brand.setGravity(Gravity.CENTER);identity.addView(brand,new LinearLayout.LayoutParams(-1,px(28)));nav.addView(identity,new LinearLayout.LayoutParams(-1,px(84)));
        for(String item:new String[]{"⌂\nHome","▦\nApps","⚙\nSettings"}){TextView b=button(item,()->{if(item.contains("Apps"))apps();else if(item.contains("Settings"))settings();});nav.addView(b,new LinearLayout.LayoutParams(-1,px(90)));}
        main=column();root.addView(main,new LinearLayout.LayoutParams(0,-1,1));pad(main,10);main.setPadding(main.getPaddingLeft(),main.getPaddingTop(),main.getPaddingRight(),px(4));
        LinearLayout header=row();clock=text("",38,TEXT);clock.setSingleLine(true);clock.setIncludeFontPadding(false);clock.setPadding(px(10),0,px(8),0);
        if(Build.VERSION.SDK_INT>=26)clock.setAutoSizeTextTypeUniformWithConfiguration(px(18),px(38),1,android.util.TypedValue.COMPLEX_UNIT_PX);
        clock.setGravity(Gravity.CENTER);header.addView(clock,new LinearLayout.LayoutParams(px(164),px(55)));
        header.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        editButton=button("Edit layout",this::editLayoutMenu);LinearLayout.LayoutParams editBounds=new LinearLayout.LayoutParams(-2,px(55));editBounds.setMargins(px(10),0,px(10),0);header.addView(editButton,editBounds);
        TextView access=button("Media access  ·  ⚙",this::settings);header.addView(access,new LinearLayout.LayoutParams(-2,px(55)));main.addView(header);clock.setOnClickListener(v->clockDialog());
        date=text("",16,MUTED);main.addView(date,new LinearLayout.LayoutParams(-1,px(27)));
        prayerBar=row();prayerBar.setBackground(appearance.panel(0xff0b1c2b,px(20)));prayerBar.setOnClickListener(v->prayerDialog());main.addView(prayerBar,new LinearLayout.LayoutParams(-1,px(66)));
        next=text("",13,BLUE);main.addView(next,new LinearLayout.LayoutParams(-1,px(23)));
        buildVehicleContent();
        primaryMedia=newMediaPanel();bindPrimaryMedia();
        if(!dashPrefs().getBoolean("bare_dock_v4",false)){
            dashPrefs().edit().remove("panel_layout_v1_vehicle").remove("panel_layout_v1_media").remove("panel_layout_v1_shortcuts").remove("panel_layout_v1_dock").putBoolean("bare_dock_v4",true).apply();
        }
        layoutSurface=new DashLayoutSurface(this,dashPrefs(),unit());main.addView(layoutSurface,new LinearLayout.LayoutParams(-1,0,1));
        leftPane=new Pane("vehicle",dashPrefs().getString("pane_vehicle","car"));
        rightPane=new Pane("media",dashPrefs().getString("pane_media","media"));
        layoutSurface.addPanel("vehicle",leftPane.box);layoutSurface.addPanel("media",rightPane.box);
        dock=row();buildDock();layoutSurface.addPanel("dock",dock);
        footerText=text(footerLabel(),14,MUTED);pad(footerText,10);footerText.setBackground(background(0xff0c1b29));
        footerText.setOnClickListener(v->{if(editMode!=0)finishEditing(false);else shortcut("Vehicle settings");});footerText.setOnLongClickListener(v->{if(editMode==0)DashShortcuts.choose(this,"Vehicle settings");return true;});main.addView(footerText,new LinearLayout.LayoutParams(-1,px(44)));footerText.setVisibility(View.GONE);applyStyle();
    }
    float unit(){return scale*getResources().getDisplayMetrics().density;}
    boolean neon(){return dashPrefs().getBoolean("style_neon",true);}
    static void setText(TextView view,CharSequence value){if(view!=null&&!android.text.TextUtils.equals(view.getText(),value))view.setText(value);}
    void buildVehicleContent(){
        vehicleContent=column();
        LinearLayout vehicleHeader=row();TextView model=text("PROTON SAGA",16,MUTED);vehicleHeader.addView(model,new LinearLayout.LayoutParams(0,-2,1));
        vehicleMode=text("View ▾",14,BLUE);vehicleMode.setGravity(Gravity.CENTER);vehicleMode.setOnClickListener(v->vehicleDialog());vehicleHeader.addView(vehicleMode,new LinearLayout.LayoutParams(px(66),px(34)));
        speed=text("— km/h",22,TEXT);vehicleHeader.addView(speed);vehicleContent.addView(vehicleHeader);
        vehicleStage=new FrameLayout(this);vehicleContent.addView(vehicleStage,new LinearLayout.LayoutParams(-1,0,1));
        angles=new SagaAngleView(this,io,dashPrefs());
        angles.setOnClickListener(v->{if(vehicleVideo)startVehicleVideo();else angles.setFrame(angles.frame()+1);});vehicleStage.addView(angles,new FrameLayout.LayoutParams(-1,-1));
        vehicleHint=text("",13,MUTED);vehicleHint.setGravity(Gravity.CENTER);vehicleContent.addView(vehicleHint);
        vehicleVideo="video".equals(dashPrefs().getString("vehicle_view","angles"));updateVehicleLabels();
        gps=text("GPS · Waiting for signal",14,BLUE);vehicleContent.addView(gps);trip=text("",14,MUTED);vehicleContent.addView(trip);
        trip.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Reset GPS trip?").setPositiveButton("Reset",(d,w)->{tripMetres=0;saveTrip();}).setNegativeButton("Cancel",null).show();return true;});updateTrip();
    }
    DashMediaPanel newMediaPanel(){return new DashMediaPanel(this,unit(),()->{if(media!=null)media.command(-1);},()->{if(media!=null)media.command(0);},()->{if(media!=null)media.command(1);},()->new AlertDialog.Builder(this).setTitle("Lyrics · LRCLIB").setMessage(lyrics.plain).setPositiveButton("Close",null).show());}
    void bindPrimaryMedia(){title=primaryMedia.title;artist=primaryMedia.artist;source=primaryMedia.source;lyric=primaryMedia.lyric;play=primaryMedia.play;progress=primaryMedia.progress;artwork=primaryMedia.artwork;}
    final class Pane {
        final String key;final LinearLayout box;final FrameLayout slot;final TextView selector;
        String mode;DashNavigationView navigation;DashMediaPanel mediaPanel;DashSourcePanel sourcePanel;
        DashWebPlayer webPlayer;DashFilePlayer filePlayer;DashNativeFrame nativeFrame;DashFmPanel fmPanel;
        Pane(String key,String initial){
            this.key=key;box=card();LinearLayout head=row();TextView caption=text(key.equals("vehicle")?"LEFT PANEL":"RIGHT PANEL",11,MUTED);head.addView(caption,new LinearLayout.LayoutParams(0,-1,1));
            selector=text("Switch ▾",14,BLUE);selector.setGravity(Gravity.CENTER);selector.setOnClickListener(v->menu());selector.setContentDescription("Switch "+key+" panel");head.addView(selector,new LinearLayout.LayoutParams(px(142),-1));box.addView(head,new LinearLayout.LayoutParams(-1,px(27)));
            slot=new FrameLayout(DashboardActivity.this);box.addView(slot,new LinearLayout.LayoutParams(-1,0,1));setMode(initial);
        }
        void menu(){
            String[] labels=key.equals("vehicle")?new String[]{"Car preview","YouTube web · in panel (beta)","Video file · in panel","TizenTube · native panel (experimental)","Native app · experimental","Navigation preview","YouTube · external app controls","TizenTube · external app controls","Media controls","Radio FM · in panel"}:new String[]{"Media controls","YouTube web · in panel (beta)","Video file · in panel","TizenTube · native panel (experimental)","Native app · experimental","Navigation preview","Radio FM · in panel","YouTube · external app controls","TizenTube · external app controls"};
            String[] values=key.equals("vehicle")?new String[]{"car","youtube_web","video_file","native_tizentube","native_app","navigation","youtube","tizentube","media","radio"}:new String[]{"media","youtube_web","video_file","native_tizentube","native_app","navigation","radio","youtube","tizentube"};
            java.util.List<String> menuLabels=new ArrayList<>(),menuValues=new ArrayList<>();
            boolean next="com.shazwan.nexadash.next".equals(getPackageName());
            for(int i=0;i<values.length;i++){if(next&&("native_tizentube".equals(values[i])||"native_app".equals(values[i])))continue;menuLabels.add(labels[i]);menuValues.add(values[i]);}
            if(next){menuLabels.add(0,"TizenTube · native Next companion (test)");menuValues.add(0,"native_tizentube_next");menuLabels.add(0,"Google Maps · native Next companion (test)");menuValues.add(0,"native_maps");}
            new AlertDialog.Builder(DashboardActivity.this).setTitle("Switch panel").setItems(menuLabels.toArray(new String[0]),(d,i)->{String picked=menuValues.get(i);if("native_maps".equals(picked))selectNativeTarget(3);else if("native_tizentube_next".equals(picked))selectNativeTarget(4);else if("native_tizentube".equals(picked))selectNativeTarget(1);else setMode(picked);}).setNegativeButton("Cancel",null).show();
        }
        void selectNativeTarget(int index){
            dashPrefs().edit().putInt("native_target_"+key,index).apply();
            setMode("native_app");
        }
        void setMode(String requested){
            String valid=Arrays.asList("car","navigation","youtube","tizentube","media","radio","youtube_web","video_file","native_app").contains(requested)?requested:"media";
            if(valid.equals("car")&&!key.equals("vehicle"))valid="media";
            if("radio".equals(valid)&&"radio".equals(mode)&&fmPanel!=null)return;
            Pane other=key.equals("vehicle")?rightPane:leftPane;
            if(other!=null&&inlineConflict(valid,other.mode))other.setMode(other.key.equals("vehicle")?"car":"media");
            if(other!=null&&"radio".equals(valid)&&"radio".equals(other.mode))other.setMode(other.key.equals("vehicle")?"car":"media");
            if(navigation!=null){navigation.close();navigation=null;}
            if(webPlayer!=null){webPlayer.close();webPlayer=null;}
            if(filePlayer!=null){filePlayer.close();filePlayer=null;}
            if(nativeFrame!=null){nativeFrame.close();nativeFrame=null;}
            if(fmPanel!=null){fmPanel.close();fmPanel=null;}
            if("car".equals(mode))releaseVehicleResources();
            slot.removeAllViews();mode=valid;mediaPanel=null;sourcePanel=null;dashPrefs().edit().putString("pane_"+key,mode).apply();
            String caption=mode.equals("car")?"Car":mode.equals("navigation")?"Map preview":mode.equals("media")?"Media controls":mode.equals("youtube_web")?"YouTube web":mode.equals("video_file")?"Video file":mode.equals("native_app")?"Native app lab":mode.equals("youtube")?"YT app controls":mode.equals("tizentube")?"TizenTube external":"Radio controls";selector.setText(caption+" ▾");
            if(mode.equals("car")){detach(vehicleContent);slot.addView(vehicleContent,new FrameLayout.LayoutParams(-1,-1));}
            else if(mode.equals("media")){mediaPanel=key.equals("media")?primaryMedia:newMediaPanel();detach(mediaPanel);slot.addView(mediaPanel,new FrameLayout.LayoutParams(-1,-1));}
            else if(mode.equals("navigation")){navigation=new DashNavigationView(DashboardActivity.this,()->shortcut("Navigation"));slot.addView(navigation,new FrameLayout.LayoutParams(-1,-1));if(latestFix!=null)navigation.setLocation(latestFix);if(visible&&editMode==0)navigation.resume();}
            else if(mode.equals("youtube_web")){webPlayer=new DashWebPlayer(DashboardActivity.this);slot.addView(webPlayer,new FrameLayout.LayoutParams(-1,-1));if(visible&&editMode==0){webPlayer.resume();webPlayer.open("");}}
            else if(mode.equals("native_app")){
                nativeFrame=new DashNativeFrame(DashboardActivity.this,key);
                int savedTarget=0;try{savedTarget=dashPrefs().getInt("native_target_"+key,0);}catch(ClassCastException ignored){}
                nativeFrame.select(savedTarget);
                nativeFrame.setOnTargetSelected(index->dashPrefs().edit().putInt("native_target_"+key,index).apply());
                slot.addView(nativeFrame,new FrameLayout.LayoutParams(-1,-1));if(visible&&editMode==0)nativeFrame.resume();
            }
            else if(mode.equals("video_file")){
                String saved=dashPrefs().getString("panel_video_uri","");Uri uri=saved.startsWith("content://")?Uri.parse(saved):null;
                filePlayer=new DashFilePlayer(DashboardActivity.this,io,uri,()->pickPanelVideo(this));slot.addView(filePlayer,new FrameLayout.LayoutParams(-1,-1));if(visible&&editMode==0)filePlayer.resume();
            }
            else if(mode.equals("radio")){
                fmPanel=new DashFmPanel(DashboardActivity.this,unit(),DashboardActivity.this::showRadioRuntime);slot.addView(fmPanel,new FrameLayout.LayoutParams(-1,-1));if(visible&&editMode==0)fmPanel.resume();
            }
            else{
                String name=mode.equals("youtube")?"YouTube":mode.equals("tizentube")?"TizenTube":"Radio";
                String app=mode.equals("youtube")?"YouTube app":name;
                sourcePanel=new DashSourcePanel(DashboardActivity.this,unit(),name,()->shortcut(app),()->DashShortcuts.choose(DashboardActivity.this,app),()->media.command(-1),()->media.command(0),()->media.command(1));
                if(mode.equals("tizentube")&&!"com.shazwan.nexadash.next".equals(getPackageName()))sourcePanel.offerNativePanel(()->selectNativeTarget(1));
                if(mode.equals("radio"))sourcePanel.offerRadioCheck(DashboardActivity.this::showRadioRuntime);
                slot.addView(sourcePanel,new FrameLayout.LayoutParams(-1,-1));
            }
            if(leftPane!=null)updateVehicleResources();styleButtons(box);refreshMedia();
        }
        void pause(){if(fmPanel!=null)fmPanel.pause();if(nativeFrame!=null)nativeFrame.pause();if(navigation!=null)navigation.pause();if(webPlayer!=null)webPlayer.pause();if(filePlayer!=null)filePlayer.pause();}
        void resume(){if(visible&&editMode==0){if(fmPanel!=null)fmPanel.resume();if(navigation!=null)navigation.resume();if(webPlayer!=null)webPlayer.resume();if(filePlayer!=null)filePlayer.resume();if(nativeFrame!=null)nativeFrame.resume();}}
        void close(){if(fmPanel!=null)fmPanel.close();if(nativeFrame!=null)nativeFrame.close();if(navigation!=null)navigation.close();if(webPlayer!=null)webPlayer.close();if(filePlayer!=null)filePlayer.close();}
    }
    static boolean inlineMode(String mode){return "youtube_web".equals(mode)||"video_file".equals(mode)||"native_app".equals(mode);}
    static boolean inlineConflict(String a,String b){return inlineMode(a)&&inlineMode(b)&&!("native_app".equals(a)&&"native_app".equals(b));}
    boolean inlineSelected(){return leftPane!=null&&inlineMode(leftPane.mode)||rightPane!=null&&inlineMode(rightPane.mode);}
    void pickPanelVideo(Pane pane){
        pendingVideoPane=pane.key;dashPrefs().edit().putString("panel_video_target",pane.key).apply();
        try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),PICK_PANEL_VIDEO);}
        catch(ActivityNotFoundException|SecurityException unavailable){
            try{startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),PICK_PANEL_VIDEO);}
            catch(ActivityNotFoundException|SecurityException missing){toast("No video file picker is available on this device");}
        }
    }
    static void detach(View view){if(view.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)view.getParent()).removeView(view);}
    void buildDock(){
        dock.removeAllViews();String saved=dashPrefs().getString("dock_order",android.text.TextUtils.join(",",DOCK_NAMES));LinkedHashSet<String> order=new LinkedHashSet<>();for(String s:saved.split(",")){if(s.equals("Car Link"))s="DiAuto";else if(s.equals("DSP"))s="YouTube";if(Arrays.asList(DOCK_NAMES).contains(s))order.add(s);}Collections.addAll(order,DOCK_NAMES);dockOrder=new ArrayList<>(order);
        for(String name:dockOrder){int index=Arrays.asList(DOCK_NAMES).indexOf(name);LinearLayout item=iconButton(name,DOCK_SYMBOLS[index],DOCK_COLORS[index],()->{if(editMode==2)iconSizeDialog(name);else if(editMode==0)shortcut(name);});
            styleButtons(item);item.setTag(name);item.setOnLongClickListener(v->{if(editMode==2){v.startDragAndDrop(ClipData.newPlainText("dock",name),new View.DragShadowBuilder(v),name,0);return true;}if(editMode==0){if(name.equals("Radio")){shortcut("Radio");Pane target=radioPane();if(target!=null)target.fmPanel.chooseSource();}else DashShortcuts.choose(this,name);}return true;});
            item.setOnDragListener((v,event)->{
                if(editMode!=2||!(event.getLocalState() instanceof String))return false;
                if(event.getAction()==DragEvent.ACTION_DROP){String moving=(String)event.getLocalState();int from=dockOrder.indexOf(moving),to=dockOrder.indexOf(name);if(from>=0&&to>=0&&from!=to){dockOrder.remove(from);dockOrder.add(to,moving);dashPrefs().edit().putString("dock_order",android.text.TextUtils.join(",",dockOrder)).apply();buildDock();}return true;}
                return true;
            });item.setContentDescription(name+(editMode==2?". Long press and drag to reorder.":name.equals("Radio")?". Long press to choose FM source.":". Long press to choose app."));equal(dock,item);
        }
        if(media!=null)updateDockSelection(media.current==null?"":media.current.getPackageName(),media.playing());
    }
    void editLayoutMenu(){
        if(editMode!=0){finishEditing(true);return;}
        new AlertDialog.Builder(this).setTitle("Dashboard layout").setItems(new String[]{"Move / resize panels","Reorder dock icons","Reset default layout","Neon outline: "+(neon()?"On":"Off")},(d,i)->{
            if(i<2){dockBeforeEdit=dashPrefs().getString("dock_order",android.text.TextUtils.join(",",DOCK_NAMES));for(int n=0;n<DOCK_NAMES.length;n++)dockSizesBefore[n]=dashPrefs().getFloat("dock_icon_scale_"+DOCK_NAMES[n],1);editMode=i+1;revealFooter();layoutSurface.setEditing(i==0);releaseVehicleResources();leftPane.pause();rightPane.pause();buildDock();editButton.setText("Done ✓");footerText.setText(i==0?"Drag a panel to move · Bottom-right handle to resize · Tap here to cancel":"Tap icon to resize · Long press + drag to reorder · Tap here to cancel");}
            else if(i==2){layoutSurface.resetLayout();android.content.SharedPreferences.Editor reset=dashPrefs().edit().remove("dock_order");for(String name:DOCK_NAMES)reset.remove("dock_icon_scale_"+name);reset.apply();buildDock();leftPane.setMode("car");rightPane.setMode("media");}
            else{boolean enabled=!neon();android.content.SharedPreferences.Editor edit=dashPrefs().edit().putBoolean("style_neon",enabled);if(enabled&&appearance.intensity()==0)edit.putInt("appearance_neon",65);edit.apply();applyStyle();}
        }).setNegativeButton("Close",null).show();
    }
    void iconSizeDialog(String name){
        LinearLayout content=column();pad(content,20);TextView value=text("",18,TEXT);content.addView(value);SeekBar slider=new SeekBar(this);slider.setMax(60);slider.setProgress(Math.round((dashPrefs().getFloat("dock_icon_scale_"+name,1)-.7f)*100));content.addView(slider);value.setText("Icon size: "+(70+slider.getProgress())+"%");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){value.setText("Icon size: "+(70+p)+"%");}});
        new AlertDialog.Builder(this).setTitle(name+" icon").setView(content).setPositiveButton("Apply",(d,w)->{dashPrefs().edit().putFloat("dock_icon_scale_"+name,.7f+slider.getProgress()/100f).apply();buildDock();}).setNeutralButton("Default",(d,w)->{dashPrefs().edit().remove("dock_icon_scale_"+name).apply();buildDock();}).setNegativeButton("Cancel",null).show();
    }
    void finishEditing(boolean save){
        if(editMode==0)return;if(layoutSurface.isEditing()){if(save)layoutSurface.setEditing(false);else layoutSurface.cancelEditing();}
        if(!save){android.content.SharedPreferences.Editor restore=dashPrefs().edit().putString("dock_order",dockBeforeEdit);for(int n=0;n<DOCK_NAMES.length;n++)restore.putFloat("dock_icon_scale_"+DOCK_NAMES[n],dockSizesBefore[n]);restore.apply();}
        editMode=0;revealFooter();buildDock();editButton.setText("Edit layout");footerText.setText(footerLabel());leftPane.resume();rightPane.resume();updateVehicleResources();
    }
    void applyStyle(){
        if(appearance==null||root==null)return;root.setBackground(appearance.backdrop());appearance.refresh();
        if(leftPane!=null)leftPane.box.setBackground(appearance.panel(PANEL,px(20)));
        if(rightPane!=null)rightPane.box.setBackground(appearance.panel(PANEL,px(20)));
        if(prayerBar!=null)prayerBar.setBackground(appearance.panel(0xff0b1c2b,px(20)));
        if(dock!=null)dock.setBackground(null);
        if(clock!=null){((View)clock.getParent()).setBackground(null);clock.setBackground(appearance.synthwave()?appearance.panel(0x44080b1f,px(18)):background(PANEL));clock.setTypeface(appearance.synthwave()?Typeface.create("monospace",Typeface.BOLD):Typeface.DEFAULT);}
        if(date!=null)date.setTextColor(appearance.synthwave()?DashAppearance.CYAN:MUTED);
        styleButtons(root);
    }
    void styleButtons(View view){
        if(view instanceof DashDockItem){((DashDockItem)view).style(appearance.synthwave(),appearance.panelAlpha(),appearance.strength(),neon(),appearance.reduceMotion());return;}
        android.graphics.drawable.Drawable background=view.getBackground();Integer original=null;
        if(background instanceof DashAppearance.Control)original=((DashAppearance.Control)background).original;
        else if(background instanceof GradientDrawable){android.content.res.ColorStateList color=((GradientDrawable)background).getColor();if(color!=null)original=color.getDefaultColor();}
        if(original!=null)view.setBackground(appearance.control(original,px(20)));
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)styleButtons(group.getChildAt(i));}
    }
    void refreshClock(){
        TrustedClock.Reading time=TrustedClock.read(this);long now=time.utc;Date instant=new Date(now);
        setText(clock,clockFormat.format(instant));setText(date,dayFormat.format(instant)+" · "+TrustedClock.label(this));
        String zone=JakimSchedule.zone(this);JakimSchedule.Day day=time.verified?JakimSchedule.readyAt(this,now):null;
        long target=day==null?0:JakimSchedule.readyNext(this,now);
        String renderKey=zone+time.verified+(day==null?"missing":day.date)+target;
        boolean rebuild=!renderKey.equals(prayerRenderKey);
        if(rebuild){prayerBar.removeAllViews();prayerRenderKey=renderKey;}
        if(day==null){if(rebuild){TextView prompt=text(!time.verified?"JAKIM · Waiting for verified time — tap the clock":zone.isEmpty()?"JAKIM  ·  Waiting for GPS — tap for prayer settings":zone+(JakimSchedule.ready(this)?"  ·  Today’s offline timetable unavailable/expired — tap to download":"  ·  Loading offline timetable…"),17,BLUE);pad(prompt,12);prayerBar.addView(prompt);}setText(next,(time.verified?prayerLocationLabel():"Countdown and prayer alerts wait for GPS time or your confirmation")+" · "+prayerStatus);}
        else{
            if(rebuild){TextView zoneView=text(zone+"\nJAKIM",12,MUTED);pad(zoneView,10);prayerBar.addView(zoneView);}
            if(rebuild)for(int i=0;i<6;i++){TextView v=text(JakimSchedule.NAMES[i]+"\n"+JakimSchedule.time(day.times[i]),16,day.times[i]==target?BLUE:TEXT);v.setGravity(Gravity.CENTER);if(rebuild)prayerBar.addView(v,new LinearLayout.LayoutParams(0,-1,1));}
            if(target>0){long sec=(target-now)/1000;setText(next,"Next prayer in "+twoDigits(sec/3600)+":"+twoDigits(sec/60%60)+":"+twoDigits(sec%60)+" · Official JAKIM times · "+zone+" · "+prayerLocationLabel());}else setText(next,"Today's timetable ended · Download tomorrow's schedule · "+prayerLocationLabel());
        }
        String requestKey=zone+(time.verified?JakimSchedule.dateKey(now):"unverified");
        if(!requestKey.equals(requestedZone)){requestedZone=requestKey;fetchPrayer(false);}
    }
    static String twoDigits(long value){return (value<10?"0":"")+value;}
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
                +"\nNexaDash time: "+f.format(new Date(TrustedClock.now(this)))
                +"\n\nGPS time works offline after a fresh satellite fix. It changes NexaDash's clock only. The verified time is kept for this boot, up to 24 hours without another fix."
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
        if(media==null)return;MediaController controller=media.current;MediaMetadata metadata=controller==null?null:controller.getMetadata();
        String t=metadata==null?"":metadata.getString(MediaMetadata.METADATA_KEY_TITLE),a=metadata==null?"":metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if(t==null)t="";if(a==null)a="";boolean playing=media.playing();String pkg=controller==null?"":controller.getPackageName();String name=controller==null?"NOW PLAYING":appName(pkg);
        Bitmap cover=metadata==null?null:metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(cover==null&&metadata!=null)cover=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        int token=cover==null?0:System.identityHashCode(cover);
        if(token!=artToken){artToken=token;smallArtwork=null;if(cover!=null&&!cover.isRecycled()){int longest=Math.max(cover.getWidth(),cover.getHeight());smallArtwork=longest>128?Bitmap.createScaledBitmap(cover,Math.max(1,cover.getWidth()*128/longest),Math.max(1,cover.getHeight()*128/longest),true):cover;}}
        primaryMedia.update(t,a.isEmpty()?media.problem:a,name,playing,smallArtwork);
        for(Pane pane:new Pane[]{leftPane,rightPane})if(pane!=null){
            if(pane.mediaPanel!=null&&pane.mediaPanel!=primaryMedia)pane.mediaPanel.update(t,a.isEmpty()?media.problem:a,name,playing,smallArtwork);
            if(pane.sourcePanel!=null)pane.sourcePanel.setTrack(t,a,sourceMatches(pane.mode,pkg),playing);
        }
        String key=t+"\n"+a;if(!key.equals(lyricKey)){lyricGeneration++;lyricKey=key;lyrics=new DashLyrics();lyrics.plain=t.isEmpty()?"Play media to show track details":a.isEmpty()?"Artist metadata is needed to find lyrics":"Tap Lyrics in Settings to look up this track";if(dashPrefs().getBoolean("lyrics",false)&&!t.isEmpty()&&!a.isEmpty())fetchLyrics(t,a,key);}
        updateDockSelection(pkg,playing);updateMediaPosition();
    }
    void updateDockSelection(String pkg,boolean playing){
        if(dock==null)return;for(int i=0;i<dock.getChildCount();i++){View child=dock.getChildAt(i);if(child instanceof DashDockItem&&child.getTag() instanceof String)child.setSelected(playing&&DashShortcuts.matches(this,(String)child.getTag(),pkg));}
    }
    boolean sourceMatches(String mode,String pkg){
        if(pkg.isEmpty())return false;
        String key=mode.equals("youtube")?"YouTube app":mode.equals("tizentube")?"TizenTube":"Radio";
        return DashShortcuts.matches(this,key,pkg);
    }
    void updateMediaPosition(){
        if(media==null||primaryMedia==null)return;long position=media.position();String line=lyrics.at(position);
        primaryMedia.updatePosition(position,line);
        if(leftPane!=null&&leftPane.mediaPanel!=null&&leftPane.mediaPanel!=primaryMedia)leftPane.mediaPanel.updatePosition(position,line);
    }
    void fetchLyrics(String t,String a,String key){
        lyrics.plain="Finding lyrics…";final int generation=lyricGeneration;
        lyricsIo.execute(()->{if(generation!=lyricGeneration)return;DashLyrics found=new DashLyrics();try{found=DashLyrics.parse(new JSONObject(DashNetwork.get("https://lrclib.net/api/get?track_name="+URLEncoder.encode(t,"UTF-8")+"&artist_name="+URLEncoder.encode(a,"UTF-8"))));}catch(Exception ignored){}final DashLyrics result=found;handler.post(()->{if(!isDestroyed()&&generation==lyricGeneration&&lyricKey.equals(key)&&dashPrefs().getBoolean("lyrics",false)){lyrics=result;updateMediaPosition();}});});
    }
    String appName(String pkg){try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){return pkg;}}
    void sources(){List<MediaController> sessions=media.sessions();List<String> names=new ArrayList<>();names.add("Auto · Follow playing media");for(MediaController c:sessions)names.add(appName(c.getPackageName()));names.add("Open Bluetooth");names.add("Radio FM · in panel");names.add("Open YouTube / TizenTube");new AlertDialog.Builder(this).setTitle("Media source").setItems(names.toArray(new String[0]),(d,i)->{if(i==0)media.choose("");else if(i<=sessions.size())media.choose(sessions.get(i-1).getPackageName());else if(i==sessions.size()+1)shortcut("Bluetooth");else if(i==sessions.size()+2)shortcut("Radio");else launchVideo();}).show();}
    String versionName(){
        if(installedVersion==null)try{installedVersion=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(PackageManager.NameNotFoundException ignored){installedVersion="unknown";}
        return installedVersion;
    }
    String footerLabel(){return "Native Lab · Vehicle settings     ·     NexaDash v"+versionName();}
    String[] settingsLabels(){return new String[]{"Prayer zone & timetable","Media / notification access","Lyrics: "+(dashPrefs().getBoolean("lyrics",false)?"On":"Off"),"Location permission","Android settings","AI assistant","Screen lock","Power saver","AI profiles","Default launcher","Clock / GPS time","Dashboard layout","Appearance","App panel compatibility","About NexaDash","Vehicle settings","Performance check","Radio runtime paths","Export Radio files"};}
    void settings(){
        closeApps();
        if(!visible||isFinishing()||isDestroyed()||(settingsDialog!=null&&settingsDialog.isShowing()))return;
        DashSettingsGrid grid=new DashSettingsGrid(this,appearance,settingsLabels(),index->{closeSettings();settingsAction(index);});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Dashboard settings · v"+versionName())
            .setView(grid).setNegativeButton("Close",null).create();
        settingsDialog=dialog;settingsGrid=grid;
        dialog.setOnDismissListener(d->{grid.close();if(settingsDialog==dialog)settingsDialog=null;if(settingsGrid==grid)settingsGrid=null;});dialog.show();
        Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawable(new DashPanelDrawable(0xee0b1c2b,appearance.accent(),px(24),appearance.neon(),appearance.strength(),appearance.glass()));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.35f);
            android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();window.setLayout(Math.round(metrics.widthPixels*.96f),Math.round(metrics.heightPixels*.88f));}
    }
    void closeSettings(){if(settingsGrid!=null){settingsGrid.close();settingsGrid=null;}if(settingsDialog!=null){settingsDialog.dismiss();settingsDialog=null;}}
    void showRadioRuntime(){if(radioRuntime==null)radioRuntime=new DashRadioRuntime();radioRuntime.show(this,io);}
    void exportRadioFiles(){if(radioExport==null)radioExport=new DashRadioExport();radioExport.choose(this);}
    void showPanelCompatibility(){if(panelCompatibility==null)panelCompatibility=new DashPanelCompatibility();panelCompatibility.show(this,io);}
    void settingsAction(int i){
        try{switch(i){case 0:prayerDialog();break;case 1:startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));break;case 2:new AlertDialog.Builder(this).setTitle("Online lyrics").setMessage("Send the current track title and artist to LRCLIB to find lyrics? Bluetooth itself does not supply lyrics. Synced lyrics depend on availability and playback position from the source.").setPositiveButton("Enable",(v,w)->{dashPrefs().edit().putBoolean("lyrics",true).apply();lyricKey="";refreshMedia();}).setNeutralButton("Disable",(v,w)->{dashPrefs().edit().putBoolean("lyrics",false).apply();lyricKey="";refreshMedia();}).setNegativeButton("Cancel",null).show();break;case 3:requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},7);break;case 4:startActivity(new Intent(Settings.ACTION_SETTINGS));break;case 5:startActivity(new Intent(this,AskActivity.class));break;case 6:configureLock();break;case 7:startActivity(new Intent(this,SettingsActivity.class).putExtra(SettingsActivity.EXTRA_TAB,SettingsActivity.TAB_POWER));break;case 8:startActivity(new Intent(this,SettingsActivity.class).putExtra(SettingsActivity.EXTRA_TAB,SettingsActivity.TAB_AI));break;case 9:startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));break;case 10:clockDialog();break;case 11:editLayoutMenu();break;case 12:appearance.show();break;case 13:showPanelCompatibility();break;case 14:showAbout();break;case 15:shortcut("Vehicle settings");break;case 16:if(performance==null)performance=new DashPerformance(this,()->"left="+leftPane.mode+", right="+rightPane.mode,()->visible);performance.show();break;case 17:showRadioRuntime();break;case 18:exportRadioFiles();break;}}
        catch(ActivityNotFoundException|SecurityException unavailable){toast("This setting is unavailable on this device");}
    }
    void showAbout(){
        if(aboutDialog!=null&&aboutDialog.isShowing())return;
        aboutDialog=new AlertDialog.Builder(this).setTitle("About NexaDash")
            .setMessage("Version: "+versionName()+"\nApplication ID: "+getPackageName()+"\n\nApps: installed-app icon grid\nDashboard settings: icon grid\n\nYouTube web and local video can play in a panel. App-source modes remain media controls. Search destinations and open directions/voice in Maps or Waze. The panel map remains a GPS preview; this Lab adds cooperative native hosting with matching companions only; it does not embed arbitrary original apps.")
            .setPositiveButton("Close",null).create();
        final AlertDialog shown=aboutDialog;shown.setOnDismissListener(d->{if(aboutDialog==shown)aboutDialog=null;});shown.show();
    }
    void apps(){
        closeSettings();
        if(!visible||isFinishing()||isDestroyed()||appsLoading||(appsDialog!=null&&appsDialog.isShowing()))return;
        appsLoading=true;final int generation=++appsGeneration;
        io.execute(()->{
            List<DashShortcuts.App> found=null;
            try{found=DashShortcuts.installed(this);}catch(RuntimeException ignored){}
            final List<DashShortcuts.App> list=found;
            handler.post(()->{
                appsLoading=false;
                if(generation!=appsGeneration||!visible||isFinishing()||isDestroyed())return;
                if(list==null){toast("Unable to load installed apps");return;}
                DashAppGrid grid=new DashAppGrid(this,list,io,appearance);
                AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Apps · "+list.size()+" · v"+versionName())
                    .setView(grid).setNegativeButton("Close",null).create();
                grid.setOnItemClickListener((parent,view,position,id)->{
                    if(position>=0&&position<list.size()){
                        if(DashShortcuts.start(this,DashShortcuts.launch(list.get(position))))dialog.dismiss();
                        else toast("Unable to open app");
                    }
                });
                appsDialog=dialog;appsGrid=grid;
                dialog.setOnDismissListener(d->{grid.close();if(appsDialog==dialog)appsDialog=null;if(appsGrid==grid)appsGrid=null;});dialog.show();
                Window window=dialog.getWindow();
                if(window!=null){window.setBackgroundDrawable(new DashPanelDrawable(0xee0b1c2b,appearance.accent(),px(24),appearance.neon(),appearance.strength(),appearance.glass()));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.35f);
                    android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
                    window.setLayout(Math.round(metrics.widthPixels*.96f),Math.round(metrics.heightPixels*.88f));}
            });
        });
    }
    void closeApps(){appsGeneration++;if(appsGrid!=null){appsGrid.close();appsGrid=null;}if(appsDialog!=null){appsDialog.dismiss();appsDialog=null;}}
    Pane radioPane(){return rightPane!=null&&rightPane.fmPanel!=null?rightPane:leftPane!=null&&leftPane.fmPanel!=null?leftPane:null;}
    void shortcut(String name){if(name.equals("Radio")){if(radioPane()==null&&rightPane!=null)rightPane.setMode("radio");return;}if(name.equals("YouTube")){launchOfficialYouTube();return;}if(name.equals("Navigation"))DashDirections.show(this);else DashShortcuts.open(this,name);}
    void launchOfficialYouTube(){
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
        if(launch==null||!DashShortcuts.start(this,launch))toast("Official YouTube is not installed or cannot be opened on this device.");
    }
    void launchVideo(){DashShortcuts.open(this,"YouTube");}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==DashRadioExport.REQUEST){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null){
                if(radioExport==null)radioExport=new DashRadioExport();radioExport.save(this,io,data.getData());
            }
            return;
        }
        if(request==PICK_PANEL_VIDEO){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null){
                Uri uri=data.getData();
                if(!"content".equals(uri.getScheme())){toast("Choose a video from the Android file picker");return;}
                int read=data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION;
                if(read!=0)try{getContentResolver().takePersistableUriPermission(uri,read);}catch(SecurityException ignored){}
                String target=dashPrefs().getString("panel_video_target",pendingVideoPane);Pane pane="media".equals(target)?rightPane:leftPane;
                if(pane!=null&&pane.filePlayer!=null){
                    String old=dashPrefs().getString("panel_video_uri","");
                    dashPrefs().edit().putString("panel_video_uri",uri.toString()).remove("panel_video_target").apply();pane.filePlayer.setSource(uri);
                    if(old.startsWith("content://")&&!old.equals(uri.toString()))try{getContentResolver().releasePersistableUriPermission(Uri.parse(old),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
                }
            }
            pendingVideoPane="";return;
        }
        if(request==SagaShowroom.PICK_CAR_VIDEO){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null){
                final Uri uri=data.getData();toast("Importing car video…");
                io.execute(()->{
                    try{
                        SagaShowroom.importVideo(this,uri);
                        handler.post(()->{if(!isDestroyed()){selectVehicleVideo("imported");toast("Car video ready. Tap it to play or pause.");}});
                    }catch(Exception e){handler.post(()->{if(!isDestroyed())new AlertDialog.Builder(this).setTitle("Cannot import video").setMessage("Choose a playable video up to 2 minutes and 32 MB, preferably MP4 / H.264. Your previous video has been kept.").setPositiveButton("OK",null).show();});}
                });
            }
            return;
        }
        DashShortcuts.handleResult(this,request,result,data);
    }
    void vehicleDialog(){
        String[] modes=SagaShowroom.hasImportedVideo(this)
            ?new String[]{"Car image angles (lightweight)","Choose car video…","Imported video preview"}
            :new String[]{"Car image angles (lightweight)","Choose car video…"};
        new AlertDialog.Builder(this).setTitle("Vehicle view").setItems(modes,(d,index)->{
            if(index==0)setVehicleVideo(false);
            else if(index==1){try{startActivityForResult(SagaShowroom.pickIntent(),SagaShowroom.PICK_CAR_VIDEO);}catch(ActivityNotFoundException|SecurityException e){toast("No file picker is available on this device");}}
            else selectVehicleVideo("imported");
        }).setNegativeButton("Close",null).show();
    }
    boolean carVisible(){return visible&&editMode==0&&leftPane!=null&&"car".equals(leftPane.mode);}
    void releaseShowroom(){if(showroom!=null){SagaShowroom old=showroom;showroom=null;old.pause();vehicleStage.removeView(old);}}
    void releaseVehicleResources(){releaseShowroom();if(angles!=null){angles.pause();angles.setVisibility(View.VISIBLE);}}
    void updateVehicleResources(){
        if(vehicleStage==null)return;
        if(!carVisible()){releaseVehicleResources();return;}
        if(!vehicleVideo||inlineSelected())releaseShowroom();
        boolean images=showroom==null;
        angles.setVisibility(images?View.VISIBLE:View.GONE);if(images)angles.resume();else angles.pause();
        if(showroom!=null)showroom.resume();updateVehicleLabels();
    }
    void ensureShowroom(boolean autoPlay){
        if(!carVisible()||!vehicleVideo||inlineSelected())return;
        angles.pause();angles.setVisibility(View.GONE);
        if(showroom==null){showroom=new SagaShowroom(this);vehicleStage.addView(showroom,new FrameLayout.LayoutParams(-1,-1));}
        showroom.playWhenPrepared=autoPlay;showroom.resume();if(autoPlay&&showroom.prepared&&!showroom.playing())showroom.toggle();updateVehicleLabels();
    }
    void startVehicleVideo(){if(inlineSelected()){toast("Close the video panel before playing the car preview");return;}if(editMode==0&&vehicleVideo)ensureShowroom(true);}
    void updateVehicleLabels(){
        boolean cutout=vehicleVideo&&"transparent".equals(dashPrefs().getString("vehicle_preview_source","transparent"));
        setText(vehicleMode,vehicleVideo?(cutout?"Cutout ▾":"Video ▾"):"Angles ▾");vehicleMode.setContentDescription("Choose vehicle view");
        setText(vehicleHint,vehicleVideo?(inlineSelected()?"Video panel active · car uses an image preview":showroom==null?"Tap the car to play video":"Video preview · Tap to play / pause"):"Drag: 8 image angles · Pinch: zoom · Double-tap: reset");
    }
    void selectVehicleVideo(String source){
        dashPrefs().edit().putString("vehicle_preview_source","imported".equals(source)?"imported":"transparent").apply();releaseShowroom();setVehicleVideo(true);
    }
    void setVehicleVideo(boolean useVideo){
        vehicleVideo=useVideo&&SagaShowroom.hasVideo(this);dashPrefs().edit().putString("vehicle_view",vehicleVideo?"video":"angles").remove("vehicle_video").apply();
        if(leftPane!=null&&!"car".equals(leftPane.mode))leftPane.setMode("car");
        updateVehicleResources();if(vehicleVideo&&carVisible())ensureShowroom(false);updateVehicleLabels();
    }
    void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("No browser installed");}}
    android.content.SharedPreferences dashPrefs(){return createDeviceProtectedStorageContext().getSharedPreferences("dashboard",0);}
    android.content.SharedPreferences lockPrefs(){return createDeviceProtectedStorageContext().getSharedPreferences("klikdash",0);}
    void configureLock(){
        if(!ScreenLock.hasPin(lockPrefs()))ScreenLock.create(this,pin->{ScreenLock.setPin(lockPrefs(),pin);toast("PIN saved. Open Screen lock again to lock.");});
        else new AlertDialog.Builder(this).setTitle("Screen lock").setItems(new String[]{"Lock now","Remove PIN"},(d,i)->{if(i==0){ScreenLock.setLocked(lockPrefs(),true);toast("Locked · Tap the screen to unlock");}else ScreenLock.verify(this,lockPrefs(),()->ScreenLock.clear(lockPrefs()),()->{});}).show();
    }
    final Runnable hideFooter=()->{if(footerText!=null&&editMode==0)footerText.setVisibility(View.GONE);};
    void revealFooter(){
        if(footerText==null)return;
        handler.removeCallbacks(hideFooter);
        footerText.setVisibility(editMode==0?View.GONE:View.VISIBLE);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(ScreenLock.locked(lockPrefs())){if(e.getAction()==MotionEvent.ACTION_UP)ScreenLock.verify(this,lockPrefs(),()->ScreenLock.setLocked(lockPrefs(),false),()->{});return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN)revealFooter();
        if(editMode==2){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){layoutSurface.getGlobalVisibleRect(editSurfaceBounds);dock.getGlobalVisibleRect(editDockBounds);int x=Math.round(e.getRawX()),y=Math.round(e.getRawY());blockEditTouch=editSurfaceBounds.contains(x,y)&&!editDockBounds.contains(x,y);}
            if(blockEditTouch){if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)blockEditTouch=false;return true;}
        }
        return super.dispatchTouchEvent(e);
    }
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
        latestFix=new Location(l);if(leftPane!=null&&leftPane.navigation!=null)leftPane.navigation.setLocation(latestFix);if(rightPane!=null&&rightPane.navigation!=null)rightPane.navigation.setLocation(latestFix);
        fixTime=l.getTime();fixAt=SystemClock.elapsedRealtime();setText(gps,"GPS · Connected");if(l.hasSpeed()){int kmh=Math.round(l.getSpeed()*3.6f);setText(speed,kmh+" km/h");HomeActivity.lastKmh=kmh;HomeActivity.lastKmhAt=fixAt;}else setText(speed,"— km/h");
        if(previous!=null){float distance=l.distanceTo(previous);long elapsed=l.getElapsedRealtimeNanos()-previous.getElapsedRealtimeNanos();if(elapsed>0&&elapsed<30000000000L&&distance>Math.max(l.getAccuracy(),previous.getAccuracy())&&distance/(elapsed/1e9)<70){tripMetres+=distance;saveTrip();}else if(elapsed>0&&elapsed<30000000000L&&distance<=Math.max(l.getAccuracy(),previous.getAccuracy()))return;}
        previous=l;
    }
    @Override public void onProviderDisabled(String p){if(prayerLocation!=null)prayerLocation.signalLost();latestFix=null;if(leftPane.navigation!=null)leftPane.navigation.setLocation(null);if(rightPane.navigation!=null)rightPane.navigation.setLocation(null);setText(gps,"GPS · Disabled");previous=null;setText(speed,"— km/h");}
    @Override public void onProviderEnabled(String p){}
    @Override public void onStatusChanged(String p,int s,Bundle b){}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);if(r==7&&visible)startGps();}
    @Override protected void onResume(){super.onResume();Notice.dismissExpired();visible=true;revealFooter();appearance.start();leftPane.resume();rightPane.resume();updateVehicleResources();media.start();handler.removeCallbacks(tick);tick.run();startGps();PrayerAlarm.home=new java.lang.ref.WeakReference<>(this);schedulePrayer();fetchPrayer();}
    boolean nativeRunning(){return leftPane!=null&&leftPane.nativeFrame!=null&&leftPane.nativeFrame.display!=null||rightPane!=null&&rightPane.nativeFrame!=null&&rightPane.nativeFrame.display!=null;}
    @Override protected void onStop(){pauseDashboard();super.onStop();}
    @Override protected void onPause(){if(!nativeRunning())pauseDashboard();super.onPause();}
    void pauseDashboard(){if(radioRuntime!=null)radioRuntime.close();if(performance!=null)performance.pause();handler.removeCallbacks(hideFooter);visible=false;closeApps();closeSettings();if(aboutDialog!=null){aboutDialog.dismiss();aboutDialog=null;}if(panelCompatibility!=null)panelCompatibility.close();appearance.stop();if(editMode!=0)finishEditing(false);leftPane.pause();rightPane.pause();releaseVehicleResources();if(prayerLocation!=null)prayerLocation.signalLost();handler.removeCallbacks(tick);media.stop();locations.removeUpdates(this);previous=null;dashPrefs().edit().putFloat("trip",(float)tripMetres).apply();PrayerAlarm.home.clear();}
    @Override protected void onDestroy(){if(radioExport!=null)radioExport.close();if(radioRuntime!=null)radioRuntime.close();if(performance!=null)performance.close();closeSettings();if(panelCompatibility!=null)panelCompatibility.close();appearance.stop();if(clockEventsRegistered)unregisterReceiver(clockEvents);leftPane.close();rightPane.close();io.shutdownNow();lyricsIo.shutdownNow();prayerIo.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){
        if(leftPane!=null&&leftPane.webPlayer!=null&&leftPane.webPlayer.exitFullscreen())return;
        if(rightPane!=null&&rightPane.webPlayer!=null&&rightPane.webPlayer.exitFullscreen())return;
        if(editMode!=0)finishEditing(false);
    }
}
