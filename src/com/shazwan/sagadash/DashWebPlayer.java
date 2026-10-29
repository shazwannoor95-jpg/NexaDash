package com.shazwan.sagadash;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import java.util.Locale;

/** Optional in-panel YouTube website. No installed activity hosting or native JavaScript bridge. */
final class DashWebPlayer extends LinearLayout implements AutoCloseable {
    final FrameLayout body;
    final EditText query;
    final TextView status;
    WebView web;
    private final Activity activity;
    private final TextView start;
    private final LinearLayout searchRow;
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean active,closed;
    private View customView;
    private FrameLayout fullscreenOverlay;
    private ViewGroup fullscreenParent;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private int savedSystemUi,savedWindowFlags;
    private static final int FULLSCREEN_WINDOW_FLAGS=WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

    DashWebPlayer(Activity activity){
        super(activity);this.activity=activity;setOrientation(VERTICAL);
        LinearLayout search=new LinearLayout(activity);searchRow=search;query=new EditText(activity);query.setSingleLine(true);
        query.setTextSize(13);query.setTextColor(DashboardActivity.TEXT);query.setHintTextColor(DashboardActivity.MUTED);
        query.setHint("YouTube link or search");query.setImeOptions(EditorInfo.IME_ACTION_GO);
        query.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_GO){open(query.getText().toString());return true;}return false;});
        search.addView(query,new LayoutParams(0,dp(32),1));search.addView(button("Go",()->open(query.getText().toString())),new LayoutParams(dp(44),dp(32)));addView(search);
        body=new FrameLayout(activity);body.setBackgroundColor(Color.BLACK);addView(body,new LayoutParams(-1,0,1));
        start=button("Start YouTube web\nPlays here · Internet required",()->open(query.getText().toString()));start.setTextSize(14);body.addView(start,new FrameLayout.LayoutParams(-1,-1));
        status=new TextView(activity);status.setTextSize(11);status.setTextColor(DashboardActivity.MUTED);status.setText("Website player · tap video to play");status.setMaxLines(2);addView(status,new LayoutParams(-1,-2));
        LinearLayout controls=new LinearLayout(activity);
        controls.addView(button("Back",()->{if(!exitFullscreen()&&web!=null&&web.canGoBack())web.goBack();}),new LayoutParams(0,dp(32),1));
        controls.addView(button("Search",()->{boolean show=searchRow.getVisibility()!=VISIBLE;searchRow.setVisibility(show?VISIBLE:GONE);status.setVisibility(show?VISIBLE:GONE);}),new LayoutParams(0,dp(32),1));
        controls.addView(button("Home",()->open("")),new LayoutParams(0,dp(32),1));
        controls.addView(button("Stop",()->{releaseWeb();status.setText("Stopped · tap Start to load again");}),new LayoutParams(0,dp(32),1));addView(controls);
    }
    private int dp(float value){return Math.max(1,Math.round(value*getResources().getDisplayMetrics().density));}
    private TextView button(String text,Runnable action){TextView b=new TextView(activity);b.setText(text);b.setTextColor(DashboardActivity.TEXT);b.setTextSize(13);b.setGravity(Gravity.CENTER);b.setContentDescription(text);android.graphics.drawable.GradientDrawable tile=new android.graphics.drawable.GradientDrawable();tile.setColor(DashboardActivity.PANEL);tile.setCornerRadius(dp(12));tile.setStroke(dp(1),DashboardActivity.LINE);b.setBackground(tile);b.setOnClickListener(v->action.run());return b;}
    static boolean allowed(String value){
        try{Uri uri=Uri.parse(value);String host=uri.getHost();if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null)return false;
            host=host.toLowerCase(Locale.ROOT);return host.equals("youtube.com")||host.endsWith(".youtube.com")||host.equals("youtu.be")||host.equals("consent.google.com");
        }catch(RuntimeException ignored){return false;}
    }
    static String address(String input){
        String value=input==null?"":input.trim();if(value.isEmpty())return "https://m.youtube.com/";
        if(value.startsWith("www.youtube.com/")||value.startsWith("m.youtube.com/")||value.startsWith("youtube.com/")||value.startsWith("youtu.be/"))value="https://"+value;
        if(allowed(value)){Uri uri=Uri.parse(value);if("youtube.com".equalsIgnoreCase(uri.getHost())||"www.youtube.com".equalsIgnoreCase(uri.getHost()))return uri.buildUpon().authority("m.youtube.com").build().toString();return value;}
        return "https://m.youtube.com/results?search_query="+Uri.encode(value);
    }
    void resume(){if(!closed)active=true;}
    void pause(){active=false;releaseWeb();}
    void open(String value){
        if(!active||closed)return;
        exitFullscreen();
        android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(query.getWindowToken(),0);query.clearFocus();
        if(ensureWeb()==null)return;
        // Keep the browsing surface usable even in the small default dashboard panel.
        searchRow.setVisibility(GONE);status.setVisibility(GONE);
        status.setText("Loading YouTube · tap video to play");web.loadUrl(address(value));
    }
    @SuppressLint("SetJavaScriptEnabled") WebView ensureWeb(){
        if(!active||closed)return null;
        if(web==null)try{
            WebView created=new WebView(activity);web=created;created.setBackgroundColor(Color.BLACK);
            WebSettings settings=created.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setMediaPlaybackRequiresUserGesture(true);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
            // Dashboard labels may follow accessibility font scaling; the website has its own viewport.
            settings.setTextZoom(100);created.setInitialScale(0);
            settings.setGeolocationEnabled(false);CookieManager.getInstance().setAcceptThirdPartyCookies(created,false);
            created.setWebViewClient(Build.VERSION.SDK_INT>=26?new ModernClient():new Client());
            created.setWebChromeClient(new WebChromeClient(){
                @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
                @Override public void onShowCustomView(View view,CustomViewCallback callback){
                    if(web!=created){notifyHidden(callback);return;}showFullscreen(view,callback);
                }
                @Override public void onHideCustomView(){if(web==created)exitFullscreen();}
                @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){callback.invoke(origin,false,false);}
            });
            created.setDownloadListener((url,ua,disposition,mime,length)->{status.setVisibility(VISIBLE);status.setText("Downloads are unavailable in this player");});
            start.setVisibility(GONE);body.addView(created,new FrameLayout.LayoutParams(-1,-1));
        }catch(RuntimeException|LinkageError unavailable){releaseWeb();status.setText("WebView unavailable. Update Android System WebView on this device.");return null;}
        return web;
    }
    boolean isFullscreen(){return customView!=null;}
    private static void notifyHidden(WebChromeClient.CustomViewCallback callback){
        if(callback!=null)try{callback.onCustomViewHidden();}catch(RuntimeException ignored){}
    }
    /** Hosts the WebView's existing video surface; no second browser or player is created. */
    void showFullscreen(View view,WebChromeClient.CustomViewCallback callback){
        if(customView!=null){
            if(callback!=fullscreenCallback)notifyHidden(callback);
            return;
        }
        if(view==null||view.getParent()!=null||!active||closed||web==null||activity.isFinishing()||activity.isDestroyed()){
            notifyHidden(callback);return;
        }
        View decor=activity.getWindow().getDecorView();
        if(!(decor instanceof ViewGroup)){notifyHidden(callback);return;}
        savedSystemUi=decor.getSystemUiVisibility();savedWindowFlags=activity.getWindow().getAttributes().flags&FULLSCREEN_WINDOW_FLAGS;
        customView=view;fullscreenCallback=callback;fullscreenParent=(ViewGroup)decor;
        FrameLayout overlay=new FrameLayout(activity);fullscreenOverlay=overlay;overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(true);overlay.setFocusableInTouchMode(true);overlay.setContentDescription("YouTube web fullscreen video");
        overlay.setOnKeyListener((v,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP)exitFullscreen();return true;});
        try{
            overlay.addView(view,new FrameLayout.LayoutParams(-1,-1));
            TextView exit=button("Exit fullscreen",this::exitFullscreen);exit.setContentDescription("Exit fullscreen and return to dashboard panel");
            FrameLayout.LayoutParams exitBounds=new FrameLayout.LayoutParams(dp(124),dp(44),Gravity.TOP|Gravity.END);exitBounds.setMargins(dp(12),dp(12),dp(12),dp(12));overlay.addView(exit,exitBounds);
            fullscreenParent.addView(overlay,new ViewGroup.LayoutParams(-1,-1));
            activity.getWindow().setFlags(FULLSCREEN_WINDOW_FLAGS,FULLSCREEN_WINDOW_FLAGS);
            decor.setSystemUiVisibility(savedSystemUi|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            overlay.requestFocus();
        }catch(RuntimeException unavailable){
            exitFullscreen();status.setVisibility(VISIBLE);status.setText("Fullscreen unavailable. Video remains in the panel.");
        }
    }
    /** Clear state before callbacks because the provider may synchronously call onHideCustomView. */
    boolean exitFullscreen(){
        if(customView==null)return false;
        View oldView=customView;FrameLayout overlay=fullscreenOverlay;ViewGroup parent=fullscreenParent;WebChromeClient.CustomViewCallback callback=fullscreenCallback;
        customView=null;fullscreenOverlay=null;fullscreenParent=null;fullscreenCallback=null;
        if(overlay!=null){overlay.setOnKeyListener(null);overlay.removeView(oldView);if(parent!=null)parent.removeView(overlay);}
        activity.getWindow().setFlags(savedWindowFlags,FULLSCREEN_WINDOW_FLAGS);
        activity.getWindow().getDecorView().setSystemUiVisibility(savedSystemUi);
        notifyHidden(callback);return true;
    }
    private class Client extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return block(request.getUrl().toString());}
        @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return block(url);}
        private boolean block(String url){if(allowed(url))return false;status.setVisibility(VISIBLE);status.setText("This link is unavailable in the panel. No external app was opened.");return true;}
        @Override public void onPageFinished(WebView view,String url){if(web==view){status.setText("YouTube web · tap video to play · sign-in/restricted videos may be unavailable");compactWatch(url);}}
        @Override public void doUpdateVisitedHistory(WebView view,String url,boolean reload){if(web==view)compactWatch(url);}
        private void compactWatch(String url){Uri uri=Uri.parse(url);String path=uri.getPath();if(allowed(url)&&path!=null&&(path.equals("/watch")||path.startsWith("/shorts/"))){searchRow.setVisibility(GONE);status.setVisibility(GONE);}}
        @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(web==view&&request.isForMainFrame()){status.setVisibility(VISIBLE);status.setText("Page unavailable. Check Internet/System WebView, then tap Home to retry.");}}
        @Override public void onReceivedSslError(WebView view,android.webkit.SslErrorHandler handler,android.net.http.SslError error){handler.cancel();if(web==view){status.setVisibility(VISIBLE);status.setText("Secure connection failed. Check the device date/time.");}}
    }
    @android.annotation.TargetApi(26) private final class ModernClient extends Client {
        @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail){
            main.post(()->{if(web==view){releaseWeb();status.setText("Web player stopped. Tap Start to retry.");}});return true;
        }
    }
    private void releaseWeb(){
        WebView old=web;web=null;exitFullscreen();
        if(old!=null){body.removeView(old);old.stopLoading();old.onPause();old.removeAllViews();old.destroy();}
        start.setVisibility(VISIBLE);searchRow.setVisibility(VISIBLE);status.setVisibility(VISIBLE);
    }
    @Override public void close(){closed=true;pause();}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
}
