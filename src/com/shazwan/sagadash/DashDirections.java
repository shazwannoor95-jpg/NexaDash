package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

/** Lightweight destination entry. Routing, traffic and voice belong to Maps/Waze. */
final class DashDirections {
    static final String MAPS="com.google.android.apps.maps",WAZE="com.waze";
    static final int MAX_QUERY=240;
    private DashDirections(){}
    static String query(String value){return value==null?"":value.trim();}
    static Intent intent(String provider,String destination,boolean directions){
        String q=query(destination);
        if(q.isEmpty()||q.length()>MAX_QUERY)throw new IllegalArgumentException("Enter a destination (up to 240 characters).");
        Uri uri;
        if(WAZE.equals(provider))uri=new Uri.Builder().scheme("https").authority("waze.com").path("/ul")
            .appendQueryParameter("q",q).appendQueryParameter("navigate",directions?"yes":"no").build();
        else if(directions)uri=new Uri.Builder().scheme("https").authority("www.google.com").path("/maps/dir/")
            .appendQueryParameter("api","1").appendQueryParameter("destination",q).appendQueryParameter("travelmode","driving").build();
        else uri=new Uri.Builder().scheme("https").authority("www.google.com").path("/maps/search/")
            .appendQueryParameter("api","1").appendQueryParameter("query",q).build();
        Intent result=new Intent(Intent.ACTION_VIEW,uri);
        if(MAPS.equals(provider)||WAZE.equals(provider))result.setPackage(provider);
        return result;
    }
    static boolean installed(Activity activity,String pkg){
        try{return activity.getPackageManager().getApplicationInfo(pkg,0).enabled;}
        catch(PackageManager.NameNotFoundException ignored){return false;}
    }
    static AlertDialog show(Activity activity){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        int pad=Math.round(18*activity.getResources().getDisplayMetrics().density);content.setPadding(pad,pad,pad,pad);
        TextView explanation=new TextView(activity);explanation.setText("Search a place or address. Directions and voice guidance open in Google Maps or Waze. The dashboard panel remains a GPS map preview.");content.addView(explanation);
        EditText destination=new EditText(activity);destination.setTag("directions_destination");destination.setSingleLine(true);destination.setHint("Place, address or coordinates");destination.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);destination.setImeOptions(EditorInfo.IME_ACTION_DONE);
        destination.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(MAX_QUERY)});
        destination.setText(DashShortcuts.prefs(activity).getString("directions_destination",""));content.addView(destination);
        RadioGroup providers=new RadioGroup(activity);providers.setOrientation(RadioGroup.HORIZONTAL);
        boolean maps=installed(activity,MAPS),waze=installed(activity,WAZE);
        String selected=DashShortcuts.prefs(activity).getString("directions_provider",MAPS);
        if(!maps&&waze)selected=WAZE;else if(!waze&&maps)selected=MAPS;
        RadioButton google=new RadioButton(activity);google.setId(101);google.setText(maps?"Google Maps":"Maps web");providers.addView(google);
        RadioButton wz=new RadioButton(activity);wz.setId(102);wz.setText("Waze");wz.setEnabled(waze);providers.addView(wz);providers.check(WAZE.equals(selected)&&waze?102:101);content.addView(providers);
        TextView note=new TextView(activity);note.setText("Voice follows the navigation app's sound and language settings. Internet/GPS may be needed. Choose your destination while parked.");content.addView(note);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Search / directions").setView(content)
            .setPositiveButton("Directions",null).setNeutralButton("Search",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->{
            for(int button:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEUTRAL})dialog.getButton(button).setOnClickListener(v->{
                String q=query(destination.getText().toString());if(q.isEmpty()||q.length()>MAX_QUERY){destination.setError("Enter a destination (up to 240 characters).");return;}
                String provider=providers.getCheckedRadioButtonId()==102?WAZE:maps?MAPS:"web";
                Intent request=intent(provider,q,button==AlertDialog.BUTTON_POSITIVE);
                if(DashShortcuts.start(activity,request)){
                    DashShortcuts.prefs(activity).edit().putString("directions_destination",q).putString("directions_provider",provider).apply();dialog.dismiss();
                }else Toast.makeText(activity,"Unable to open navigation. Install Maps/Waze or a web browser, then try again.",Toast.LENGTH_LONG).show();
            });
            destination.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();return true;}return false;});
        });
        dialog.show();return dialog;
    }
}
