package dev.nexadash.radiofixture;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import org.json.*;

public class Events extends ContentProvider {
    public boolean onCreate(){return true;}
    public String getType(Uri uri){return "vnd.android.cursor.dir/fm-fixture";}
    public Cursor query(Uri uri,String[] fields,String where,String[] values,String order){
        MatrixCursor result=new MatrixCursor(new String[]{"event"});try{JSONArray rows=new JSONArray(getContext().getSharedPreferences(FixtureService.EVENTS,0).getString("rows","[]"));for(int i=0;i<rows.length();i++)result.addRow(new Object[]{rows.getJSONObject(i).toString()});}catch(JSONException error){throw new IllegalStateException(error);}return result;
    }
    public Bundle call(String method,String value,Bundle extra){
        if("reset".equals(method))getContext().getSharedPreferences(FixtureService.EVENTS,0).edit().putString("rows","[]").putString("mode",value==null?"normal":value).commit();
        else if("disable".equals(method)||"enable".equals(method)){
            String name=getContext().getPackageName().equals("com.android.fmradio")?"com.android.fmradio.FmtestService":"com.radio.service.RadioService";
            getContext().getPackageManager().setComponentEnabledSetting(new ComponentName(getContext(),name),"disable".equals(method)?android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED:android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,android.content.pm.PackageManager.DONT_KILL_APP);
        }else throw new IllegalArgumentException("Unknown fixture operation");return new Bundle();
    }
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}
    public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
}
