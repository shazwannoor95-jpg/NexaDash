package com.shazwan.sagadash;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Separate internet-only test host for the production web player's fullscreen lifecycle. */
public final class WebTestActivity extends Activity {
    private DashWebPlayer player;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(8),dp(12),dp(8));root.setBackgroundColor(0xff07131f);
        TextView title=new TextView(this);title.setText("NexaDash Web Test · 1.0");title.setTextColor(0xffedf5ff);title.setTextSize(20);root.addView(title,new LinearLayout.LayoutParams(-1,dp(40)));
        LinearLayout panels=new LinearLayout(this);root.addView(panels,new LinearLayout.LayoutParams(-1,0,1));
        player=new DashWebPlayer(this);panels.addView(player,new LinearLayout.LayoutParams(0,-1,3));
        TextView help=new TextView(this);help.setTextColor(0xffc6deef);help.setTextSize(15);help.setPadding(dp(18),dp(8),0,0);
        help.setText("UJIAN YOUTUBE WEB\n\n1. Tekan Start YouTube web.\n2. Cari dan main video.\n3. Tekan fullscreen pada video.\n4. Exit fullscreen / Back kembali ke panel.\n\nCuba ulang fullscreen dan keluar beberapa kali.\n\nApp ujian berasingan. NexaDash lama dan datanya tidak diganti.\n\nTiada Radio, TizenTube native atau fungsi default launcher dalam ujian ini.");
        ScrollView instructions=new ScrollView(this);instructions.addView(help);panels.addView(instructions,new LinearLayout.LayoutParams(0,-1,2));setContentView(root);
    }
    @Override protected void onResume(){super.onResume();if(player!=null)player.resume();}
    @Override protected void onPause(){if(player!=null)player.pause();super.onPause();}
    @Override protected void onDestroy(){if(player!=null)player.close();super.onDestroy();}
    @Override public void onBackPressed(){
        if(player!=null&&player.exitFullscreen())return;
        if(player!=null&&player.web!=null&&player.web.canGoBack()){player.web.goBack();return;}
        super.onBackPressed();
    }
}
