package dev.nexadash.radiofixture;
import android.app.Service;import android.content.*;import android.os.*;import org.json.*;

/** Emulator-only firmware double. No FM library, device, audio patch or tuner hardware. */
public class FixtureService extends Service {
    public static final String EVENTS="events";
    final Handler main=new Handler(Looper.getMainLooper());
    static synchronized void record(Context context,Intent intent,String kind){
        android.content.SharedPreferences p=context.getSharedPreferences(EVENTS,0);
        try{JSONArray events=new JSONArray(p.getString("rows","[]"));JSONObject row=new JSONObject();row.put("kind",kind);row.put("uid",android.os.Process.myUid());row.put("method",intent.getStringExtra("method"));row.put("flag",intent.getIntExtra("flag",-1));row.put("mhz","service".equals(kind)?intent.getFloatExtra("freq",0):0);row.put("khz",intent.getIntExtra("param_freq",0));row.put("action",intent.getAction());events.put(row);p.edit().putString("rows",events.toString()).apply();}catch(JSONException error){throw new IllegalStateException(error);}
    }
    public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null)return START_NOT_STICKY;record(this,intent,"service");
        if(getPackageName().equals("com.android.fmradio")){
            if(intent.getIntExtra("flag",-1)==0&&!"silent".equals(getSharedPreferences(EVENTS,0).getString("mode","normal"))){
                main.postDelayed(()->{Intent result=new Intent("android.intent.action.fmradioseekok").putExtra("freqone",939);record(this,result,"partial");sendBroadcast(result);},100);
                main.postDelayed(()->{Intent result=new Intent("android.intent.action.fmradioseekok").putExtra("freq",new int[]{10180,8750,9390,8750,0,-100});record(this,result,"complete");sendBroadcast(result);},1200);
            }
        }else{
            String method=intent.getStringExtra("method");
            if("method_open".equals(method)||"method_close".equals(method))sendBroadcast(new Intent("com.acloud.intent.android.MAINUI_UPDATE").putExtra("extras",new String[]{"local_radio_state","method_open".equals(method)?"1":"0"}));
        }
        return START_NOT_STICKY;
    }
    public IBinder onBind(Intent intent){return null;}
    public void onDestroy(){record(this,new Intent(),"destroy");main.removeCallbacksAndMessages(null);super.onDestroy();}
}
