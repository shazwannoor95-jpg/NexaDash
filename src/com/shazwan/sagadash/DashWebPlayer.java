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

    DashWebPlayer(Activity activity){
        super(activity);this.activity=activity;setOrientation(VERTICAL);
        LinearLayout search=new LinearLayout(activity);searchRow=search;query=new EditText(activity);query.setSingleLine(true);
        query.setTextSize(13);query.setTextColor(DashboardActivity.TEXT);query.setHintTextColor(DashboardActivity.MUTED);
        query.setHint("YouTube link or search");query.setImeOptions(EditorInfo.IME_ACTION_GO);
        query.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_GO){open(query.getText().toString());return true;}return false;});
        search.addView(query,new LayoutParams(0,dp(42),1));search.addView(button("Go",()->open(query.getText().toString())),new LayoutParams(dp(44),dp(42)));addView(search);
        body=new FrameLayout(activity);body.setBackgroundColor(Color.BLACK);addView(body,new LayoutParams(-1,0,1));
        start=button("Start YouTube web\nPlays here · Internet required",()->open(query.getText().toString()));start.setTextSize(14);body.addView(start,new FrameLayout.LayoutParams(-1,-1));
        status=new TextView(activity);status.setTextSize(11);status.setTextColor(DashboardActivity.MUTED);status.setText("Website player · tap video to play");status.setMaxLines(2);addView(status,new LayoutParams(-1,-2));
        LinearLayout controls=new LinearLayout(activity);
        controls.addView(button("Back",()->{if(web!=null&&web.canGoBack())web.goBack();}),new LayoutParams(0,dp(35),1));
        controls.addView(button("Search",()->{boolean show=searchRow.getVisibility()!=VISIBLE;searchRow.setVisibility(show?VISIBLE:GONE);status.setVisibility(show?VISIBLE:GONE);}),new LayoutParams(0,dp(35),1));
        controls.addView(button("Home",()->open("")),new LayoutParams(0,dp(35),1));
        controls.addView(button("Stop",()->{releaseWeb();status.setText("Stopped · tap Start to load again");}),new LayoutParams(0,dp(35),1));addView(controls);
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
        android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(query.getWindowToken(),0);query.clearFocus();
        searchRow.setVisibility(VISIBLE);status.setVisibility(VISIBLE);
        if(ensureWeb()==null)return;
        status.setText("Loading YouTube · tap video to play");web.loadUrl(address(value));
    }
    @SuppressLint("SetJavaScriptEnabled") WebView ensureWeb(){
        if(!active||closed)return null;
        if(web==null)try{
            WebView created=new WebView(activity);web=created;created.setBackgroundColor(Color.BLACK);
            WebSettings settings=created.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setMediaPlaybackRequiresUserGesture(true);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
            settings.setGeolocationEnabled(false);CookieManager.getInstance().setAcceptThirdPartyCookies(created,false);
            created.setWebViewClient(Build.VERSION.SDK_INT>=26?new ModernClient():new Client());
            created.setWebChromeClient(new WebChromeClient(){
                @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
                @Override public void onShowCustomView(View view,CustomViewCallback callback){callback.onCustomViewHidden();status.setVisibility(VISIBLE);status.setText("Use Edit layout to resize this panel; playback stays inside it.");}
                @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){callback.invoke(origin,false,false);}
            });
            created.setDownloadListener((url,ua,disposition,mime,length)->{status.setVisibility(VISIBLE);status.setText("Downloads are unavailable in this player");});
            start.setVisibility(GONE);body.addView(created,new FrameLayout.LayoutParams(-1,-1));
        }catch(RuntimeException|LinkageError unavailable){releaseWeb();status.setText("WebView unavailable. Update Android System WebView on this device.");return null;}
        return web;
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
        WebView old=web;web=null;
        if(old!=null){body.removeView(old);old.stopLoading();old.onPause();old.removeAllViews();old.destroy();}
        start.setVisibility(VISIBLE);searchRow.setVisibility(VISIBLE);status.setVisibility(VISIBLE);
    }
    @Override public void close(){closed=true;pause();}
}
